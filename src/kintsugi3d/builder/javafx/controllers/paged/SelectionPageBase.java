package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SelectionPageBase<T, ControllerType extends SelectionPageControllerBase<? super T>>
    extends PageBase<T, T, ControllerType> implements SelectionPage<T>
{
    private final HashMap<String, Page<? super T, ?>> choicePages = new LinkedHashMap<>(4);

    SelectionPageBase(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public void initController()
    {
        this.getController().setPage(this);
        this.getController().initPage();
    }

    @Override
    public Page<? super T, ?> getChoicePage(String choiceLabel)
    {
        return choicePages.get(choiceLabel);
    }

    @Override
    public void addChoice(String choiceLabel, Page<? super T, ?> page)
    {
        choicePages.put(choiceLabel, page);
    }

    @Override
    public void selectChoice(String choiceLabel)
    {
        setNextPage(getChoicePage(choiceLabel));
    }

    @Override
    public Iterator<Map.Entry<String, Page<? super T, ?>>> iterator()
    {
        return choicePages.entrySet().iterator();
    }
}
