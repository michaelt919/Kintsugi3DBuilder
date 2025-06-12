/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.io.primaryview;

import javafx.scene.image.Image;
import kintsugi3d.builder.core.ViewSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class GenericPrimaryViewSelectionModel implements PrimaryViewSelectionModel
{
    private static final Logger log = LoggerFactory.getLogger(GenericPrimaryViewSelectionModel.class);

    private final String name;
    private final ViewSet viewSet;

    private GenericPrimaryViewSelectionModel(String name, ViewSet viewSet)
    {
        this.name = name;
        this.viewSet = viewSet;
    }

    public static PrimaryViewSelectionModel createInstance(String name, ViewSet viewSet)
    {
        return new GenericPrimaryViewSelectionModel(name, viewSet);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public List<View> getViews()
    {
        return IntStream.range(0, viewSet.getCameraPoseCount())
            .mapToObj(i -> new View(viewSet.getImageFileName(i)))
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<Image> getThumbnails()
    {
        // TODO implement thumbnails for generic case
        return List.of();
    }

    @Override
    public Optional<String> findFullResImagePath(String imageName) {
        for (int i = 0; i < viewSet.getImageFiles().size(); ++i){
            String fileName = viewSet.getImageFileName(i);
            if (fileName.matches(".*" + imageName + ".*")){
                return Optional.of(viewSet.getFullResImageFile(i).getPath());
            }
        }
        return Optional.empty();
    }
}