package presentation.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.Objects;

public final class AppHeader extends HBox
{
    public AppHeader()
    {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);
        getStyleClass().add("app-header");

        ImageView mark = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/presentation/assets/droplink.png"), "DropLink icon is missing")));
        mark.setFitWidth(42);
        mark.setFitHeight(42);
        mark.setPreserveRatio(true);
        Label title = new Label("DropLink");
        title.getStyleClass().add("brand-title");
        getChildren().addAll(mark, title);
    }
}
