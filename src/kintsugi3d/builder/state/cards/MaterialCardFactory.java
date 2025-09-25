package kintsugi3d.builder.state.cards;

import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.fit.decomposition.BasisResources;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.resources.project.specular.SpecularMaterialResources;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MaterialCardFactory implements ProjectDataCardFactory
{
    private final ProjectInstance<?> instance;

    public MaterialCardFactory(ProjectInstance<?> instance)
    {
        this.instance = instance;
    }

    public ProjectDataCard createCard(CardsModel cardsModel, SpecularMaterialResources<?> resources, int cardIndex)
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

        return new ProjectDataCard("Material " + cardIndex, thumbnailPath, Map.of(),
            Map.of("Delete Material", () ->
                Rendering.runLater(() -> // needs to run on graphics thread to replace GPU resources
                {
                    try
                    {
                        resources.deleteBasisMaterial(cardIndex);
                    }
                    finally // even if an exception is thrown, want to make sure we're in sync with the current state.
                    {
                        // hard reset of cards list to re-number, etc.
                        cardsModel.setCardList(createAllCards(cardsModel));
                    }
                })));
    }

    @Override
    public List<ProjectDataCard> createAllCards(CardsModel cardsModel)
    {
        if (instance.getResources() != null)
        {
            SpecularMaterialResources<?> resources = instance.getResources().getSpecularMaterialResources();
            BasisResources<?> basisResources = resources.getBasisResources();
            if (basisResources != null)
            {
                return IntStream.range(0, basisResources.getBasisCount())
                    .mapToObj(i -> createCard(cardsModel, resources, i))
                    .collect(Collectors.toUnmodifiableList());
            }
        }

        // If not yet initialized, return empty list.
        return List.of();
    }
}
