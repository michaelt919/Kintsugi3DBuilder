package kintsugi3d.builder.state.cards;

import java.util.List;

public interface CardsModel
{
    List<ProjectDataCard> getCardList();
    void deleteCard(ProjectDataCard card);
}
