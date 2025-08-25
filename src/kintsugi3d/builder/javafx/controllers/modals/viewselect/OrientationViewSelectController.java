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

package kintsugi3d.builder.javafx.controllers.modals.viewselect;

import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.core.ViewSet;

/**
 * Controller for the PrimaryViewSelector, which is now used as the orientation view selector
 */
public class OrientationViewSelectController extends ViewSelectController
{
    //TODO: --> "INFO: index exceeds maxCellCount. Check size calculations for class javafx.scene.control.skin.TreeViewSkin$1"
    //suppress warning?

    @Override
    public void initPage()
    {
        super.initPage();
        setCanConfirm(true);
    }

    @Override
    public boolean advance()
    {
        data.selectView(getSelectedViewName(), primaryImgView.getRotate());
        return true;
    }

    @Override
    public boolean confirm()
    {
        // If a view set was already loaded, apply changes.
        ViewSet currentViewSet = Global.state().getIOModel().getLoadedViewSet();
        if (currentViewSet != null)
        {
            if (data.getViewSelection() == null)
            {
                currentViewSet.setOrientationViewIndex(-1);
            }
            else
            {
                currentViewSet.setOrientationView(data.getViewSelection());
            }

            currentViewSet.setOrientationViewRotationDegrees(data.getViewRotation());
        }

        // The input source will handle loading if a view set wasn't already loaded.
        data.confirm();

        return true;
    }

    @Override
    protected String getHintText()
    {
        return "Select model orientation view";
    }

    @Override
    protected boolean allowViewRotation()
    {
        return true;
    }

    @Override
    protected boolean allowNullViewSelection()
    {
        return true;
    }

}