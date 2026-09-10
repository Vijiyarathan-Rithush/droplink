package presentation.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class AppHeader extends HBox
{
    public AppHeader()
    {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);
        getStyleClass().add("app-header");

        Label mark = new Label("↗");
        mark.getStyleClass().add("brand-mark");
        Label title = new Label("DropLink");
        title.getStyleClass().add("brand-title");
        Label subtitle = new Label("Schneller Dateitransfer im lokalen Netzwerk");
        subtitle.getStyleClass().add("brand-subtitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label localBadge = new Label("●  LOKALES NETZWERK");
        localBadge.getStyleClass().add("network-badge");
        getChildren().addAll(mark, new VBox(2, title, subtitle), spacer, localBadge);
    }
}
