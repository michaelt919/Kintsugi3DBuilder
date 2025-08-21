package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Predicate;

public class SelectionPageController<T> extends PageControllerBase<T, SelectionPage<T>>
{
    private static final double BUTTON_PREF_WIDTH = 330.0;
    private static final String BUTTON_STYLE_CLASS = "wireframeSubtitle";
    private static final Insets BUTTON_INSETS = new Insets(16.0);

    private final ToggleGroup buttons = new ToggleGroup();

    @FXML private VBox rootNode;
    @FXML private Label promptLabel;

    @Override
    public Region getRootNode()
    {
        return rootNode;
    }

    @Override
    public void initPage()
    {
        this.getCanAdvanceObservable().bind(buttons.selectedToggleProperty().isNotNull());
    }

    @Override
    public void refresh()
    {
        promptLabel.setText(getPage().getPrompt());

        // Clear and repopulate
        buttons.getToggles().clear();

        // Remove all except the prompt label.
        rootNode.getChildren().removeIf(Predicate.not(promptLabel::equals));

        for (var choice : getPage().getChoices())
        {
            // create button
            ToggleButton button = new ToggleButton();
            button.setText(choice.getKey());
            button.setPrefWidth(BUTTON_PREF_WIDTH);
            button.setStyle(BUTTON_STYLE_CLASS);
            button.setPadding(BUTTON_INSETS);
            button.setOnAction(e -> onButtonAction(button, choice.getValue()));

            // add to root node and button group
            rootNode.getChildren().add(button);
            buttons.getToggles().add(button);
        }
    }

    private void onButtonAction(Toggle button, Page<? super T, ?> page)
    {
        if (button.isSelected())
        {
            this.getPage().setNextPage(page);
        }
        else
        {
            getPage().setNextPage(null);
        }
    }

    @Override
    public void receiveData(T data)
    {
    }
}
