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

package kintsugi3d.builder.javafx.internal;

import kintsugi3d.builder.core.ProjectInstance;
import kintsugi3d.builder.core.ViewSet;

public class InstanceModel
{
    private ProjectInstance instance;
    private ViewSet viewSet;

    /**
     * Global method to set both the instance and viewset currently in use. Needs both at
     * once to use.
     * @param currentInstance
     * @param currentViewSet
     */
    public void setInstances(ProjectInstance<?> currentInstance, ViewSet currentViewSet)
    {
        instance = currentInstance;
        viewSet = currentViewSet;
    }

    /**
     * returns the instance stored in this global class.
     * @return instance
     */
    public ProjectInstance<?> getProjectInstance(){ return instance; }

    /**
     * returns the viewset stored in this global class.
     * @return viewSet
     */
    public ViewSet getViewSet() {return viewSet; }
}
