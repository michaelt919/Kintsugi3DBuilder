package kintsugi3d.builder.state.cards;

import java.util.List;

@FunctionalInterface
public interface ProjectDataCardFactory
{
    List<ProjectDataCard> createAllCards(CardsModel cardsModel);
}
