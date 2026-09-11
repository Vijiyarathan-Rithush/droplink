package presentation.view;

import domain.TransferRequest;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import presentation.component.PathPickerField;
import presentation.component.SectionHeader;
import presentation.component.StatusBanner;
import presentation.component.TransferProgressView;
import presentation.util.FileSizeFormatter;
import presentation.validation.InputValidators;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ReceiveView extends VBox
{
    public static final int DEFAULT_TRANSFER_PORT = 5_000;
    private final Stage owner;
    private Path outputDirectory = Path.of(System.getProperty("user.home"), "Downloads");
    private final PathPickerField directory = new PathPickerField(
            "Speicherort", outputDirectory.toString(), "Zielordner auswählen");
    private final StatusBanner status = new StatusBanner("Empfang wird vorbereitet …");
    private final VBox requestList = new VBox(12);
    private final Label emptyState = new Label("Noch keine Anfragen\nNeue Dateien erscheinen automatisch hier.");
    private final Map<UUID, RequestCard> requestCards = new HashMap<>();
    private final IntegerProperty pendingCount = new SimpleIntegerProperty();
    private final Button startButton = new Button("Aktivieren");
    private final Button stopButton = new Button("Pausieren");

    public ReceiveView(Stage owner, String deviceName)
    {
        this.owner = owner;
        setSpacing(16);
        getStyleClass().add("content-card");

        directory.setValidator(InputValidators::outputDirectory);
        directory.setOnBrowse(event -> chooseDirectory());
        startButton.getStyleClass().add("primary-button");
        stopButton.getStyleClass().add("secondary-button");
        stopButton.setManaged(false);
        stopButton.setVisible(false);

        Label identityAvatar = new Label(initials(deviceName));
        identityAvatar.getStyleClass().add("mini-device-avatar");
        Label identityName = new Label(deviceName);
        identityName.getStyleClass().add("identity-name");
        Label identityHint = new Label("Im lokalen Netzwerk sichtbar");
        identityHint.getStyleClass().add("muted-label");
        VBox identityText = new VBox(2, identityName, identityHint);
        HBox identity = new HBox(10, identityAvatar, identityText);
        identity.setAlignment(Pos.CENTER_LEFT);
        identity.getStyleClass().add("receiver-identity");

        emptyState.setWrapText(true);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setMaxWidth(Double.MAX_VALUE);
        emptyState.getStyleClass().add("request-empty-state");
        requestList.getChildren().add(emptyState);

        ScrollPane scroll = new ScrollPane(requestList);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("request-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(
                new SectionHeader("Eingehende Anfragen"),
                identity,
                directory,
                status,
                scroll,
                actions());
    }

    private HBox actions()
    {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, spacer, startButton, stopButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        return actions;
    }

    public boolean validateInputs()
    {
        return directory.validate();
    }

    public int port()
    {
        return DEFAULT_TRANSFER_PORT;
    }

    public Path outputDirectory()
    {
        return outputDirectory.toAbsolutePath().normalize();
    }

    public ReadOnlyIntegerProperty pendingCountProperty()
    {
        return pendingCount;
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
        directory.setInputDisabled(listening);
        startButton.setManaged(!listening);
        startButton.setVisible(!listening);
        stopButton.setManaged(listening);
        stopButton.setVisible(listening);
    }

    public void addRequest(UUID id, TransferRequest request, Runnable acceptHandler, Runnable rejectHandler)
    {
        if (requestCards.containsKey(id)) return;
        if (requestCards.isEmpty()) requestList.getChildren().remove(emptyState);
        RequestCard card = new RequestCard(request, acceptHandler, rejectHandler);
        requestCards.put(id, card);
        requestList.getChildren().add(0, card);
        pendingCount.set(requestCards.size());
    }

    public void setRequestBusy(UUID id)
    {
        RequestCard card = requestCards.get(id);
        if (card != null) card.setBusy();
    }

    public void updateRequestProgress(UUID id, long transferred, long total)
    {
        RequestCard card = requestCards.get(id);
        if (card != null) card.updateProgress(transferred, total);
    }

    public void removeRequest(UUID id)
    {
        RequestCard card = requestCards.remove(id);
        if (card != null) requestList.getChildren().remove(card);
        if (requestCards.isEmpty() && !requestList.getChildren().contains(emptyState))
            requestList.getChildren().add(emptyState);
        pendingCount.set(requestCards.size());
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

    private static String initials(String name)
    {
        String[] parts = name.trim().split("\\s+");
        if (parts.length > 1)
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private static final class RequestCard extends VBox
    {
        private final Button accept = new Button("Annehmen");
        private final Button reject = new Button("Ablehnen");
        private final Label state = new Label("Wartet auf deine Entscheidung");
        private final TransferProgressView progress = new TransferProgressView();

        private RequestCard(TransferRequest request, Runnable acceptHandler, Runnable rejectHandler)
        {
            setSpacing(12);
            getStyleClass().add("request-card");

            Label icon = new Label("↓");
            icon.getStyleClass().add("request-avatar");
            Label fileName = new Label(request.fileName());
            fileName.setWrapText(true);
            fileName.getStyleClass().add("request-file-name");
            Label fileSize = new Label(FileSizeFormatter.format(request.fileSize()));
            fileSize.getStyleClass().add("request-file-size");
            state.getStyleClass().add("muted-label");
            VBox details = new VBox(3, fileName, fileSize, state);
            HBox.setHgrow(details, Priority.ALWAYS);

            accept.getStyleClass().add("primary-button");
            reject.getStyleClass().add("secondary-button");
            accept.setOnAction(event -> acceptHandler.run());
            reject.setOnAction(event -> rejectHandler.run());
            HBox buttons = new HBox(8, reject, accept);
            buttons.setAlignment(Pos.CENTER_RIGHT);

            HBox header = new HBox(12, icon, details, buttons);
            header.setAlignment(Pos.CENTER_LEFT);
            getChildren().addAll(header, progress);
        }

        private void setBusy()
        {
            accept.setDisable(true);
            reject.setDisable(true);
            state.setText("Datei wird empfangen …");
        }

        private void updateProgress(long transferred, long total)
        {
            progress.update(transferred, total);
        }
    }
}
