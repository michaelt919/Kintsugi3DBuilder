package kintsugi3d.builder.state.impl;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import kintsugi3d.builder.javafx.multithread.CardsModelWrapper;
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.state.TabModels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class TabModelsBase implements TabModels {
    private LinkedHashMap<String, CardsModel> models; //Wrappers
    private ObservableMap<String, CardsModel> observableModels;

    public TabModelsBase(List<CardsModel> cardsModels) {
        models = new LinkedHashMap<>();
        for (CardsModel cardsModel : cardsModels) {
            models.put(cardsModel.getModelLabel(), new CardsModelWrapper(cardsModel));
        }
        observableModels = FXCollections.observableMap(models);
    }

    @Override
    public CardsModel getCardsModel(String label) {
        return models.get(label);
    }

    @Override
    public List<CardsModel> getAllCardsModels() {
        return new ArrayList<CardsModel>(models.values());
    }

    @Override
    public ObservableMap<String, CardsModel> getItems() {
        return observableModels;
    }

    @Override
    public void addCardsModel(CardsModel model) {
        models.put(model.getModelLabel(), model);
        observableModels.put(model.getModelLabel(), model);
    }

    @Override
    public void replaceCardsModel(String label, CardsModel model) {
        models.replace(label, model);
        observableModels.replace(label, model);
    }

    @Override
    public void setAllCardsModels(List<CardsModel> tabCardsModels) {
        models.clear();
        tabCardsModels.forEach(model -> {
            models.put(model.getModelLabel(), model);
        });

        observableModels.clear();
        tabCardsModels.forEach(model -> {
            observableModels.put(model.getModelLabel(), model);
        });
    }

    @Override
    public LinkedHashMap<String, CardsModel> getCardsModelsMap() {
        return models;
    }

    @Override
    public void setCardsModelsMap(LinkedHashMap<String, CardsModel> cardsModelsMap) {
        models = cardsModelsMap;
        observableModels = FXCollections.observableMap(cardsModelsMap);
    }
}
