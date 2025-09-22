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

package kintsugi3d.builder.javafx.controllers.modals.workflow;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class SpecularBasisFitController extends SpecularTexturesFitController
{
    @FXML private TextField basisCountTextField;
    @FXML private TextField mfdResolutionTextField;
    @FXML private TextField specularComplexityTextField;
    @FXML private TextField specularMinWidthTextField;
    @FXML private TextField specularSmoothnessTextField;
    @FXML private TextField metallicityTextField;
    @FXML private CheckBox translucencyCheckBox;

    @Override
    public void initPage()
    {
        super.initPage();

        bindIntegerSetting(basisCountTextField, "basisCount", 0, 256);
        bindNormalizedSetting(specularMinWidthTextField, "specularMinWidthFrac");
        bindNormalizedSetting(specularSmoothnessTextField, "specularMaxWidthFrac");
        bindBooleanSetting(translucencyCheckBox, "constantTermEnabled");
        bindIntegerSetting(mfdResolutionTextField, "basisResolution", 0, 8192);
        bindNormalizedSetting(specularComplexityTextField, "basisComplexityFrac");
        bindNormalizedSetting(metallicityTextField, "metallicity");
    }

    @Override
    protected boolean shouldOptimizeBasis()
    {
        return true;
    }
}
