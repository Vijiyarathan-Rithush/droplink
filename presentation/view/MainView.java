package presentation.view;

import javafx.geometry.Insets;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import presentation.component.AppHeader;

public final class MainView extends BorderPane
{
    public MainView(SendView sendView, ReceiveView receiveView)
    {
        getStyleClass().add("app-root");
        setTop(new AppHeader());

        Tab sendTab = new Tab("Senden", sendView);
        Tab receiveTab = new Tab("Empfangen", receiveView);
        sendTab.setClosable(false);
        receiveTab.setClosable(false);

        TabPane tabs = new TabPane(sendTab, receiveTab);
        tabs.getStyleClass().add("main-tabs");
        tabs.setMaxHeight(Double.MAX_VALUE);
        VBox content = new VBox(tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        content.setPadding(new Insets(0, 28, 28, 28));
        setCenter(content);
    }

}
