package presentation.component;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import presentation.validation.ValidationResult;

import java.util.Objects;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PathPickerField extends VBox
{
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");
    private final TextField input = new TextField();
    private final Button browseButton = new Button("Auswählen");
    private final Label error = new Label();
    private Function<String, ValidationResult> validator = value -> ValidationResult.ok();

    public PathPickerField(String label, String initialValue, String prompt)
    {
        setSpacing(7);
        getStyleClass().add("form-field");
        Label title = new Label(label);
        title.getStyleClass().add("field-label");

        input.setText(initialValue);
        input.setPromptText(prompt);
        input.setEditable(false);
        input.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(input, Priority.ALWAYS);
        browseButton.getStyleClass().add("secondary-button");

        error.getStyleClass().add("field-error");
        error.setManaged(false);
        error.setVisible(false);
        getChildren().addAll(title, new HBox(10, input, browseButton), error);
    }

    public void setValidator(Function<String, ValidationResult> validator)
    {
        this.validator = Objects.requireNonNull(validator);
    }

    public void setOnBrowse(EventHandler<ActionEvent> handler)
    {
        browseButton.setOnAction(handler);
    }

    public void setOnPathDropped(Consumer<Path> handler)
    {
        Objects.requireNonNull(handler);
        setOnDragOver(event ->
        {
            if (event.getGestureSource() != this && event.getDragboard().hasFiles())
                event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        setOnDragDropped(event ->
        {
            boolean completed = false;
            if (event.getDragboard().hasFiles() && !event.getDragboard().getFiles().isEmpty())
            {
                handler.accept(event.getDragboard().getFiles().getFirst().toPath());
                completed = true;
            }
            event.setDropCompleted(completed);
            event.consume();
        });
    }

    public void setValue(String value)
    {
        input.setText(value == null ? "" : value);
        validate();
    }

    public String value()
    {
        return input.getText();
    }

    public boolean validate()
    {
        ValidationResult result = validator.apply(value());
        input.pseudoClassStateChanged(INVALID, !result.valid());
        error.setText(result.message());
        error.setManaged(!result.valid());
        error.setVisible(!result.valid());
        return result.valid();
    }

    public void setInputDisabled(boolean disabled)
    {
        input.setDisable(disabled);
        browseButton.setDisable(disabled);
    }
}
