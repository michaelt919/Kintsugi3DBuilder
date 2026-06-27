/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.state.cards;

import javafx.application.Platform;
import kintsugi3d.builder.app.Rendering;
import kintsugi3d.builder.core.ViewSet;
import kintsugi3d.builder.core.ViewSetData;
import kintsugi3d.builder.core.ViewSetDataCollection;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.gl.vecmath.IntVector2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CameraCardFactory implements ProjectDataCardFactory
{
    private static final Logger LOG = LoggerFactory.getLogger(CameraCardFactory.class);

    private final ViewSet viewSet;

    private CardsModel lastUsedCardsModel;

    public CameraCardFactory(ViewSet viewSet)
    {
        this.viewSet = viewSet;
    }

    private ProjectDataCard createCard(CardsModel cardsModel, int cardIndex, ViewSetDataCollection viewSetDataCollection)
    {
        String thumbnailPath;
        try
        {
            thumbnailPath = viewSetDataCollection.findThumbnailImageFile(cardIndex).toString();
        }
        catch (FileNotFoundException e)
        {
            // Default to icon if thumbnail isn't found
            thumbnailPath = MainApplication.ICON_PATH;
        }

        try
        {
            File fullResFile = viewSetDataCollection.findFullResImageFile(cardIndex);
            IntVector2 dimensions = ImageHelper.dimensionsOf(fullResFile);
            String res = String.format("%dx%d", dimensions.x, dimensions.y);

            ViewSetData view = viewSetDataCollection.getViewSetData().get(cardIndex);

            return new ProjectDataCard(
                view.imageFile.getPath(), // path is used to uniquely identify views for synchronizing with backend
                view.imageFile.getName(), thumbnailPath,
                new LinkedHashMap<>()
                {{
                    put("Resolution", res);
                    put("Size", (fullResFile.length() / (1024 * 1024)) + " MB");
                }},
                Map.of(
                    "Remove from Project", () ->
                        cardsModel.confirm("Remove Image", "Remove Image?", "This will remove the image from the project.",
                            () -> Rendering.runLater(() -> viewSet.deleteCamera(fullResFile))),
                    "Toggle Disabled", () ->
                            Rendering.runLater(() -> viewSet.toggleCamera(fullResFile))
                ),
                view.isDisabled
            );
        }
        catch (RuntimeException|IOException e)
        {
            LOG.error("Error creating card", e);
            return null;
        }
    }

    @Override
    public List<ProjectDataCard> createAllCards(CardsModel cardsModel)
    {
        lastUsedCardsModel = cardsModel;
        List<ProjectDataCard> cardsList = IntStream.range(0, viewSet.getEnabledCameraPoseCount())
            .mapToObj(i -> createCard(cardsModel, i, viewSet.getViewSetData()))
            .collect(Collectors.toList());
        // Make sure to also display disabled cards
        List<ProjectDataCard> disabledCardsList = IntStream.range(0, viewSet.getDisabledCameraPoseCount())
            .mapToObj(i -> createCard(cardsModel, i, viewSet.getDisabledViewSetData()))
            .collect(Collectors.toList());
        cardsList.addAll(disabledCardsList);
        cardsList = cardsList.stream().sorted(Comparator.comparing(ProjectDataCard::getTitle)).collect(Collectors.toUnmodifiableList());
        return cardsList;
    }

    @Override
    public Map<ProjectDataCard, ProjectDataCard> refreshCards(CardsModel cardsModel, Predicate<ProjectDataCard> filter)
    {
        Map<ProjectDataCard, ProjectDataCard> changes = new HashMap<>(1);

        List<ProjectDataCard> cardsList = cardsModel.getCardList();
        for (ProjectDataCard card : cardsList)
        {
            if (filter.test(card)) // Check whether the card is in the filter
            {
                int viewIndex = viewSet.findIndexOfView(card.getInternalName()); // find the view index with this view name.

                if (viewIndex >= 0)
                {
                    if (viewSet.isViewDisabled(viewIndex))
                    {
                        changes.put(card, createCard(cardsModel,
                            // Subtract # of enabled views to get index within just disabled view list.
                            viewIndex - viewSet.getEnabledCameraPoseCount(), viewSet.getDisabledViewSetData()));
                    }
                    else
                    {
                        changes.put(card, createCard(cardsModel, viewIndex, viewSet.getViewSetData()));
                    }
                }
            }
        }

        return changes;
    }

    private void updateCards()
    {
        Platform.runLater(() -> lastUsedCardsModel.setCardList(createAllCards(lastUsedCardsModel)));
    }
}
