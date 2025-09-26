package kintsugi3d.builder.javafx.internal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import kintsugi3d.builder.state.cards.ProjectDataCard;
import kintsugi3d.builder.state.cards.ProjectDataCardFactory;
import kintsugi3d.builder.state.cards.TabsModel;

import java.util.*;

public class ObservableTabsModel implements TabsModel
{
    private final ObservableMap<String, ObservableCardsModel> tabs;

    public ObservableTabsModel()
    {
        Map<String, ObservableCardsModel> cardsModels = new LinkedHashMap<>(4);
        tabs = FXCollections.observableMap(cardsModels);
    }

    @Override
    public void addTab(String tabName, ProjectDataCardFactory cardFactory)
    {
        ObservableCardsModel newTab = new ObservableCardsModel(tabName);
        List<ProjectDataCard> dataCards = cardFactory.createAllCards(newTab);
        newTab.setCardList(dataCards);
        tabs.put(tabName, newTab);
    }

    @Override
    public void clearTabs()
    {
        tabs.clear();
    }

    @Override
    public ObservableCardsModel getTab(String label)
    {
        return tabs.get(label);
    }

    @Override
    public Map<String, ObservableCardsModel> getTabsMap()
    {
        return Collections.unmodifiableMap(tabs);
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
