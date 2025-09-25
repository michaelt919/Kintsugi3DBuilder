package kintsugi3d.builder.javafx.multithread;

import javafx.application.Platform;
import kintsugi3d.builder.state.cards.CardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCardFactory;
import kintsugi3d.builder.state.cards.TabsModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class SynchronizedTabsModel implements TabsModel
{
    private final TabsModel base;

    public SynchronizedTabsModel(TabsModel base)
    {
        this.base = base;
    }

    @Override
    public void addTab(String tabName, ProjectDataCardFactory cardFactory)
    {
        Platform.runLater(() -> base.addTab(tabName, cardFactory));
    }

    @Override
    public void clearTabs()
    {
        Platform.runLater(base::clearTabs);
    }

    @Override
    public CardsModel getTab(String label)
    {
        return new SynchronizedCardsModel(base.getTab(label));
    }

    @Override
    public Map<String, CardsModel> getTabsMap()
    {
        Map<String, CardsModel> wrapped = new LinkedHashMap<>();

        for (var entry : base.getTabsMap().entrySet())
        {
            wrapped.put(entry.getKey(), new SynchronizedCardsModel(entry.getValue()));
        }

        return wrapped;
    }
}
