package kintsugi3d.builder.state.cards;

import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.gl.vecmath.IntVector2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CameraCardFactory implements ProjectDataCardFactory
{
    ReadonlyViewSet viewSet;

    public CameraCardFactory(ReadonlyViewSet viewSet)
    {
        this.viewSet = viewSet;
    }

    public ProjectDataCard createCard(CardsModel cardsModel, int cardIndex)
    {
        String thumbnailPath;
        try
        {
            thumbnailPath = viewSet.findThumbnailImageFile(cardIndex).toString();
        }
        catch (FileNotFoundException e)
        {
            // Default to icon if thumbnail isn't found
            thumbnailPath = MainApplication.ICON_PATH;
        }

        try
        {
            File fullResFile = viewSet.findFullResImageFile(cardIndex);
            IntVector2 dimensions = ImageHelper.dimensionsOf(fullResFile);
            String res = dimensions.x + "x" + dimensions.y;

            return new ProjectDataCard(
                viewSet.getImageFiles().get(cardIndex).getName(), thumbnailPath,
                new LinkedHashMap<>()
                {{
                    put("Resolution", res);
                    put("Size", (fullResFile.length() / (1024 * 1024)) + " MB");
                }});
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ProjectDataCard> createAllCards(CardsModel cardsModel)
    {
        return IntStream.range(0, viewSet.getCameraPoseCount())
            .mapToObj(i -> createCard(cardsModel, i))
            .collect(Collectors.toUnmodifiableList());
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

    private static void deleteCard(CardsModel cardsModel, ProjectDataCard card, ViewSet viewSet)
    {
        UUID id = card.getCardId();
        int index = findIndexByCardUUID(cardsModel.getCardList(), id);
        if (index != -1 && viewSet != null)
        {
            viewSet.deleteCamera(index);
        }

        cardsModel.deleteCard(card);
    }
}
