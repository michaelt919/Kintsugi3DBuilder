package kintsugi3d.builder.javafx.multithread;

import javafx.application.Platform;
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.state.ProjectDataCard;
import kintsugi3d.builder.state.TabsModel;

import java.util.List;
import java.util.function.Function;

public class SynchronizedTabsModel implements TabsModel
{
    private final TabsModel base;

    public SynchronizedTabsModel(TabsModel base)
    {
        this.base = base;
    }

    @Override
    public void addTab(String tabName, Function<CardsModel, List<ProjectDataCard>> cardFactory)
    {
        Platform.runLater(() -> base.addTab(tabName, model -> cardFactory.apply(new SynchronizedCardsModel(model))));
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
