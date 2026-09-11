package presentation;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import infrastructure.DeviceDiscoveryService;
import presentation.controller.ReceiveController;
import presentation.controller.SendController;
import presentation.view.MainView;
import presentation.view.ReceiveView;
import presentation.view.SendView;
import service.UpdateService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class DropLinkApplication extends Application
{
    private SendController sendController;
    private ReceiveController receiveController;

    @Override
    public void start(Stage stage)
    {
        SendView sendView = new SendView(stage);
        ReceiveView receiveView = new ReceiveView(stage, DeviceDiscoveryService.localDeviceName());
        sendController = new SendController(sendView);
        receiveController = new ReceiveController(receiveView);

        Scene scene = new Scene(new MainView(sendView, receiveView), 840, 650);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("styles.css"), "JavaFX stylesheet is missing").toExternalForm());

        stage.setTitle("DropLink – Local File Transfer");
        stage.getIcons().add(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("assets/droplink.png"), "DropLink icon is missing")));
        stage.setMinWidth(760);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
        receiveController.start();
        selectLaunchFile(sendView, getParameters().getRaw());
        checkForUpdates(stage);
    }

    private void selectLaunchFile(SendView sendView, List<String> arguments)
    {
        if (arguments.isEmpty()) return;
        try
        {
            Path candidate = Path.of(arguments.getFirst()).toAbsolutePath().normalize();
            if (Files.isRegularFile(candidate)) sendView.selectFile(candidate);
        }
        catch (RuntimeException ignored)
        {
            // Invalid shell arguments are ignored without showing an error to the user.
        }
    }

    private void checkForUpdates(Stage owner)
    {
        UpdateService updates = new UpdateService();
        Task<UpdateService.UpdateInfo> task = new Task<>()
        {
            @Override
            protected UpdateService.UpdateInfo call() throws Exception
            {
                return updates.findUpdate().orElse(null);
            }
        };
        task.setOnSucceeded(event ->
        {
            UpdateService.UpdateInfo update = task.getValue();
            if (update == null) return;

            ButtonType install = new ButtonType("Jetzt aktualisieren", ButtonBar.ButtonData.OK_DONE);
            ButtonType later = new ButtonType("Später", ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert dialog = new Alert(Alert.AlertType.INFORMATION,
                    "DropLink " + update.version() + " ist verfügbar.", install, later);
            dialog.initOwner(owner);
            dialog.setTitle("DropLink-Update");
            dialog.setHeaderText("Eine neue Version ist verfügbar");
            dialog.showAndWait().filter(install::equals)
                    .ifPresent(button -> getHostServices().showDocument(update.releaseUrl()));
        });
        task.setOnFailed(event -> { /* Update checks stay silent for end users. */ });
        Thread thread = new Thread(task, "droplink-update-check");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void stop()
    {
        if (sendController != null) sendController.shutdown();
        if (receiveController != null) receiveController.stop();
    }
}
