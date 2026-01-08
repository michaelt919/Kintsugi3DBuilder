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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class GenericViewSelectionModel implements ViewSelectionModel
{
    private final String name;
    private final ViewSet viewSet;

    public GenericViewSelectionModel(String name, ViewSet viewSet)
    {
        this.name = name;
        this.viewSet = viewSet;
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
    public Map<Integer, Image> getThumbnailMap()
    {
        // TODO implement thumbnails for generic case
        return new HashMap<>(16);
    }

    @Override
    public Optional<String> findFullResImagePath(String imageName)
    {
        for (int i = 0; i < viewSet.getImageFiles().size(); ++i)
        {
            String fileName = viewSet.getImageFileName(i);
            if (fileName.matches(".*" + imageName + ".*"))
            {
                return Optional.of(viewSet.getFullResImageFile(i).getPath());
            }
        }
        return Optional.empty();
    }
}