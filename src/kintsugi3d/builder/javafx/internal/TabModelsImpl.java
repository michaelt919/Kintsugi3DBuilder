package kintsugi3d.builder.javafx.internal;

import kintsugi3d.builder.state.TabModels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class TabModelsImpl implements TabModels {
    LinkedHashMap<String, CardsModelImpl> models;

    public TabModelsImpl() {
        models = new LinkedHashMap<String, CardsModelImpl>() {{
                put("Cameras", new CardsModelImpl("Cameras"));
                put("Textures", new CardsModelImpl("Textures"));
                //put("Shaders", new CardsModelImpl("Shaders"));
            }};
    }

    @Override
    public CardsModelImpl getCardModel(String label) {
        return models.get(label);
    }

    @Override
    public List<CardsModelImpl> getCardModels() {
        return new ArrayList<CardsModelImpl>(models.values());
    }

    @Override
    public void addCardsModel(CardsModelImpl model) {
        models.put(model.getLabel(), model);
    }

    @Override
    public void setCardModel(String label, CardsModelImpl model) {
        models.replace(label, model);
    }
}
