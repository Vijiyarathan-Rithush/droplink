package presentation.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class SectionHeader extends VBox
{
    public SectionHeader(String number, String title, String subtitle)
    {
        setSpacing(6);
        Label badge = new Label(number);
        badge.getStyleClass().add("section-number");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("section-subtitle");
        HBox heading = new HBox(12, badge, titleLabel);
        heading.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(heading, subtitleLabel);
    }
}
