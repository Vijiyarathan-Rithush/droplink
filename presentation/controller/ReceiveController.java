package presentation.controller;

import domain.TransferRequest;
import infrastructure.DeviceDiscoveryResponder;
import infrastructure.DeviceDiscoveryService;
import infrastructure.TcpServer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import presentation.component.StatusBanner;
import presentation.view.ReceiveView;
import service.FileTransferService;
import service.IncomingTransferService;
import service.TransferDecisionService;
import service.interfaces.IClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ReceiveController
{
    private final ReceiveView view;
    private final Map<UUID, PendingTransfer> pendingTransfers = new ConcurrentHashMap<>();
    private volatile TcpServer activeServer;
    private volatile DeviceDiscoveryResponder discoveryResponder;
    private Task<Void> listenerTask;

    public ReceiveController(ReceiveView view)
    {
        this.view = view;
        view.onStart(event -> start());
        view.onStop(event -> stop());
    }

    public void start()
    {
        if (listenerTask != null && listenerTask.isRunning()) return;
        if (!view.validateInputs())
        {
            view.setStatus("Bitte einen gültigen Speicherort auswählen.", StatusBanner.Type.NEUTRAL);
            return;
        }

        try
        {
            int port = view.port();
            Path directory = view.outputDirectory();
            Files.createDirectories(directory);
            view.setListening(true);
            view.setStatus("Empfang wird aktiviert …", StatusBanner.Type.ACTIVE);

            DeviceDiscoveryResponder responder = new DeviceDiscoveryResponder(
                    DeviceDiscoveryService.localDeviceName(), port);
            responder.start();
            discoveryResponder = responder;

            listenerTask = createListenerTask(port, directory);
            listenerTask.setOnCancelled(event -> finish("Empfang pausiert.", StatusBanner.Type.NEUTRAL));
            listenerTask.setOnSucceeded(event -> finish("Empfang pausiert.", StatusBanner.Type.NEUTRAL));
            listenerTask.setOnFailed(event -> finish(
                    "Empfang ist derzeit nicht verfügbar.", StatusBanner.Type.NEUTRAL));
            startDaemon(listenerTask, "droplink-receive");
        }
        catch (Exception ignored)
        {
            closeDiscoveryResponder();
            view.setListening(false);
            view.setStatus("Empfang ist derzeit nicht verfügbar.", StatusBanner.Type.NEUTRAL);
        }
    }

    private Task<Void> createListenerTask(int port, Path directory)
    {
        return new Task<>()
        {
            @Override
            protected Void call()
            {
                setStatus("Bereit für neue Anfragen.", StatusBanner.Type.ACTIVE);
                while (!isCancelled())
                {
                    TcpServer server = new TcpServer();
                    activeServer = server;
                    try
                    {
                        server.start(port);
                        if (isCancelled()) break;
                        IClient client = server.getClient();
                        server.closeServerSocket();
                        queueRequest(client, directory);
                    }
                    catch (Exception ignored)
                    {
                        if (!isCancelled())
                            setStatus("Bereit für neue Anfragen.", StatusBanner.Type.ACTIVE);
                    }
                    finally
                    {
                        closeServer(server);
                        activeServer = null;
                    }
                }
                return null;
            }
        };
    }

    private void queueRequest(IClient client, Path directory)
    {
        UUID id = UUID.randomUUID();
        TransferDecisionService decisions = new TransferDecisionService();
        FileTransferService files = new FileTransferService(
                client,
                decisions,
                (transferred, total) -> Platform.runLater(
                        () -> view.updateRequestProgress(id, transferred, total)));
        IncomingTransferService incoming = new IncomingTransferService(files, decisions);

        try
        {
            TransferRequest request = incoming.receiveRequest(client);
            if (request.fileName() == null || request.fileName().isBlank() || request.fileSize() < 0)
            {
                incoming.reject(client);
                disconnect(client);
                return;
            }

            PendingTransfer pending = new PendingTransfer(client, incoming, request, directory);
            pendingTransfers.put(id, pending);
            Platform.runLater(() ->
            {
                view.addRequest(id, request, () -> accept(id), () -> reject(id));
                updateInboxStatus();
            });
        }
        catch (Exception ignored)
        {
            disconnect(client);
        }
    }

    private void accept(UUID id)
    {
        PendingTransfer pending = pendingTransfers.get(id);
        if (pending == null || !pending.decided.compareAndSet(false, true)) return;
        view.setRequestBusy(id);

        Task<Path> receiveTask = new Task<>()
        {
            @Override
            protected Path call() throws Exception
            {
                return pending.incoming.accept(
                        pending.client, pending.request, pending.outputDirectory);
            }
        };
        receiveTask.setOnSucceeded(event ->
        {
            Path receivedFile = receiveTask.getValue();
            complete(id, pending);
            view.setStatus(receivedFile == null
                            ? "Die Anfrage wurde nicht gespeichert."
                            : receivedFile.getFileName() + " wurde gespeichert.",
                    receivedFile == null ? StatusBanner.Type.NEUTRAL : StatusBanner.Type.SUCCESS);
        });
        receiveTask.setOnFailed(event ->
        {
            complete(id, pending);
            view.setStatus("Die Übertragung wurde beendet.", StatusBanner.Type.NEUTRAL);
        });
        startDaemon(receiveTask, "droplink-accept-" + id);
    }

    private void reject(UUID id)
    {
        PendingTransfer pending = pendingTransfers.get(id);
        if (pending == null || !pending.decided.compareAndSet(false, true)) return;
        try
        {
            pending.incoming.reject(pending.client);
        }
        catch (Exception ignored)
        {
            // The sender may already have disconnected.
        }
        complete(id, pending);
        view.setStatus("Anfrage abgelehnt.", StatusBanner.Type.NEUTRAL);
    }

    private void complete(UUID id, PendingTransfer pending)
    {
        pendingTransfers.remove(id, pending);
        disconnect(pending.client);
        view.removeRequest(id);
        updateInboxStatus();
    }

    private void updateInboxStatus()
    {
        int count = pendingTransfers.size();
        if (count == 0)
            view.setStatus("Bereit für neue Anfragen.", StatusBanner.Type.ACTIVE);
        else
            view.setStatus(count == 1 ? "1 offene Anfrage." : count + " offene Anfragen.",
                    StatusBanner.Type.ACTIVE);
    }

    public void stop()
    {
        closeDiscoveryResponder();
        if (listenerTask != null) listenerTask.cancel(true);
        closeServer(activeServer);

        pendingTransfers.forEach((id, pending) ->
        {
            if (pending.decided.compareAndSet(false, true))
            {
                try
                {
                    pending.incoming.reject(pending.client);
                }
                catch (Exception ignored)
                {
                    // Closing the socket below is sufficient.
                }
            }
            else
            {
                pending.incoming.cancel();
            }
            disconnect(pending.client);
            view.removeRequest(id);
        });
        pendingTransfers.clear();
        finish("Empfang pausiert.", StatusBanner.Type.NEUTRAL);
    }

    private void finish(String message, StatusBanner.Type type)
    {
        closeDiscoveryResponder();
        view.setListening(false);
        view.setStatus(message, type);
    }

    private void closeDiscoveryResponder()
    {
        DeviceDiscoveryResponder responder = discoveryResponder;
        discoveryResponder = null;
        if (responder != null) responder.close();
    }

    private void setStatus(String message, StatusBanner.Type type)
    {
        Platform.runLater(() -> view.setStatus(message, type));
    }

    private void closeServer(TcpServer server)
    {
        if (server == null || !server.isRunning()) return;
        try
        {
            server.stop();
        }
        catch (RuntimeException ignored)
        {
            // Closing the listening socket intentionally interrupts accept().
        }
    }

    private void disconnect(IClient client)
    {
        if (client == null || !client.isConnected()) return;
        try
        {
            client.disconnect();
        }
        catch (RuntimeException ignored)
        {
            // The connection may already be closed.
        }
    }

    private void startDaemon(Task<?> task, String name)
    {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }

    private static final class PendingTransfer
    {
        private final IClient client;
        private final IncomingTransferService incoming;
        private final TransferRequest request;
        private final Path outputDirectory;
        private final AtomicBoolean decided = new AtomicBoolean();

        private PendingTransfer(
                IClient client,
                IncomingTransferService incoming,
                TransferRequest request,
                Path outputDirectory)
        {
            this.client = client;
            this.incoming = incoming;
            this.request = request;
            this.outputDirectory = outputDirectory;
        }
    }
}
