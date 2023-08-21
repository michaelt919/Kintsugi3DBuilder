/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.javafx.controllers.scene;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import kintsugi3d.builder.javafx.controllers.menubar.ImgSelectionThread;
import kintsugi3d.builder.javafx.controllers.menubar.MetashapeObjectChunk;
import kintsugi3d.gl.javafx.FramebufferView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ModelExaminationController extends ImgThreadCompatibleController{
    private static final Logger log = LoggerFactory.getLogger(ModelExaminationController.class);

    @FXML private FramebufferView framebufferView;

    public void init(MetashapeObjectChunk metashapeObjectChunk, Stage injectedStage){
        this.metashapeObjectChunk = metashapeObjectChunk;
        this.framebufferView.registerKeyAndWindowEventsFromStage(injectedStage);
    }

    public void openObjectOrientation(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("fxml/scene/ObjectOrientation.fxml"));
        Parent newRoot = fxmlLoader.load();
        ObjectOrientationController controller = fxmlLoader.getController();

        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        controller.init(metashapeObjectChunk, stage);

        Scene scene = new Scene(newRoot);
        stage.setScene(scene);
        stage.show();
    }
}