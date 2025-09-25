package kintsugi3d.builder.state.cards;

import kintsugi3d.builder.core.ViewSet;

import java.util.LinkedHashMap;
import java.util.Map;

public class TabsManager
{
    private final Map<String, ProjectDataCardFactory> factories = new LinkedHashMap<>();

    public TabsManager(ViewSet viewSet)
    {
        factories.put("Cameras", new CameraCardFactory(viewSet));
    }

    public void refreshTabs(TabsModel tabsModel)
    {
        tabsModel.clearTabs();

        for (var entry : factories.entrySet())
        {
            tabsModel.addTab(entry.getKey(), entry.getValue());
        }
    }
}
