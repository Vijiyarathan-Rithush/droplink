package presentation.controller;

import domain.TransferRequest;
import infrastructure.TcpServer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import presentation.component.DialogService;
import presentation.component.StatusBanner;
import presentation.view.ReceiveView;
import service.FileTransferService;
import service.IncomingTransferService;
import service.TransferDecisionService;
import service.interfaces.IClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ReceiveController
{
    private final ReceiveView view;
    private final DialogService dialogs;
    private volatile IncomingTransferService activeTransfer;
    private volatile TcpServer activeServer;
    private Task<Void> task;

    public ReceiveController(ReceiveView view, DialogService dialogs)
    {
        this.view = view;
        this.dialogs = dialogs;
        view.onStart(event -> start());
        view.onStop(event -> stop());
    }

    private void start()
    {
        if (task != null && task.isRunning()) return;
        if (!view.validateInputs())
        {
            view.setStatus("Bitte korrigiere die markierten Eingaben.", StatusBanner.Type.ERROR);
            return;
        }

        try
        {
            int port = view.port();
            Path directory = view.outputDirectory();
            Files.createDirectories(directory);
            view.resetProgress();
            view.setListening(true);
            view.setStatus("Empfänger wird gestartet …", StatusBanner.Type.ACTIVE);

            task = createReceiverTask(port, directory);
            task.setOnCancelled(event -> finish("Empfänger gestoppt.", StatusBanner.Type.NEUTRAL));
            task.setOnSucceeded(event -> finish("Empfänger gestoppt.", StatusBanner.Type.NEUTRAL));
            task.setOnFailed(event -> finish(
                    "Empfänger wurde gestoppt. Bitte Port und Netzwerkfreigabe prüfen.",
                    StatusBanner.Type.ERROR));
            startDaemon(task, "droplink-receive");
        }
        catch (Exception e)
        {
            view.setListening(false);
            view.setStatus("Empfänger konnte nicht gestartet werden.", StatusBanner.Type.ERROR);
        }
    }

    private Task<Void> createReceiverTask(int port, Path directory)
    {
        return new Task<>()
        {
            @Override
            protected Void call() throws Exception
            {
                while (!isCancelled())
                {
                    TcpServer server = new TcpServer();
                    activeServer = server;
                    setStatus("Bereit – warte auf Verbindung an Port " + port + " …", StatusBanner.Type.ACTIVE);

                    try
                    {
                        server.start(port);
                        if (isCancelled()) break;
                        handleConnection(server.getClient(), directory);
                    }
                    catch (IOException e)
                    {
                        if (!isCancelled())
                            setStatus("Transfer unterbrochen – warte auf die nächste Verbindung …",
                                    StatusBanner.Type.ERROR);
                    }
                    catch (RuntimeException e)
                    {
                        if (!isCancelled()) throw e;
                    }
                    finally
                    {
                        closeServer(server);
                        activeTransfer = null;
                        activeServer = null;
                    }
                }
                return null;
            }
        };
    }

    private void handleConnection(IClient client, Path directory) throws Exception
    {
        Platform.runLater(view::resetProgress);
        TransferDecisionService decisions = new TransferDecisionService();
        FileTransferService files = new FileTransferService(
                client,
                decisions,
                (transferred, total) -> Platform.runLater(() -> view.updateProgress(transferred, total)));
        IncomingTransferService incoming = new IncomingTransferService(files, decisions);
        activeTransfer = incoming;

        TransferRequest request = incoming.receiveRequest(client);
        setStatus("Anfrage für „" + request.fileName() + "“ erhalten.", StatusBanner.Type.ACTIVE);
        boolean accepted = dialogs.askToAccept(request).get();
        if (task.isCancelled()) return;

        if (!accepted)
        {
            incoming.reject(client);
            setStatus("Datei abgelehnt – warte auf die nächste Verbindung …", StatusBanner.Type.NEUTRAL);
            return;
        }

        Path receivedFile = incoming.accept(client, request, directory);
        setStatus(receivedFile == null
                        ? "Datei nicht gespeichert: Name ungültig oder bereits vorhanden."
                        : "Empfangen: " + receivedFile.getFileName(),
                receivedFile == null ? StatusBanner.Type.ERROR : StatusBanner.Type.SUCCESS);
    }

    public void stop()
    {
        IncomingTransferService transfer = activeTransfer;
        if (transfer != null) transfer.cancel();
        if (task != null) task.cancel(true);
        closeServer(activeServer);
        finish("Empfänger gestoppt.", StatusBanner.Type.NEUTRAL);
    }

    private void finish(String message, StatusBanner.Type type)
    {
        view.setListening(false);
        view.setStatus(message, type);
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

    private void startDaemon(Task<?> task, String name)
    {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }
}
