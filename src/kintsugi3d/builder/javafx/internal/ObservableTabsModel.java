package kintsugi3d.builder.javafx.internal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.state.TabsModel;

import java.util.*;

public class ObservableTabsModel implements TabsModel
{
    private final ObservableMap<String, CardsModel> tabs;

    public ObservableTabsModel()
    {
        Map<String, CardsModel> cardsModels = new LinkedHashMap<>(4);
        tabs = FXCollections.observableMap(cardsModels);
    }

    @Override
    public void addTab(String tabName)
    {
        tabs.put(tabName, new ObservableCardsModel(tabName));
    }

    @Override
    public CardsModel getTab(String label)
    {
        return tabs.get(label);
    }

    @Override
    public Collection<CardsModel> getAllTabs()
    {
        return Collections.unmodifiableCollection(tabs.values());
    }

    public ObservableMap<String, CardsModel> getObservableTabsMap()
    {
        return tabs;
    }
}
