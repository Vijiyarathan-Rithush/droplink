package presentation.controller;

import domain.NetworkEndpoint;
import domain.TransferDecision;
import infrastructure.TcpClient;
import javafx.application.Platform;
import javafx.concurrent.Task;
import presentation.component.DialogService;
import presentation.component.StatusBanner;
import presentation.view.SendView;
import service.FileTransferService;
import service.TransferCancelledException;
import service.TransferDecisionService;
import service.interfaces.IClient;

import java.net.Socket;
import java.nio.file.Path;

public final class SendController
{
    private final SendView view;
    private final DialogService dialogs;
    private volatile FileTransferService activeTransfer;
    private Task<TransferDecision> task;

    public SendController(SendView view, DialogService dialogs)
    {
        this.view = view;
        this.dialogs = dialogs;
        view.onSend(event -> send());
        view.onCancel(event -> cancel());
    }

    private void send()
    {
        if (task != null && task.isRunning()) return;
        if (!view.validateInputs())
        {
            view.setStatus("Bitte korrigiere die markierten Eingaben.", StatusBanner.Type.ERROR);
            return;
        }

        try
        {
            NetworkEndpoint endpoint = new NetworkEndpoint(view.host(), view.port());
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
                                : "Senden fehlgeschlagen: " + messageOf(error),
                        error instanceof TransferCancelledException
                                ? StatusBanner.Type.NEUTRAL
                                : StatusBanner.Type.ERROR);
            });
            startDaemon(task, "droplink-send");
        }
        catch (Exception e)
        {
            view.setBusy(false);
            dialogs.showError("Senden nicht möglich", messageOf(e));
        }
    }

    public void cancel()
    {
        FileTransferService transfer = activeTransfer;
        if (transfer != null) transfer.cancel();
        if (task != null) task.cancel(true);
    }

    private void finish(String message, StatusBanner.Type type)
    {
        activeTransfer = null;
        view.setBusy(false);
        view.setStatus(message, type);
    }

    private String messageOf(Throwable error)
    {
        if (error == null) return "Unbekannter Fehler";
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private void startDaemon(Task<?> task, String name)
    {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }
}
