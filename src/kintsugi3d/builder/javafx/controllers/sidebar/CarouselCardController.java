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

package kintsugi3d.builder.javafx.controllers.sidebar;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.state.CarouselModel;
import kintsugi3d.builder.state.ReadonlyCanvasModel;
import kintsugi3d.builder.state.scene.UserShader;
import kintsugi3d.gl.javafx.FramebufferView;

/**
 * This is the controller for the carousel cards it handles what happens when the X button is clicked
 * or if the checkbox is selected. Additionally, it keeps track of which shader is for which card
 * using initData method.
 */
public class CarouselCardController
{
    private static final UserShader DEFAULT_SHADER_UNPROCESSED =
        new UserShader("Image-based", "rendermodes/ibrUntextured.frag");

    private static final UserShader DEFAULT_SHADER_PROCESSED =
        new UserShader("Material (basis)", "rendermodes/basisMaterial.frag");

    @FXML private FramebufferView framebufferView;
    @FXML private CheckBox selectedCheckbox;
    @FXML private Label shaderName;
    @FXML private HBox buttonBox;
    @FXML private AnchorPane carouselCard;
    @FXML private Button moveLeft;
    @FXML private Button moveRight;

    private CarouselModel carouselModel;
    private CarouselController carousel;
    private UserShader shader;

    /**
     * If the checkbox is selected it will apply the shader that is assigned to the card.
     * If the checkbox is already selected when the user selects it again it will
     * remove the shader / change it to the default shader.
     */
    @FXML
    public void selectedChanged()
    {
        if (selectedCheckbox.isSelected())
        {
            Global.state().getUserShaderModel().setUserShader(shader);
        }
        else
        {
            if (Global.state().getProjectModel().isProjectProcessed())
            {
                Global.state().getUserShaderModel().setUserShader(DEFAULT_SHADER_PROCESSED);
            }
            else
            {
                Global.state().getUserShaderModel().setUserShader(DEFAULT_SHADER_UNPROCESSED);
            }
        }
    }

    /**
     * assigns private variable currentShader with the shader of the current card
     * and changes the label to have the name of the shader
     * @param shader
     */
    public void init(CarouselModel carouselModel, UserShader shader, CarouselController carousel)
    {
        this.carouselModel = carouselModel;
        this.carousel = carousel;
        this.shader = shader;

        shaderName.setText(this.shader.getFriendlyName());

        /*
        Works like a listener in that it detects if there were any changes to the
        shader that is applied to the model, if there is it looks to see if it needs
        to deselect its own checkbox.
        */
        Global.state().getUserShaderModel().registerHandler(this::updateCheckboxState);
        updateCheckboxState(Global.state().getUserShaderModel().getUserShader());
    }
    public void initialize()
    {
        buttonBox.translateYProperty().bind(carouselCard.heightProperty().divide(2.0).subtract(buttonBox.heightProperty().divide(2.0)));

        Tooltip leftTip = new Tooltip();
        Tooltip rightTip = new Tooltip();
        leftTip.setText("Move Card Left");
        rightTip.setText("Move Card Right");
        leftTip.setShowDelay(Duration.millis(10));
        rightTip.setShowDelay(Duration.millis(10));
        moveLeft.setTooltip(leftTip);
        moveRight.setTooltip(rightTip);
    }

    public void setupCanvas(ReadonlyCanvasModel canvasModel)
    {
        framebufferView.setCanvas(canvasModel.getCanvas());
    }

    /**
     * Will select or deselect the shader cards checkbox if another cards checkbox is selected
     * @param activeShader
     */
    private void updateCheckboxState(UserShader activeShader)
    {
        selectedCheckbox.setSelected(shader != null && shader.equals(activeShader));
    }

    /**
     * closeCard() activates whenever X button on a carousel card is clicked, and it
     * removes the element from global carousel list of shaders.
     */
    @FXML
    public void closeCard()
    {
        if (shader != null)
        {
            /*
            If the shader that is currently applied is assigned to the card that is being closed,
            It will change the shader back to the default shader. Afterwords it will remove the
            cards shader from the global carousel shaders list.
             */
            if (shader.equals(Global.state().getUserShaderModel().getUserShader()))
            {
                if(Global.state().getProjectModel().isProjectProcessed())
                {
                    Global.state().getUserShaderModel().setUserShader(DEFAULT_SHADER_PROCESSED);
                }
                else
                {
                    Global.state().getUserShaderModel().setUserShader(DEFAULT_SHADER_UNPROCESSED);
                }
            }

            carouselModel.removeFromCarousel(shader);
        }
    }
    @FXML public void showMoveButtons()
    {
        buttonBox.setVisible(true);
    }
    @FXML public void hideMoveButtons()
    {
        buttonBox.setVisible(false);
    }
    @FXML public void moveCarouselCardLeft()
    {
        Global.state().getCarouselModel().moveCardLeft(this);
    }
    @FXML public void moveCarouselCardRight()
    {
        Global.state().getCarouselModel().moveCardRight(this);
    }
    public UserShader getShader(){ return shader;}
    public double getHBarValue(){ return carousel.getHBarValue();}
    public void setHBarValue(double pos) { carousel.setHBarPosition(pos);}
}
