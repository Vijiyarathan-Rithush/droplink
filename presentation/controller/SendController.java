package presentation.controller;

import domain.NetworkEndpoint;
import domain.TransferDecision;
import infrastructure.DeviceDiscoveryService;
import infrastructure.TcpClient;
import javafx.application.Platform;
import javafx.concurrent.Task;
import presentation.component.StatusBanner;
import presentation.view.SendView;
import service.FileTransferService;
import service.exceptions.TransferCancelledException;
import service.TransferDecisionService;
import service.interfaces.IClient;

import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SendController
{
    private final SendView view;
    private final DeviceDiscoveryService discovery = new DeviceDiscoveryService();
    private final AtomicBoolean discovering = new AtomicBoolean();
    private final ScheduledExecutorService discoveryExecutor = Executors.newSingleThreadScheduledExecutor(task ->
    {
        Thread thread = new Thread(task, "droplink-discovery-scan");
        thread.setDaemon(true);
        return thread;
    });
    private volatile FileTransferService activeTransfer;
    private Task<TransferDecision> task;

    public SendController(SendView view)
    {
        this.view = view;
        view.onSend(event -> send());
        view.onCancel(event -> cancel());
        view.onRefresh(event -> discoveryExecutor.execute(this::refreshDevices));
        discoveryExecutor.scheduleWithFixedDelay(this::refreshDevices, 0, 4, TimeUnit.SECONDS);
    }

    private void send()
    {
        if (task != null && task.isRunning()) return;
        if (!view.validateInputs())
        {
            view.setStatus("Bitte ein Gerät und eine Datei auswählen.", StatusBanner.Type.NEUTRAL);
            return;
        }

        try
        {
            NetworkEndpoint endpoint = view.selectedDevice().endpoint();
            Path file = view.selectedFile();
            view.resetProgress();
            view.setBusy(true);
            view.setStatus("Verbindung wird aufgebaut …", StatusBanner.Type.ACTIVE);

            task = new Task<>()
            {
                @Override
                protected TransferDecision call() throws Exception
                {
                    IClient client = new TcpClient(new Socket());
                    TransferDecisionService decisions = new TransferDecisionService();
                    activeTransfer = new FileTransferService(
                            client,
                            decisions,
                            (transferred, total) -> Platform.runLater(
                                    () -> view.updateProgress(transferred, total)));
                    return activeTransfer.send(endpoint, file);
                }
            };

            task.setOnSucceeded(event -> finish(
                    task.getValue() == TransferDecision.ACCEPTED
                            ? "Datei erfolgreich gesendet."
                            : "Der Empfänger hat die Datei abgelehnt.",
                    task.getValue() == TransferDecision.ACCEPTED
                            ? StatusBanner.Type.SUCCESS
                            : StatusBanner.Type.ERROR));
            task.setOnCancelled(event -> finish("Transfer abgebrochen.", StatusBanner.Type.NEUTRAL));
            task.setOnFailed(event ->
            {
                Throwable error = task.getException();
                finish(error instanceof TransferCancelledException
                                ? "Transfer abgebrochen."
                                : "Senden nicht möglich. Bitte Verbindung und Empfänger prüfen.",
                        error instanceof TransferCancelledException
                                ? StatusBanner.Type.NEUTRAL
                                : StatusBanner.Type.ERROR);
            });
            startDaemon(task, "droplink-send");
        }
        catch (Exception e)
        {
            view.setBusy(false);
            view.setStatus("Senden konnte nicht gestartet werden.", StatusBanner.Type.ERROR);
        }
    }

    public void cancel()
    {
        FileTransferService transfer = activeTransfer;
        if (transfer != null) transfer.cancel();
        if (task != null) task.cancel(true);
    }

    public void shutdown()
    {
        cancel();
        discoveryExecutor.shutdownNow();
    }

    private void refreshDevices()
    {
        if (!discovering.compareAndSet(false, true)) return;
        Platform.runLater(() -> view.setDiscovering(true));
        try
        {
            List<domain.DiscoveredDevice> devices = discovery.discover(1_200);
            Platform.runLater(() ->
            {
                view.setDevices(devices);
                if (task == null || !task.isRunning())
                {
                    view.setStatus(devices.isEmpty()
                                    ? "Keine empfangsbereiten Geräte gefunden."
                                    : devices.size() == 1
                                            ? "1 Gerät gefunden – Datei auswählen und senden."
                                            : devices.size() + " Geräte gefunden – Empfänger auswählen.",
                            devices.isEmpty() ? StatusBanner.Type.NEUTRAL : StatusBanner.Type.SUCCESS);
                }
            });
        }
        catch (Exception ignored)
        {
            Platform.runLater(() ->
                    view.setStatus("Gerätesuche derzeit nicht verfügbar.", StatusBanner.Type.NEUTRAL));
        }
        finally
        {
            discovering.set(false);
            Platform.runLater(() -> view.setDiscovering(false));
        }
    }

    private void finish(String message, StatusBanner.Type type)
    {
        activeTransfer = null;
        view.setBusy(false);
        view.setStatus(message, type);
    }

    private void startDaemon(Task<?> task, String name)
    {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }
}
