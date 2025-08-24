package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

import java.util.*;

public abstract class SelectionPageBase<T, ControllerType extends SelectionPageController<T>>
    extends PageBase<T, T, ControllerType> implements SelectionPage<T>
{
    private String prompt = "Select an option:";
    private final HashMap<String, Page<? super T, ?>> choicePages = new LinkedHashMap<>(4);

    SelectionPageBase(String fxmlFile, FXMLLoader loader)
    {
        super(fxmlFile, loader);
    }

    @Override
    public String getPrompt()
    {
        return prompt;
    }

    @Override
    public void setPrompt(String prompt)
    {
        this.prompt = prompt;
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

        if (page.getPrevPage() == null)
        {
            // set default link back (useful for fallback pages which weren't accessed via next button)
            page.setPrevPage(this);
        }
    }

    @Override
    public void selectChoice(String choiceLabel)
    {
        setNextPage(getChoicePage(choiceLabel));
    }

    @Override
    public Set<Map.Entry<String, Page<? super T, ?>>> getChoices()
    {
        return Collections.unmodifiableSet(choicePages.entrySet());
    }
}
