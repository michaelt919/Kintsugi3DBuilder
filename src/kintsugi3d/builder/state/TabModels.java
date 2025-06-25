package kintsugi3d.builder.state;

import kintsugi3d.builder.javafx.internal.CardsModelImpl;

import java.util.List;

public interface TabModels {
    CardsModelImpl getCardModel(String label);
    List<CardsModelImpl> getCardModels();
    void addCardsModel(CardsModelImpl model);
    void setCardModel(String label, CardsModelImpl model);
}
