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

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.RenderableInstance;
import kintsugi3d.builder.core.viewset.View;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.gl.util.ImageHelper;
import kintsugi3d.gl.vecmath.IntVector2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PhotoCardFactory extends ProjectDataCardFactoryBase<View>
{
    private static final Logger LOG = LoggerFactory.getLogger(PhotoCardFactory.class);

    public PhotoCardFactory(RenderableInstance<?> instance)
    {
        super(instance);
    }

    @Override
    public Class<View> getDataClass()
    {
        return View.class;
    }

    @Override
    public ProjectDataCard createCard(View view)
    {
        String thumbnailPath;
        try
        {
            thumbnailPath = view.findThumbnailImageFile().toString();
        }
        catch (FileNotFoundException e)
        {
            // Default to icon if thumbnail isn't found
            thumbnailPath = MainApplication.ICON_PATH;
        }

        try
        {
            File fullResFile = view.findFullResImageFile();
            IntVector2 dimensions = ImageHelper.dimensionsOf(fullResFile);
            String res = String.format("%dx%d", dimensions.x, dimensions.y);

            return new ProjectDataCard(
                view.getImageFile().getPath(), // path is used to uniquely identify views for synchronizing with backend
                view.getImageFile().getName(),
                thumbnailPath,
                new LinkedHashMap<>()
                {{
                    put("Resolution", res);
                    put("Size", (fullResFile.length() / (1024 * 1024)) + " MB");
                }},
                Map.of(
                    "Remove from Project", () ->
                        Global.state().getProjectModel().confirm("Remove Image", "Remove Image?",
                            "This will remove the image from the project.",
                            () -> getViewSet().removeViewByImageFilename(view.getImageFile())),
                    "Toggle Disabled", () -> getViewSet().toggleViewEnabled(view.getImageFile())
                ),
                !view.isEnabled()
            );
        }
        catch (RuntimeException|IOException e)
        {
            LOG.error("Error creating card", e);
            return null;
        }
    }

    @Override
    public List<? extends Map<String, Runnable>> getGlobalActions()
    {
        return List.of(Map.of(
            "Disable All", () ->
            {
                Collection<File> photosToDisable = getViewSet().getEnabledViews().stream()
                    .map(View::getImageFile)
                    .collect(Collectors.toList());

                getViewSet().setViewsEnabled(photosToDisable, false);
            },
            "Enable All", () ->
            {
                Collection<File> photosToEnable = getViewSet().getDisabledViews().stream()
                    .map(View::getImageFile)
                    .collect(Collectors.toList());

                getViewSet().setViewsEnabled(photosToEnable, true);
            }
        ));
    }

    @Override
    public List<ProjectDataCard> createAllCards()
    {
        List<ProjectDataCard> cardsList = getViewSet().getViews().stream()
            .map(this::createCard)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        cardsList = cardsList.stream().sorted(Comparator.comparing(ProjectDataCard::getTitle)).collect(Collectors.toUnmodifiableList());
        return cardsList;
    }
}
