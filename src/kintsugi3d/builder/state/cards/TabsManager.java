package kintsugi3d.builder.state.cards;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.core.ViewSet;

import java.util.LinkedHashMap;
import java.util.Map;

public class TabsManager
{
    private final Map<String, ProjectDataCardFactory> factories = new LinkedHashMap<>();

    public TabsManager(ViewSet viewSet, ProjectInstance<?> instance)
    {
        factories.put("Cameras", new CameraCardFactory(viewSet));
        factories.put("Materials", new MaterialCardFactory(instance));
    }

    public TabsManager(ProjectInstance<?> instance)
    {
        this(instance.getActiveViewSet(), instance);
    }

    public void rebuildTabs()
    {
        TabsModel tabsModel = Global.state().getTabModels();
        tabsModel.clearTabs();

        for (var entry : factories.entrySet())
        {
            tabsModel.addTab(entry.getKey(), entry.getValue());
        }
    }

    public void refreshAllTabs()
    {
        TabsModel tabsModel = Global.state().getTabModels();

        for (var tab : tabsModel.getTabsMap().entrySet())
        {
            CardsModel cardsModel = tab.getValue();
            cardsModel.setCardList(factories.get(tab.getKey()).createAllCards(cardsModel));
        }
    }

    public void refreshTab(String tabName)
    {
        TabsModel tabsModel = Global.state().getTabModels();

        CardsModel cardsModel = tabsModel.getTab(tabName);
        cardsModel.setCardList(factories.get(tabName).createAllCards(cardsModel));
    }
}
