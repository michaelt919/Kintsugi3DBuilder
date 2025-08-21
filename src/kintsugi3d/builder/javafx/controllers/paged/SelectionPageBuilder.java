package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class SelectionPageBuilder<T, FinishType> extends PageBuilder<FinishType>
{
    final SelectionPage<T> page;

    SelectionPageBuilder(SelectionPage<T> page, PageFrameController frameController, Supplier<FinishType> finisher)
    {
        super(frameController, finisher);
        this.page = page;
    }

    @Override
    public SelectionPage<T> getPage()
    {
        return this.page;
    }

    public <PageType extends Page<? super T, NextOutType>, NextOutType>
    SelectionPageBuilder<? super T, FinishType> choiceJoin(String choiceLabel, PageType joinPage)
    {
        this.page.addChoice(choiceLabel, joinPage);
        return this;
    }

    public <PageType extends Page<? super T, NextOutType>, NextOutType, ControllerType extends PageController<?>>
    SimplePageBuilder<T, NextOutType, SelectionPageBuilder<T,FinishType>> choice(String choiceLabel, String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor,
        Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType nextPage = frameController.createPage(fxmlPath, pageConstructor, controllerConstructorOverride);
        this.page.addChoice(choiceLabel, nextPage);
        return new DataPageBuilder<>(nextPage, frameController, () -> this);
    }

    public <PageType extends Page<? super T, NextOutType>, NextOutType>
    SimplePageBuilder<T, NextOutType, SelectionPageBuilder<T,FinishType>> choice(String choiceLabel, String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        return choice(choiceLabel, fxmlPath, pageConstructor, null);
    }

    public <ControllerType extends NonSupplierPageController<T>>
    SimplePageBuilder<T, T, SelectionPageBuilder<T,FinishType>> choice(String choiceLabel, String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return choice(choiceLabel, fxmlPath, SimpleDataReceiverPage<T, ControllerType>::new, controllerConstructorOverride);
    }

    public <ControllerType extends NonSupplierPageController<T>>
    SimplePageBuilder<T, T, SelectionPageBuilder<T,FinishType>> choice(String choiceLabel, String fxmlPath)
    {
        return this.<ControllerType>choice(choiceLabel, fxmlPath, null);
    }

    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder<SelectionPageBuilder<T,FinishType>> choiceNonData(String choiceLabel, String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        SimpleNonDataPage<ControllerType> nextPage =
            frameController.createPage(fxmlPath, SimpleNonDataPage<ControllerType>::new, controllerConstructorOverride);
        this.page.addChoice(choiceLabel, nextPage);
        return new NonDataPageBuilder<>(nextPage, frameController, () -> this);
    }

    public <ControllerType extends NonSupplierPageController<Object>>
    NonDataPageBuilder<SelectionPageBuilder<T,FinishType>> choiceNonData(String choiceLabel, String fxmlPath)
    {
        return this.<ControllerType>choiceNonData(choiceLabel, fxmlPath, null);
    }

    public <PageType extends SelectionPage<T>, ControllerType extends PageController<T>>
    SelectionPageBuilder<T, FinishType> choiceSubSelect(String choiceLabel, String subprompt, BiFunction<String, FXMLLoader, PageType> pageConstructor, Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType nextPage = frameController.createPage(SELECTION_PAGE_FXML, pageConstructor, controllerConstructorOverride);
        nextPage.setPrompt(subprompt);
        this.page.addChoice(choiceLabel, nextPage);
        return new SelectionPageBuilder<>(nextPage, frameController, finisher);
    }

    public <PageType extends SelectionPage<T>, ControllerType extends PageController<T>>
    SelectionPageBuilder<T, FinishType> choiceSubSelect(String choiceLabel, String subprompt, BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        return this.<PageType, ControllerType>choiceSubSelect(choiceLabel, subprompt, pageConstructor, null);
    }

    public <ControllerType extends SelectionPageController<T>>
    SelectionPageBuilder<T, FinishType> choiceSubSelect(String choiceLabel, String subprompt, Supplier<ControllerType> controllerConstructorOverride)
    {
        return choiceSubSelect(choiceLabel, subprompt, SimpleSelectionPage<T, ControllerType>::new, controllerConstructorOverride);
    }

    public SelectionPageBuilder<T, FinishType> choiceSubSelect(String choiceLabel, String subprompt)
    {
        return this.<SelectionPageController<T>>choiceSubSelect(choiceLabel, subprompt, null);
    }
}
