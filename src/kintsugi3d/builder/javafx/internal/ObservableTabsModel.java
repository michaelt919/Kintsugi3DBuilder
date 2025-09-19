package kintsugi3d.builder.javafx.internal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.state.ProjectDataCard;
import kintsugi3d.builder.state.TabsModel;

import java.util.*;
import java.util.function.Function;

public class ObservableTabsModel implements TabsModel
{
    private final ObservableMap<String, ObservableCardsModel> tabs;

    public ObservableTabsModel()
    {
        Map<String, ObservableCardsModel> cardsModels = new LinkedHashMap<>(4);
        tabs = FXCollections.observableMap(cardsModels);
    }

    @Override
    public void addTab(String tabName, Function<CardsModel, List<ProjectDataCard>> cardFactory)
    {
        ObservableCardsModel newTab = new ObservableCardsModel(tabName);
        List<ProjectDataCard> dataCards = cardFactory.apply(newTab);
        newTab.setCardList(dataCards);
        tabs.put(tabName, newTab);
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

    public Collection<ObservableCardsModel> getAllTabs()
    {
        return Collections.unmodifiableCollection(tabs.values());
    }

    public ObservableMap<String, ObservableCardsModel> getObservableTabsMap()
    {
        return tabs;
    }
}
