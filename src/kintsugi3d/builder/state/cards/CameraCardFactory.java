/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state.cards;

import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.gl.vecmath.IntVector2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CameraCardFactory implements ProjectDataCardFactory
{
    ViewSet viewSet;

    public CameraCardFactory(ViewSet viewSet)
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
                }},
                Map.of("Delete Image", () ->
                    {
                        cardsModel.confirm("Delete Image", "Delete Image?", "This will delete the image from the project.",
                            ()-> {
                                try
                                {
                                    viewSet.deleteCamera(fullResFile);
                                }
                                catch (IOException e)
                                {
                                    throw new RuntimeException(e);
                                }
                                finally
                                {
                                    cardsModel.setCardList(createAllCards(cardsModel));
                                }
                            });
                    },
                   "Disable Image", () -> {}
                )
            );
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
//            viewSet.deleteCamera(index);
        }

        cardsModel.deleteCard(card);
    }

}
