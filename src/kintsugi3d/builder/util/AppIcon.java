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

package kintsugi3d.builder.util;

import javafx.scene.image.Image;

import java.io.File;
import java.net.MalformedURLException;

public final class AppIcon
{
    public static final String PATH = "Kintsugi3D-icon.png";

    private static volatile Image image;
    private static final Object INITIALIZATION_LOCK = new Object();

    private AppIcon()
    {
    }

    public static Image getImage()
    {
        if (image != null)
        {
            //noinspection StaticVariableUsedBeforeInitialization
            return image;
        }
        else
        {
            throw new IllegalStateException("App icon has not been initialized.");
        }
    }

    public static void initialize() throws MalformedURLException
    {
        //noinspection SynchronizationOnStaticField
        synchronized (INITIALIZATION_LOCK)
        {
            if (image == null)
            {
                image = new Image(new File(PATH).toURI().toURL().toExternalForm());
            }
        }
    }
}
