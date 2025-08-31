package kintsugi3d.builder.javafx.internal;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.state.TabModels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class TabModelsImpl implements TabModels {
    private LinkedHashMap<String, CardsModel> models; //CardsModelImpl
    private ObservableMap<String, CardsModel> observableModels;
    //private int modelCount;

    public TabModelsImpl() {
        models = new LinkedHashMap<>() {{
            put("Cameras", new ObservableCardsModel("Cameras"));
            put("Textures", new ObservableCardsModel("Textures"));
            //put("Shaders", new CardsModelImpl("Shaders"));
        }};
        observableModels = FXCollections.observableMap(models);
    }

    @Override
    public CardsModel getCardsModel(String label) {
        return observableModels.get(label);
    }

    @Override
    public List<CardsModel> getAllCardsModels() {
        return new ArrayList<>(models.values());
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
        tabCardsModels.forEach(model -> models.put(model.getModelLabel(), model));

        observableModels.clear();
        tabCardsModels.forEach(model -> observableModels.put(model.getModelLabel(), model));
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
