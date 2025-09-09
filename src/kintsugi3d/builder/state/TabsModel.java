package kintsugi3d.builder.state;

import java.util.Collection;

public interface TabsModel
{
    void addTab(String tabName);
    CardsModel getTab(String label);
    Collection<CardsModel> getAllTabs();
}
