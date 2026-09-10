package presentation.component;

import domain.TransferRequest;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import presentation.util.FileSizeFormatter;

import java.util.concurrent.CompletableFuture;

public final class DialogService
{
    private final Stage owner;

    public DialogService(Stage owner)
    {
        this.owner = owner;
    }

    public CompletableFuture<Boolean> askToAccept(TransferRequest request)
    {
        CompletableFuture<Boolean> answer = new CompletableFuture<>();
        Platform.runLater(() ->
        {
            ButtonType accept = new ButtonType("Annehmen", ButtonBar.ButtonData.OK_DONE);
            ButtonType reject = new ButtonType("Ablehnen", ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", accept, reject);
            alert.initOwner(owner);
            alert.setTitle("Eingehende Datei");
            alert.setHeaderText(request.fileName());
            alert.setContentText(
                    "Dateigröße: " + FileSizeFormatter.format(request.fileSize())
                            + "\n\nMöchtest du diese Datei empfangen?");
            answer.complete(alert.showAndWait().orElse(reject) == accept);
        });
        return answer;
    }

    public void showError(String title, String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle("DropLink");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
