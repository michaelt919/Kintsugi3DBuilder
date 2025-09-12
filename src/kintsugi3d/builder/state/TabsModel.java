package kintsugi3d.builder.state;

import java.util.List;
import java.util.function.Function;

public interface TabsModel
{
    void addTab(String tabName, Function<CardsModel, List<ProjectDataCard>> cardFactory);
    void clearTabs();
    CardsModel getTab(String label);
}
