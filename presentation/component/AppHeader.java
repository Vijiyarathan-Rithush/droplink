package presentation.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

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
        getChildren().addAll(mark, title);
    }
}
