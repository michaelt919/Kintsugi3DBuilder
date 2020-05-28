/*
 *  Copyright (c) Michael Tetzlaff 2019
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.javafx.controllers.menubar;//Created by alexk on 7/31/2017.

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import tetzlaff.ibrelight.javafx.internal.LoadOptionsModelImpl;

public class LoadOptionsController implements Initializable
{
    @FXML private CheckBox compressedImages;
    @FXML private CheckBox alphaChannel;
    @FXML private CheckBox mipmaps;
    @FXML private CheckBox depthImages;
    @FXML private VBox root;
    @FXML private TextField depthWidth;
    @FXML private TextField depthHeight;

    private final IntegerProperty w = new SimpleIntegerProperty(1024);
    private final IntegerProperty h = new SimpleIntegerProperty(1024);

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        setupTextAndProp(w, depthWidth);
        setupTextAndProp(h, depthHeight);

        depthWidth.disableProperty().bind(depthImages.selectedProperty().not());
        depthHeight.disableProperty().bind(depthImages.selectedProperty().not());
    }

    private void setupTextAndProp(IntegerProperty prop, TextField tex)
    {
        StringConverter<Number> ISC = new StringConverter<Number>()
        {
            @Override
            public String toString(Number object)
            {
                if (object != null)
                {
                    return Integer.toString(object.intValue());
                }
                else
                {
                    return "1024";
                }
            }

            @Override
            public Number fromString(String string)
            {
                try
                {
                    return Integer.valueOf(string);
                }
                catch (NumberFormatException nfe)
                {
                    return 1024;
                }
            }
        };
        tex.textProperty().bindBidirectional(prop, ISC);
        tex.focusedProperty().addListener((ob, o, n) ->
        {
            if (o && !n)
            {
                tex.setText(prop.getValue().toString());
            }
        });
        prop.addListener((ob, o, n) ->
        {
            if (n.intValue() < 1)
            {
                prop.setValue(1);
            }
        });
    }

    private LoadOptionsModelImpl loadSettingsCache;

    public void bind(LoadOptionsModelImpl loadSettings)
    {
        compressedImages.selectedProperty().bindBidirectional(loadSettings.compression);
        alphaChannel.selectedProperty().bindBidirectional(loadSettings.alpha);
        mipmaps.selectedProperty().bindBidirectional(loadSettings.mipmaps);
        depthImages.selectedProperty().bindBidirectional(loadSettings.depthImages);
        w.bindBidirectional(loadSettings.depthWidth);
        h.bindBidirectional(loadSettings.depthHeight);
        loadSettingsCache = loadSettings;

        root.getScene().getWindow().setOnCloseRequest(param -> unbind());
    }

    private void unbind()
    {
        if (loadSettingsCache != null)
        {
            compressedImages.selectedProperty().bindBidirectional(loadSettingsCache.compression);
            alphaChannel.selectedProperty().bindBidirectional(loadSettingsCache.alpha);
            mipmaps.selectedProperty().bindBidirectional(loadSettingsCache.mipmaps);
            depthImages.selectedProperty().bindBidirectional(loadSettingsCache.depthImages);
            w.bindBidirectional(loadSettingsCache.depthWidth);
            h.bindBidirectional(loadSettingsCache.depthHeight);
        }
    }
}
