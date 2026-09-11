package presentation;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import infrastructure.DeviceDiscoveryService;
import presentation.controller.ReceiveController;
import presentation.controller.SendController;
import presentation.view.MainView;
import presentation.view.ReceiveView;
import presentation.view.SendView;

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
        stage.setMinWidth(760);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
        receiveController.start();
    }

    @Override
    public void stop()
    {
        if (sendController != null) sendController.shutdown();
        if (receiveController != null) receiveController.stop();
    }
}
