package presentation;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import presentation.component.DialogService;
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
        ReceiveView receiveView = new ReceiveView(stage);
        DialogService dialogs = new DialogService(stage);
        sendController = new SendController(sendView, dialogs);
        receiveController = new ReceiveController(receiveView, dialogs);

        Scene scene = new Scene(new MainView(sendView, receiveView), 900, 680);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("styles.css"), "JavaFX stylesheet is missing").toExternalForm());

        stage.setTitle("DropLink – Local File Transfer");
        stage.setMinWidth(780);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop()
    {
        if (sendController != null) sendController.cancel();
        if (receiveController != null) receiveController.stop();
    }
}
