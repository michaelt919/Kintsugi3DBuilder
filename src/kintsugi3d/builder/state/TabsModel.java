package kintsugi3d.builder.state;

import java.util.Collection;

public interface TabsModel
{
    CardsModel addTab(String tabName);
    void clearTabs();
    CardsModel getTab(String label);
    Collection<? extends CardsModel> getAllTabs();
}
