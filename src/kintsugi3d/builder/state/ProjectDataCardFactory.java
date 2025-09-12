package kintsugi3d.builder.state;

import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.core.MainApplication;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.UUID;

public final class ProjectDataCardFactory
{
    private ProjectDataCardFactory()
    {
    }

    public static ProjectDataCard createCameraCard(CardsModel cardsModel, ViewSet viewSet, int viewIndex)
    {
        String thumbnailPath;
        try
        {
            thumbnailPath = viewSet.findThumbnailImageFile(viewIndex).toString();
        }
        catch (FileNotFoundException e)
        {
            // Default to icon if thumbnail isn't found
            thumbnailPath = MainApplication.ICON_PATH;
        }

        ProjectDataCard card = new ProjectDataCard(
            viewSet.getImageFiles().get(viewIndex).getName(), thumbnailPath, viewSet.getCameraMetadata().get(viewIndex));
//        addReplaceButton(cardsModel, card, 0);
//        addRefreshButton(cardsModel, card, 1);
//        addDisableButton(cardsModel, card, 1);
//        addDeleteButton(cardsModel, card, 1, viewSet);
        return card;
    }

    private static int findIndexByCardUUID(List<ProjectDataCard> cardsList, UUID id)
    {
        for (int i = 0; i < cardsList.size(); i++)
        {
            if (cardsList.get(i).getCardId() == id)
            {
                return i;
            }
        }
        return -1;
    }

    private static void addDeleteButton(CardsModel cardsModel, ProjectDataCard card, int buttonGroup, ViewSet viewSet)
    {
        Runnable run = () ->
        {
            UUID id = card.getCardId();
            int index = findIndexByCardUUID(cardsModel.getCardList(), id);
            if (index != -1 && viewSet != null)
            {
                viewSet.deleteCamera(index);
            }

            cardsModel.deleteCard(card);
        };
        card.addButton(buttonGroup, "Delete", run);
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
