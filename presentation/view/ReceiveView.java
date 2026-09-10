package presentation.view;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;
import presentation.component.PathPickerField;
import presentation.component.SectionHeader;
import presentation.component.StatusBanner;
import presentation.component.TransferProgressView;
import presentation.component.ValidatedTextField;
import presentation.validation.InputValidators;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ReceiveView extends VBox
{
    private final Stage owner;
    private Path outputDirectory = Path.of(System.getProperty("user.home"), "Downloads");
    private final ValidatedTextField port = new ValidatedTextField("Lokaler Port", "5000", "1–65535");
    private final PathPickerField directory = new PathPickerField(
            "Zielordner", outputDirectory.toString(), "Zielordner auswählen");
    private final TransferProgressView progress = new TransferProgressView();
    private final StatusBanner status = new StatusBanner("Empfänger ist gestoppt");
    private final Button startButton = new Button("Empfänger starten");
    private final Button stopButton = new Button("Stoppen");

    public ReceiveView(Stage owner)
    {
        this.owner = owner;
        setSpacing(16);
        getStyleClass().add("content-card");

        port.setValidator(InputValidators::port);
        directory.setValidator(InputValidators::outputDirectory);
        directory.setOnBrowse(event -> chooseDirectory());
        startButton.getStyleClass().add("primary-button");
        stopButton.getStyleClass().add("danger-button");
        stopButton.setManaged(false);
        stopButton.setVisible(false);

        getChildren().addAll(
                new SectionHeader("Dateien empfangen"),
                port,
                directory,
                progress,
                status,
                actions());
    }

    private HBox actions()
    {
        HBox actions = new HBox(10, startButton, stopButton);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        return actions;
    }

    public boolean validateInputs()
    {
        return port.validate() & directory.validate();
    }

    public int port()
    {
        return Integer.parseInt(port.value());
    }

    public Path outputDirectory()
    {
        return outputDirectory.toAbsolutePath().normalize();
    }

    public void onStart(EventHandler<ActionEvent> handler)
    {
        startButton.setOnAction(handler);
    }

    public void onStop(EventHandler<ActionEvent> handler)
    {
        stopButton.setOnAction(handler);
    }

    public void setListening(boolean listening)
    {
        port.setInputDisabled(listening);
        directory.setInputDisabled(listening);
        startButton.setManaged(!listening);
        startButton.setVisible(!listening);
        stopButton.setManaged(listening);
        stopButton.setVisible(listening);
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

    private void chooseDirectory()
    {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Zielordner auswählen");
        if (Files.isDirectory(outputDirectory)) chooser.setInitialDirectory(outputDirectory.toFile());
        File selected = chooser.showDialog(owner);
        if (selected == null) return;

        outputDirectory = selected.toPath();
        directory.setValue(outputDirectory.toString());
    }

}
