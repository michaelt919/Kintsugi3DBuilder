/*
 *  Copyright (c) Michael Tetzlaff 2020
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.specularfit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.IBRRequestUI;
import tetzlaff.ibrelight.core.IBRelightModels;

public class SpecularFitRequestUI implements IBRRequestUI
{
    @FXML private TextField widthTextField;
    @FXML private TextField heightTextField;
    @FXML private TextField exportDirectoryField;
    @FXML private Button runButton;

    private final DirectoryChooser directoryChooser = new DirectoryChooser();

    private IBRelightModels modelAccess;
    private Stage stage;

    private File lastDirectory;

    // TODO expose this as a UI field
    private static final int BASIS_COUNT = 8;

    // TODO expose this as a UI field
    private static final int MICROFACET_DISTRIBUTION_RESOLUTION = 90;

    // TODO expose this as a UI field
    private static final int NORMAL_SMOOTHING_ITERATIONS = 0;

    // TODO expose this as a UI field
    private static final double CONVERGENCE_TOLERANCE = 0.00001;

    // TODO expose this as a UI field
    private static final double MIN_NORMAL_DAMPING = 1.0;

    // TODO expose this as a UI field
    private static final double SPECULAR_SMOOTHNESS = 0.18;

    public static SpecularFitRequestUI create(Window window, IBRelightModels modelAccess) throws IOException
    {
        String fxmlFileName = "fxml/export/SpecularFitRequestUI.fxml";
        URL url = SpecularFitRequestUI.class.getClassLoader().getResource(fxmlFileName);
        assert url != null : "Can't find " + fxmlFileName;

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Parent parent = fxmlLoader.load();
        SpecularFitRequestUI svdRequestUI = fxmlLoader.getController();
        svdRequestUI.modelAccess = modelAccess;

        svdRequestUI.stage = new Stage();
        svdRequestUI.stage.getIcons().add(new Image(new File("ibr-icon.png").toURI().toURL().toString()));
        svdRequestUI.stage.setTitle("Specular fit request");
        svdRequestUI.stage.setScene(new Scene(parent));
        svdRequestUI.stage.initOwner(window);

        return svdRequestUI;
    }

    @FXML
    private void exportDirectoryButtonAction()
    {
        this.directoryChooser.setTitle("Choose an export directory");
        if (exportDirectoryField.getText().isEmpty())
        {
            if (lastDirectory != null)
            {
                this.directoryChooser.setInitialDirectory(lastDirectory);
            }
        }
        else
        {
            File currentValue = new File(exportDirectoryField.getText());
            this.directoryChooser.setInitialDirectory(currentValue);
        }
        File file = this.directoryChooser.showDialog(stage.getOwner());
        if (file != null)
        {
            exportDirectoryField.setText(file.toString());
            lastDirectory = file;
        }
    }

    @FXML
    public void cancelButtonAction(ActionEvent actionEvent)
    {
        stage.close();
    }

    @Override
    public void prompt(Consumer<IBRRequest> requestHandler)
    {
        stage.show();

        runButton.setOnAction(event ->
        {
            //stage.close();

            SpecularFitSettings settings = new SpecularFitSettings(
                    Integer.parseInt(widthTextField.getText()),
                    Integer.parseInt(heightTextField.getText()),
                    BASIS_COUNT,
                    MICROFACET_DISTRIBUTION_RESOLUTION,
                    new File(exportDirectoryField.getText()),
                    modelAccess.getSettingsModel());
            settings.setMinNormalDamping(MIN_NORMAL_DAMPING);
            settings.setConvergenceTolerance(CONVERGENCE_TOLERANCE);
            settings.setNormalSmoothingIterations(NORMAL_SMOOTHING_ITERATIONS);
            settings.setSpecularSmoothness(SPECULAR_SMOOTHNESS);

            IBRRequest request = new SpecularFitRequest(settings);

            requestHandler.accept(request);
        });
    }
}
