package presentation.component;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class SectionHeader extends VBox
{
    public SectionHeader(String title)
    {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        getChildren().add(titleLabel);
    }
}
