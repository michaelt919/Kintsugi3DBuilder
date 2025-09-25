package kintsugi3d.builder.javafx.multithread;

import javafx.application.Platform;
import kintsugi3d.builder.state.cards.CardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCard;

import java.util.List;

public class SynchronizedCardsModel implements CardsModel
{
    private final CardsModel base;

    public SynchronizedCardsModel(CardsModel base)
    {
        this.base = base;
    }

    @Override
    public List<ProjectDataCard> getCardList()
    {
        return base.getCardList();
    }

    @Override
    public void deleteCard(ProjectDataCard card)
    {
        Platform.runLater(() -> base.deleteCard(card));
    }
}
