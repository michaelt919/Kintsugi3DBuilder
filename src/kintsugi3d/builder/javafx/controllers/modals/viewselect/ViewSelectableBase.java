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

package kintsugi3d.builder.javafx.controllers.modals.viewselect;

import javafx.stage.Window;
import kintsugi3d.builder.io.primaryview.ViewSelectionModel;

public abstract class ViewSelectableBase implements ViewSelectable
{
    private Window modalWindow;
    private ViewSelectionModel viewSelectionModel;

    private String primaryView;
    private double primaryViewRotation;

    @Override
    public boolean needsRefresh(ViewSelectable oldInstance)
    {
        // always refresh by default (only downside is performance)
        return true;
    }

    @Override
    public String getAdvanceLabelOverride()
    {
        return "Skip";
    }

    @Override
    public Window getModalWindow()
    {
        return modalWindow;
    }

    @Override
    public void setModalWindow(Window modalWindow)
    {
        this.modalWindow = modalWindow;
    }

    @Override
    public ViewSelectionModel getViewSelectionModel()
    {
        return this.viewSelectionModel;
    }

    protected void setViewSelectionModel(ViewSelectionModel viewSelectionModel)
    {
        this.viewSelectionModel = viewSelectionModel;
    }

    @Override
    public String getViewSelection()
    {
        return primaryView;
    }

    @Override
    public void selectView(String viewName, double viewRotation)
    {
        this.primaryView = viewName;
        this.primaryViewRotation = viewRotation;
    }

    @Override
    public double getViewRotation()
    {
        return primaryViewRotation;
    }
}
