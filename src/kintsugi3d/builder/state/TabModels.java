package kintsugi3d.builder.state;

import javafx.collections.ObservableMap;

import java.util.LinkedHashMap;
import java.util.List;

public interface TabModels {
    CardsModel getCardsModel(String label);
    List<CardsModel> getAllCardsModels();
    ObservableMap<String, CardsModel> getItems();
    void addCardsModel(CardsModel model);
    void replaceCardsModel(String label, CardsModel model);
    void setAllCardsModels(List<CardsModel> tabCardsModels);
    LinkedHashMap<String,CardsModel> getCardsModelsMap();
    void setCardsModelsMap(LinkedHashMap<String,CardsModel> cardsModelsMap);
}
