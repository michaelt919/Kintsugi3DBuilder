package kintsugi3d.builder.state;

import java.util.List;
import java.util.UUID;

public interface CardsModel
{
    List<ProjectDataCard> getCardList();
    void setCardList(List<ProjectDataCard> cards);
    int findIndexByCardUUID(UUID id);
}
