package kintsugi3d.builder.state.cards;

import java.util.Map;

public interface TabsModel
{
    void addTab(String tabName, ProjectDataCardFactory cardFactory);
    void clearTabs();
    CardsModel getTab(String label);
    Map<String, CardsModel> getTabsMap();
}
