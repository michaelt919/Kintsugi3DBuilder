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

package kintsugi3d.builder.javafx.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TreeView;
import kintsugi3d.builder.app.logging.LogMessage;

public class ScrollBarHelp
{
    public static void scrollbarFix(ListView<LogMessage> listView){
        Platform.runLater(() ->
        {
            listView.applyCss();
            listView.layout();

            Node verticalBarNode = listView.lookup(".scroll-bar:vertical");

            if (verticalBarNode instanceof ScrollBar)
            {
                ScrollBar scrollBar = (ScrollBar) verticalBarNode;
                scrollBar.visibleAmountProperty().addListener((obs, oldVal, newVal) ->
                {
                    if (newVal.doubleValue() < 0.1)
                    {
                        scrollBar.setVisibleAmount(0.1);
                    }
                });
                if (scrollBar.getVisibleAmount() < 0.1)
                {
                    scrollBar.setVisibleAmount(0.1);
                }
            }
        });
    }
    public static void scrollbarFix(TreeView<String> treeView){
        Platform.runLater(() ->
        {
            treeView.applyCss();
            treeView.layout();

            Node verticalBarNode = treeView.lookup(".scroll-bar:vertical");

            if (verticalBarNode instanceof ScrollBar)
            {
                ScrollBar scrollBar = (ScrollBar) verticalBarNode;
                scrollBar.visibleAmountProperty().addListener((obs, oldVal, newVal) ->
                {
                    if (newVal.doubleValue() < 0.1)
                    {
                        scrollBar.setVisibleAmount(0.1);
                    }
                });
                if (scrollBar.getVisibleAmount() < 0.1)
                {
                    scrollBar.setVisibleAmount(0.1);
                }
            }
        });
    }
}
