package kintsugi3d.builder.javafx.multithread;

import kintsugi3d.builder.javafx.internal.CardsModelImpl;
import kintsugi3d.builder.javafx.util.MultithreadValue;
import kintsugi3d.builder.state.CardsModel;
import kintsugi3d.builder.state.TabModels;
import kintsugi3d.builder.state.impl.TabModelsBase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class TabModelsWrapper extends TabModelsBase {
    private final MultithreadValue<LinkedHashMap<String,CardsModel>> modelsList;
    private final TabModels baseModel;

    public TabModelsWrapper(TabModels baseModel) {
        super(baseModel.getAllCardsModels());
        this.baseModel = baseModel;
        this.modelsList = MultithreadValue.createFromFunctions(baseModel::getCardsModelsMap, baseModel::setCardsModelsMap);
    }

    @Override
    public CardsModel getCardsModel(String label) {
        return modelsList.getValue().get(label);
    }

    @Override
    public List<CardsModel> getAllCardsModels() {
        return new ArrayList<CardsModel>(modelsList.getValue().values());
    }

    @Override
    public void addCardsModel(CardsModel model) {
        modelsList.getValue().put(model.getModelLabel(), model);
    }

    @Override
    public void replaceCardsModel(String label, CardsModel model) {
        modelsList.getValue().replace(label,model);
    }

    @Override
    public void setAllCardsModels(List<CardsModel> tabCardsModels) {
        modelsList.getValue().clear();
        tabCardsModels.forEach(model -> {
            modelsList.getValue().put(model.getModelLabel(),model);
        });
    }

    @Override
    public LinkedHashMap<String, CardsModel> getCardsModelsMap() {
        return modelsList.getValue();
    }

    @Override
    public void setCardsModelsMap(LinkedHashMap<String, CardsModel> cardsModelsMap) {
        modelsList.setValue(cardsModelsMap);
    }

}
