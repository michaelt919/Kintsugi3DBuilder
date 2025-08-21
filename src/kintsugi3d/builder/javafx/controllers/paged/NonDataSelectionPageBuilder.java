package kintsugi3d.builder.javafx.controllers.paged;

import java.util.function.Supplier;

public class NonDataSelectionPageBuilder<FinishType> extends SelectionPageBuilder<Object, FinishType>
{
    NonDataSelectionPageBuilder(SelectionPage<Object> page, PageFrameController frameController, Supplier<FinishType> finisher)
    {
        super(page, frameController, finisher);
    }

    @Override
    public <ControllerType extends SelectionPageController<Object>>
    NonDataSelectionPageBuilder<FinishType> choiceSubSelect(String choiceLabel, String subprompt, Supplier<ControllerType> controllerConstructorOverride)
    {
        SimpleNonDataSelectionPage<ControllerType> nextPage = frameController.createPage(
            SELECTION_PAGE_FXML, SimpleNonDataSelectionPage<ControllerType>::new, controllerConstructorOverride);
        nextPage.setPrompt(subprompt);
        this.page.addChoice(choiceLabel, nextPage);
        return new NonDataSelectionPageBuilder<>(nextPage, frameController, finisher);
    }

    @Override
    public NonDataSelectionPageBuilder<FinishType> choiceSubSelect(String choiceLabel, String subprompt)
    {
        return this.<SelectionPageController<Object>>choiceSubSelect(choiceLabel, subprompt, null);
    }
}
