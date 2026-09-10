package presentation.component;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public final class StatusBanner extends HBox
{
    public enum Type { NEUTRAL, ACTIVE, SUCCESS, ERROR }

    private final Label icon = new Label("●");
    private final Label text = new Label();

    public StatusBanner(String initialText)
    {
        setSpacing(10);
        getStyleClass().add("status-banner");
        icon.getStyleClass().add("status-icon");
        text.getStyleClass().add("status-text");
        getChildren().addAll(icon, text);
        setStatus(initialText, Type.NEUTRAL);
    }

    public void setStatus(String value, Type type)
    {
        text.setText(value);
        getStyleClass().removeAll("status-neutral", "status-active", "status-success", "status-error");
        getStyleClass().add("status-" + type.name().toLowerCase());
    }
}
