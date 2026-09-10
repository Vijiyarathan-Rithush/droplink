package presentation.view;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import presentation.component.PathPickerField;
import presentation.component.SectionHeader;
import presentation.component.StatusBanner;
import presentation.component.TransferProgressView;
import presentation.component.ValidatedTextField;
import presentation.util.FileSizeFormatter;
import presentation.validation.InputValidators;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SendView extends VBox
{
    private final Stage owner;
    private final ValidatedTextField host = new ValidatedTextField(
            "Empfänger-IP", "127.0.0.1", "z. B. 192.168.1.24");
    private final ValidatedTextField port = new ValidatedTextField("Port", "5000", "1–65535");
    private final PathPickerField file = new PathPickerField("Datei", "", "Datei auswählen");
    private final Label fileMeta = new Label("Noch keine Datei ausgewählt");
    private final TransferProgressView progress = new TransferProgressView();
    private final StatusBanner status = new StatusBanner("Bereit zum Senden");
    private final Button sendButton = new Button("Datei senden");
    private final Button cancelButton = new Button("Abbrechen");

    public SendView(Stage owner)
    {
        this.owner = owner;
        setSpacing(16);
        getStyleClass().add("content-card");

        host.setValidator(InputValidators::ipv4);
        port.setValidator(InputValidators::port);
        file.setValidator(InputValidators::existingFile);
        file.setOnBrowse(event -> chooseFile());
        fileMeta.getStyleClass().add("file-meta");
        fileMeta.setManaged(false);
        fileMeta.setVisible(false);

        GridPane connection = new GridPane();
        connection.setHgap(16);
        connection.add(host, 0, 0);
        connection.add(port, 1, 0);
        GridPane.setHgrow(host, Priority.ALWAYS);
        host.setMaxWidth(Double.MAX_VALUE);

        sendButton.getStyleClass().add("primary-button");
        cancelButton.getStyleClass().add("danger-button");
        cancelButton.setDisable(true);
        HBox actions = new HBox(10, sendButton, cancelButton);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        getChildren().addAll(
                new SectionHeader("Datei senden"),
                connection,
                file,
                fileMeta,
                progress,
                status,
                actions);
    }

    public boolean validateInputs()
    {
        return host.validate() & port.validate() & file.validate();
    }

    public String host()
    {
        return host.value();
    }

    public int port()
    {
        return Integer.parseInt(port.value());
    }

    public Path selectedFile()
    {
        return Path.of(file.value());
    }

    public void onSend(EventHandler<ActionEvent> handler)
    {
        sendButton.setOnAction(handler);
    }

    public void onCancel(EventHandler<ActionEvent> handler)
    {
        cancelButton.setOnAction(handler);
    }

    public void setBusy(boolean busy)
    {
        host.setInputDisabled(busy);
        port.setInputDisabled(busy);
        file.setInputDisabled(busy);
        sendButton.setDisable(busy);
        cancelButton.setDisable(!busy);
    }

    public void resetProgress()
    {
        progress.reset();
    }

    public void updateProgress(long transferred, long total)
    {
        progress.update(transferred, total);
    }

    public void setStatus(String text, StatusBanner.Type type)
    {
        status.setStatus(text, type);
    }

    private void chooseFile()
    {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Datei zum Senden auswählen");
        File selected = chooser.showOpenDialog(owner);
        if (selected == null) return;

        Path selectedPath = selected.toPath();
        file.setValue(selectedPath.toString());
        try
        {
            fileMeta.setText(selectedPath.getFileName() + "  ·  " + FileSizeFormatter.format(Files.size(selectedPath)));
        }
        catch (IOException e)
        {
            fileMeta.setText("Dateigröße konnte nicht gelesen werden");
        }
        fileMeta.setManaged(true);
        fileMeta.setVisible(true);
    }

}
