package kintsugi3d.builder.state;

import java.util.List;

public interface CardsModel
{
    List<ProjectDataCard> getCardList();
    void deleteCard(ProjectDataCard card);
}
