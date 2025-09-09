package kintsugi3d.builder.util;

import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

import java.util.LinkedHashMap;

public final class ProjectDataCardFactory
{
    private ProjectDataCardFactory()
    {
    }

    public static ProjectDataCard createCameraCard(CardsModel cardsModel, String title, String imagePath, LinkedHashMap<String, String> textContent)
    {
        ProjectDataCard card = new ProjectDataCard(title, imagePath, textContent);
//        addReplaceButton(cardsModel, card, 0);
//        addRefreshButton(cardsModel, card, 1);
//        addDisableButton(cardsModel, card, 1);
//        addDeleteButton(cardsModel, card, 1);
        return card;
    }

    private static void addDeleteButton(CardsModel cardsModel, ProjectDataCard card, int buttonGroup)
    {
        Runnable run = () -> cardsModel.deleteCard(card.getCardId());
        card.addButton(buttonGroup, "Delete ", run);
    }

    private static void addDisableButton(CardsModel cardsModel, ProjectDataCard card, int buttonGroup)
    {
        Runnable run = () -> {};
        card.addButton(buttonGroup, "Disable", run);
    }

    private static void addRefreshButton(CardsModel cardsModel, ProjectDataCard card, int buttonGroup)
    {
        Runnable run = () -> {};
        card.addButton(buttonGroup, "Refresh", run);
    }

    private static void addReplaceButton(CardsModel cardsModel, ProjectDataCard card, int buttonGroup)
    {
        Runnable run = () -> {};
        card.addButton(buttonGroup, "Replace", run);
    }
}
