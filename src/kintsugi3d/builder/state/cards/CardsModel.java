package kintsugi3d.builder.state.cards;

import java.util.List;

public interface CardsModel
{
    List<ProjectDataCard> getCardList();
    void setCardList(List<ProjectDataCard> cards);

    void deleteCard(ProjectDataCard card);
    void confirm(String title, String header, String message, Runnable onConfirm);
}
