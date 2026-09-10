package presentation.component;

import domain.TransferRequest;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import presentation.util.FileSizeFormatter;

import java.util.Objects;
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
            Stage dialog = createDialog();
            Label icon = new Label("↓");
            icon.getStyleClass().add("dialog-icon");
            Label title = new Label("Datei empfangen?");
            title.getStyleClass().add("dialog-title");
            Label fileName = new Label(request.fileName());
            fileName.setWrapText(true);
            fileName.getStyleClass().add("dialog-file-name");
            Label size = new Label(FileSizeFormatter.format(request.fileSize()));
            size.getStyleClass().add("dialog-size");

            VBox details = new VBox(4, title, fileName);
            HBox.setHgrow(details, Priority.ALWAYS);
            HBox header = new HBox(14, icon, details, size);
            header.setAlignment(Pos.CENTER_LEFT);

            Button reject = new Button("Ablehnen");
            reject.getStyleClass().add("secondary-button");
            Button accept = new Button("Annehmen");
            accept.getStyleClass().add("primary-button");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox actions = new HBox(10, spacer, reject, accept);

            reject.setOnAction(event -> complete(dialog, answer, false));
            accept.setOnAction(event -> complete(dialog, answer, true));
            dialog.setOnCloseRequest(event -> answer.complete(false));

            show(dialog, new VBox(22, header, actions));
            answer.complete(false);
        });
        return answer;
    }

    public void showError(String title, String message)
    {
        Stage dialog = createDialog();
        Label icon = new Label("!");
        icon.getStyleClass().addAll("dialog-icon", "dialog-error-icon");
        Label heading = new Label(title);
        heading.getStyleClass().add("dialog-title");
        Label body = new Label(message);
        body.setWrapText(true);
        body.getStyleClass().add("dialog-file-name");
        VBox details = new VBox(5, heading, body);
        HBox.setHgrow(details, Priority.ALWAYS);

        Button close = new Button("Schließen");
        close.getStyleClass().add("primary-button");
        close.setOnAction(event -> dialog.close());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        show(dialog, new VBox(22,
                new HBox(14, icon, details),
                new HBox(spacer, close)));
    }

    private Stage createDialog()
    {
        Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setResizable(false);
        return dialog;
    }

    private void show(Stage dialog, VBox content)
    {
        content.getStyleClass().add("dialog-card");
        StackPane root = new StackPane(content);
        root.getStyleClass().add("dialog-root");
        Scene scene = new Scene(root, 460, 220, Color.TRANSPARENT);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/presentation/styles.css"), "Dialog stylesheet is missing").toExternalForm());
        scene.setOnKeyPressed(event ->
        {
            if (event.getCode() == KeyCode.ESCAPE) dialog.close();
        });
        double[] dragOffset = new double[2];
        root.setOnMousePressed(event ->
        {
            dragOffset[0] = event.getSceneX();
            dragOffset[1] = event.getSceneY();
        });
        root.setOnMouseDragged(event ->
        {
            dialog.setX(event.getScreenX() - dragOffset[0]);
            dialog.setY(event.getScreenY() - dragOffset[1]);
        });
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void complete(Stage dialog, CompletableFuture<Boolean> answer, boolean accepted)
    {
        answer.complete(accepted);
        dialog.close();
    }
}
