package kintsugi3d.builder.state.cards;

import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.javafx.core.MainApplication;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MaterialCardFactory implements ProjectDataCardFactory
{
    private final ProjectInstance<?> instance;

    public MaterialCardFactory(ProjectInstance<?> instance)
    {
        this.instance = instance;
    }

    public ProjectDataCard createCard(CardsModel cardsModel, int cardIndex)
    {
        String thumbnailPath;
//        try
//        {
//            thumbnailPath = viewSet.findThumbnailImageFile(cardIndex).toString();
//        }
//        catch (FileNotFoundException e)
        {
            // Default to icon if thumbnail isn't found
            thumbnailPath = MainApplication.ICON_PATH;
        }

        return new ProjectDataCard("Material " + cardIndex, thumbnailPath);
    }

    @Override
    public List<ProjectDataCard> createAllCards(CardsModel cardsModel)
    {
        if (instance.getResources() != null)
        {
            BasisResources<?> basisResources = instance.getResources().getSpecularMaterialResources().getBasisResources();

            if (basisResources != null)
            {
                return IntStream.range(0, basisResources.getBasisCount())
                    .mapToObj(i -> createCard(cardsModel, i))
                    .collect(Collectors.toUnmodifiableList());
            }
        }

        // If not yet initialized, return empty list.
        return List.of();
    }
}
