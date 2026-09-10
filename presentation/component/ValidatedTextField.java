package presentation.component;

import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import presentation.validation.ValidationResult;

import java.util.Objects;
import java.util.function.Function;

public final class ValidatedTextField extends VBox
{
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");
    private final TextField input = new TextField();
    private final Label error = new Label();
    private Function<String, ValidationResult> validator = value -> ValidationResult.ok();
    private boolean validationVisible;

    public ValidatedTextField(String label, String initialValue, String prompt)
    {
        setSpacing(7);
        getStyleClass().add("form-field");

        Label title = new Label(label);
        title.getStyleClass().add("field-label");
        input.setText(initialValue);
        input.setPromptText(prompt);
        input.setMaxWidth(Double.MAX_VALUE);
        error.getStyleClass().add("field-error");
        error.setManaged(false);
        error.setVisible(false);
        getChildren().addAll(title, input, error);

        input.focusedProperty().addListener((observable, wasFocused, isFocused) ->
        {
            if (!isFocused) validate();
        });
        input.textProperty().addListener((observable, oldValue, newValue) ->
        {
            if (validationVisible) validate();
        });
    }

    public void setValidator(Function<String, ValidationResult> validator)
    {
        this.validator = Objects.requireNonNull(validator);
    }

    public boolean validate()
    {
        validationVisible = true;
        ValidationResult result = validator.apply(value());
        input.pseudoClassStateChanged(INVALID, !result.valid());
        error.setText(result.message());
        error.setManaged(!result.valid());
        error.setVisible(!result.valid());
        return result.valid();
    }

    public String value()
    {
        return input.getText().trim();
    }

    public void setInputDisabled(boolean disabled)
    {
        input.setDisable(disabled);
    }

    public TextField input()
    {
        return input;
    }
}
