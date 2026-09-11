package presentation.view;

import domain.DiscoveredDevice;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import presentation.component.PathPickerField;
import presentation.component.SectionHeader;
import presentation.component.StatusBanner;
import presentation.component.TransferProgressView;
import presentation.util.FileSizeFormatter;
import presentation.validation.InputValidators;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SendView extends VBox
{
    private final Stage owner;
    private final ToggleGroup deviceSelection = new ToggleGroup();
    private final FlowPane deviceGrid = new FlowPane(12, 12);
    private final Label noDevices = new Label("Noch keine Geräte gefunden");
    private final Button refreshButton = new Button("Aktualisieren");
    private final PathPickerField file = new PathPickerField("Datei", "", "Datei auswählen");
    private final Label fileMeta = new Label("Noch keine Datei ausgewählt");
    private final TransferProgressView progress = new TransferProgressView();
    private final StatusBanner status = new StatusBanner("Bereit zum Senden");
    private final Button sendButton = new Button("Datei senden");
    private final Button cancelButton = new Button("Abbrechen");
    private boolean busy;
    private boolean discovering;

    public SendView(Stage owner)
    {
        this.owner = owner;
        setSpacing(16);
        getStyleClass().add("content-card");

        file.setValidator(InputValidators::existingFile);
        file.setOnBrowse(event -> chooseFile());
        file.setOnPathDropped(this::selectFile);
        fileMeta.getStyleClass().add("file-meta");
        fileMeta.setManaged(false);
        fileMeta.setVisible(false);

        Label deviceLabel = new Label("Empfänger");
        deviceLabel.getStyleClass().add("field-label");
        refreshButton.getStyleClass().add("secondary-button");
        Region deviceHeaderSpacer = new Region();
        HBox.setHgrow(deviceHeaderSpacer, Priority.ALWAYS);
        HBox deviceHeader = new HBox(10, deviceLabel, deviceHeaderSpacer, refreshButton);
        deviceHeader.setAlignment(Pos.CENTER_LEFT);
        deviceGrid.getStyleClass().add("device-grid");
        noDevices.getStyleClass().add("device-empty-state");
        VBox deviceField = new VBox(10, deviceHeader, noDevices, deviceGrid);
        deviceField.getStyleClass().add("form-field");

        sendButton.getStyleClass().add("primary-button");
        cancelButton.getStyleClass().add("danger-button");
        cancelButton.setDisable(true);
        HBox actions = new HBox(10, sendButton, cancelButton);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        getChildren().addAll(
                new SectionHeader("Datei senden"),
                deviceField,
                file,
                fileMeta,
                progress,
                status,
                actions);
    }

    public boolean validateInputs()
    {
        return deviceSelection.getSelectedToggle() != null & file.validate();
    }

    public DiscoveredDevice selectedDevice()
    {
        if (!(deviceSelection.getSelectedToggle() instanceof ToggleButton selected)) return null;
        return (DiscoveredDevice) selected.getUserData();
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

    public void onRefresh(EventHandler<ActionEvent> handler)
    {
        refreshButton.setOnAction(handler);
    }

    public void setDevices(List<DiscoveredDevice> discoveredDevices)
    {
        DiscoveredDevice selected = selectedDevice();
        deviceSelection.getToggles().clear();
        deviceGrid.getChildren().clear();

        ToggleButton selectedCard = null;
        for (DiscoveredDevice device : discoveredDevices)
        {
            ToggleButton card = createDeviceCard(device);
            deviceGrid.getChildren().add(card);
            if (selected != null && selected.endpoint().equals(device.endpoint())) selectedCard = card;
        }

        noDevices.setManaged(discoveredDevices.isEmpty());
        noDevices.setVisible(discoveredDevices.isEmpty());
        if (selectedCard != null) selectedCard.setSelected(true);
        else if (discoveredDevices.size() == 1)
            ((ToggleButton) deviceGrid.getChildren().getFirst()).setSelected(true);
    }

    public void setDiscovering(boolean discovering)
    {
        this.discovering = discovering;
        refreshButton.setDisable(discovering || busy);
        refreshButton.setText(discovering ? "Suche …" : "Aktualisieren");
    }

    public void setBusy(boolean busy)
    {
        this.busy = busy;
        deviceSelection.getToggles().forEach(toggle -> ((ToggleButton) toggle).setDisable(busy));
        refreshButton.setDisable(busy || discovering);
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

        selectFile(selected.toPath());
    }

    private void selectFile(Path selectedPath)
    {
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

    private ToggleButton createDeviceCard(DiscoveredDevice device)
    {
        Label avatar = new Label(initials(device.name()));
        avatar.getStyleClass().add("device-avatar");
        Label name = new Label(device.name());
        name.getStyleClass().add("device-name");
        name.setMaxWidth(120);
        name.setWrapText(true);

        VBox content = new VBox(8, avatar, name);
        content.setAlignment(Pos.CENTER);
        ToggleButton card = new ToggleButton();
        card.setGraphic(content);
        card.setUserData(device);
        card.setToggleGroup(deviceSelection);
        card.getStyleClass().add("device-card");
        card.setMinSize(116, 126);
        card.setPrefSize(116, 126);
        card.setMaxSize(116, 126);
        return card;
    }

    private String initials(String name)
    {
        String[] parts = name.trim().split("\\s+");
        if (parts.length > 1)
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

}
