package kintsugi3d.builder.javafx.multithread;

import javafx.application.Platform;
import kintsugi3d.builder.state.cards.CardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCardFactory;
import kintsugi3d.builder.state.cards.TabsModel;

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
}
