package kintsugi3d.builder.javafx.internal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.state.TabsModel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ObservableTabsModel implements TabsModel
{
    private final ObservableMap<String, ObservableCardsModel> tabs;

    public ObservableTabsModel()
    {
        Map<String, ObservableCardsModel> cardsModels = new LinkedHashMap<>(4);
        tabs = FXCollections.observableMap(cardsModels);
    }

    @Override
    public ObservableCardsModel addTab(String tabName)
    {
        ObservableCardsModel newTab = new ObservableCardsModel(tabName);
        tabs.put(tabName, newTab);
        return newTab;
    }

    @Override
    public void clearTabs()
    {
        tabs.clear();
    }

    @Override
    public CardsModel getTab(String label)
    {
        return tabs.get(label);
    }

    @Override
    public Collection<ObservableCardsModel> getAllTabs()
    {
        return Collections.unmodifiableCollection(tabs.values());
    }

    public ObservableMap<String, ObservableCardsModel> getObservableTabsMap()
    {
        return tabs;
    }
}
