package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class SelectionPageBuilder<T, FinishType> extends PageBuilder<FinishType>
{
    private final SelectionPage<T> page;

    SelectionPageBuilder(SelectionPage<T> page, PageFrameController frameController, Supplier<FinishType> finisher)
    {
        super(frameController, finisher);
        this.page = page;
    }

    public <PageType extends Page<T, NextOutType>, NextOutType>
    SelectionPageBuilder<T, FinishType> choiceJoin(String choiceLabel, PageType joinPage)
    {
        this.page.addChoice(choiceLabel, joinPage);
        return this;
    }

    public <PageType extends Page<T, NextOutType>, NextOutType, ControllerType extends PageController<?>>
    SimplePageBuilder<T,NextOutType, SelectionPageBuilder<T,FinishType>> choice(String choiceLabel, String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor,
        Supplier<ControllerType> controllerConstructorOverride)
    {
        PageType nextPage = frameController.createPage(fxmlPath, pageConstructor, controllerConstructorOverride);
        this.page.addChoice(choiceLabel, nextPage);
        return new DataPageBuilder<>(nextPage, frameController, () -> this);
    }

    public <PageType extends Page<T, NextOutType>, NextOutType>
    SimplePageBuilder<T,NextOutType, SelectionPageBuilder<T,FinishType>> choice(String choiceLabel, String fxmlPath, BiFunction<String, FXMLLoader, PageType> pageConstructor)
    {
        return choice(choiceLabel, fxmlPath, pageConstructor, null);
    }

    public <ControllerType extends NonSupplierPageController<T>>
    SimplePageBuilder<T,T, SelectionPageBuilder<T,FinishType>> choice(String choiceLabel, String fxmlPath, Supplier<ControllerType> controllerConstructorOverride)
    {
        return choice(choiceLabel, fxmlPath, SimpleDataReceiverPage<T, ControllerType>::new, controllerConstructorOverride);
    }

    public <ControllerType extends NonSupplierPageController<T>>
    SimplePageBuilder<T,T, SelectionPageBuilder<T,FinishType>> choice(String choiceLabel, String fxmlPath)
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
}
