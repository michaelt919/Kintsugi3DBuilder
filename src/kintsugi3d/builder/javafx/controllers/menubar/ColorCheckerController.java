/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.menubar;

import java.util.function.DoubleUnaryOperator;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import kintsugi3d.builder.core.LoadingModel;

public class ColorCheckerController
{
    @FXML private TextField encoded031;
    @FXML private TextField encoded090;
    @FXML private TextField encoded198;
    @FXML private TextField encoded362;
    @FXML private TextField encoded591;
    @FXML private TextField encoded900;

    private LoadingModel loadingModel;

    public void init(LoadingModel loadingModel)
    {
        DoubleUnaryOperator luminanceEncoding = loadingModel.getLuminanceEncodingFunction();

        encoded031.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.031))));
        encoded090.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.090))));
        encoded198.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.198))));
        encoded362.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.362))));
        encoded591.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.591))));
        encoded900.setText(Long.toString(Math.round(luminanceEncoding.applyAsDouble(0.900))));

        this.loadingModel = loadingModel;
    }

    @FXML
    public void applyButtonPressed()
    {
        loadingModel.setTonemapping(
            new double[] {0.031, 0.090, 0.198, 0.362, 0.591, 0.900},
            new byte[]
            {
                (byte)Integer.parseInt(encoded031.getText()),
                (byte)Integer.parseInt(encoded090.getText()),
                (byte)Integer.parseInt(encoded198.getText()),
                (byte)Integer.parseInt(encoded362.getText()),
                (byte)Integer.parseInt(encoded591.getText()),
                (byte)Integer.parseInt(encoded900.getText())
            });
    }
}
