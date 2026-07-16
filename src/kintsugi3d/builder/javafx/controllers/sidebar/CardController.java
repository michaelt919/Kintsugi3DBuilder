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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.javafx.internal.ObservableCardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCard;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class CardController
{
    @FXML private VBox dataCardPane;
    @FXML private VBox cardBody;
    @FXML private VBox borderBox;

    @FXML private Label cardTitle;
    @FXML private VBox textContent;

    @FXML private ImageView cardIcon;
    @FXML private ImageView mainImage;

    @FXML private VBox buttonBox;

    @FXML private CheckBox selectionBox;
    @FXML private Rectangle hoverRectangle;

    private String filePath;
    private String fileName;
    private UUID cardId;
    private ObservableCardsModel cardsModel;
    private Image preview;

    public void init(ObservableCardsModel cardsModel, ProjectDataCard dataCard)
    {
        this.cardsModel = cardsModel;
        this.cardId = dataCard.getCardId();
        this.setCardVisibility(false);
        filePath = dataCard.getFilePath();
        fileName = dataCard.getTitle();

        if (filePath == null) //if file path is null it disables the checkboxes for the icons
        {
            selectionBox.setVisible(false);
            selectionBox.setManaged(false);
        }

        if (dataCard.isDisabled())
        {
            dataCardPane.pseudoClassStateChanged(PseudoClass.getPseudoClass("disabled"), true);
            cardTitle.pseudoClassStateChanged(PseudoClass.getPseudoClass("disabled"), true);
        }

        cardTitle.setText(dataCard.getTitle());

        if (dataCard.getActions().stream().allMatch(Map::isEmpty))
        {
            // Hide button box if no actions are available.
            buttonBox.setVisible(false);
            buttonBox.setManaged(false);
        }

        BooleanBinding expanded = cardsModel.isExpandedProperty(cardId);
        BooleanBinding selected = cardsModel.isSelectedProperty(cardId);

        cardBody.visibleProperty().bind(expanded);
        cardBody.managedProperty().bind(expanded);
        selected.addListener((observable, oldValue, newValue) ->
        {
            if (newValue)
            {
                borderBox.setStyle("-fx-border-color: black; -fx-border-width: 2px;");
                dataCardPane.setStyle("-fx-padding: 2px;");
            }
            else
            {
                borderBox.setStyle("");
                dataCardPane.setStyle("-fx-padding: 4px");
            }
        });

        textContent.getChildren().clear();
        dataCard.getTextContent().forEach((key, value) ->
        {
            Label label = new Label(String.format("%s:", key));
            label.getStyleClass().add("wireframeBodyStrong");

            Label caption = new Label(value);
            caption.getStyleClass().add("wireframeCaption");
            caption.setWrapText(true);
            caption.setPrefWidth(350);

            textContent.getChildren().add(label);
            textContent.getChildren().add(caption);

            Tooltip tooltip = new Tooltip(caption.getText());
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(500);
            Tooltip.install(caption, tooltip);

            VBox.setMargin(caption, new Insets(0, 0, 8, 4));
        });

        buttonBox.getChildren().clear();
        dataCard.getActions().forEach(group ->
        {
            Separator separator = new Separator();
            separator.setPrefWidth(200.0);
            separator.getStyleClass().add("card-separator");
            separator.setPadding(new Insets(16.0, 8.0, 16, 8.0)); // Top, Right, Bottom, Left
            buttonBox.getChildren().add(separator);
            group.entrySet().stream().sorted(Entry.comparingByKey()).forEach(entry ->
            {
                HBox hBox = new HBox();
                hBox.setAlignment(Pos.TOP_CENTER);

//                // Button Icon
//                ImageView imageView = new ImageView(MainApplication.getInstance().getIcon());
//                imageView.setFitHeight(16.0);
//                imageView.setFitWidth(16.0);
//                imageView.setPickOnBounds(true);
//                imageView.setPreserveRatio(true);

                // Button
                Button button = new Button(entry.getKey());
                button.setGraphicTextGap(8.0);
                button.setMnemonicParsing(false);
                button.getStyleClass().add("card-button");
                button.getStyleClass().add("wireframeBodyStrong");
                button.getStylesheets().add("file:./kintsugiStyling.css");
                button.setOnAction(event -> {

                    /*If uncommented will make it so after a button is clicked
                      boarder will remain to show it is selected*/
                    //button.getStyleClass().add("activated");
                    entry.getValue().run();
                });

                HBox.setMargin(button, new Insets(0, 0, 8, 0));
                hBox.setPadding(new Insets(0, 40.0, 0, 40.0));
                hBox.getChildren().add(button);

                buttonBox.getChildren().add(hBox);
            });
        });

        // Load the image for each card when it becomes visible.
        dataCardPane.visibleProperty().addListener((change, oldVal, newVal) ->
        {
            File imageFile = new File(dataCard.getImagePath());
            if (preview == null)
            {
                if (newVal && imageFile.exists())
                {
                    preview = new Image(imageFile.toURI().toString());
                    cardIcon.setImage(preview);
                    mainImage.setImage(preview);
                }
                else
                {
                    cardIcon.setImage(MainApplication.getIcon());
                    mainImage.setImage(MainApplication.getIcon());
                }
            }
            else
            {
                cardIcon.setImage(preview);
                mainImage.setImage(preview);
            }
        });
        mainImage.fitWidthProperty().bind(dataCardPane.widthProperty().divide(2));

        createBindings(cardIcon, selectionBox); //easy method to bind Checkbox to ImageView
        createBindings(cardIcon, hoverRectangle);//easy method to bind Rectangle to ImageView
    }

    public void setCardVisibility(boolean visibility)
    {
        dataCardPane.setVisible(visibility);
    }

    public boolean titleContainsString(String str)
    {
        return cardTitle.getText().toLowerCase(Locale.ROOT).contains(str.toLowerCase(Locale.ROOT));
    }

    @FXML
    public void cardClicked()
    {
        /*
        if (cameraCardsModel.isSelected(cardId))
        {
            cameraCardsModel.deselectCard(cardId);
        }
        else
        {
            cameraCardsModel.selectCard(cardId);
        } */
        if (cardsModel.isExpanded(cardId))
        {
            cardsModel.collapseCard(cardId);
        }
        else
        {
            cardsModel.expandCard(cardId);
        }
    }

    @FXML
    public void expansionToggleClicked(MouseEvent e)
    {
        if (cardsModel.isExpanded(cardId))
        {
            cardsModel.collapseCard(cardId);
        }
        else
        {
            cardsModel.expandCard(cardId);
        }
        e.consume();
    }

    public VBox getCard()
    {
        return dataCardPane;
    }

    /**
     *This Method is connected to the checkboxes. Whenever a box is selected or unselected
     * This method does addSelected to global tab models.
     */
    @FXML
    public void sendToDetail()
    {
        Global.state().getTabModels().addSelected(filePath, fileName);
    }

    /**
     * createBindings() will bind the checkbox width and height to imageviews width and height.
     * The method calls create imageBindings to find the proper width
     * and height needed to bind to the width properties of checkBox.
     * @param iView
     * @param cBox
     */
    private void createBindings(ImageView iView, CheckBox cBox)
    {
        //Gets double binding from createImageBinding methods
        DoubleBinding imageWidth = createImageBindingWidth(iView);
        DoubleBinding imageHeight = createImageBindingHeight(iView);

        //Binds all three width properties
        cBox.prefWidthProperty().bind(imageWidth);
        cBox.minWidthProperty().bind(imageWidth);
        cBox.maxWidthProperty().bind(imageWidth);

        //binds all three height properties
        cBox.prefHeightProperty().bind(imageHeight);
        cBox.minHeightProperty().bind(imageHeight);
        cBox.maxHeightProperty().bind(imageHeight);
    }

    /**
     * Almost the same as the createBindings for checkBoxes, but it uses only a single width property
     * for rectangles.
     * @param iView
     * @param rBox
     */
    private void createBindings(ImageView iView, Rectangle rBox)
    {
        rBox.widthProperty().bind(createImageBindingWidth(iView));

        rBox.heightProperty().bind(createImageBindingHeight(iView));
    }

    /**
     * Gets the doubleBinding needed for binding width from an imageView
     * @param iView
     * @return
     */
    private DoubleBinding createImageBindingWidth(ImageView iView)
    {
        return Bindings.createDoubleBinding(() -> iView.getLayoutBounds().getWidth(), iView.layoutBoundsProperty());
    }

    /**
     * Gets the doubleBinding needed for binding height from an imageView
     * @param iView
     * @return
     */
    private DoubleBinding createImageBindingHeight(ImageView iView)
    {
        return Bindings.createDoubleBinding(() -> iView.getLayoutBounds().getHeight(), iView.layoutBoundsProperty());
    }

    /**
     * Whenever the mouse enters selectionBox this method is called, and it will set the visibility
     * of hoverRectangle to true (which makes the icon a bit lighter)
     * These methods don't activate if selectionBox is hidden/disbled (filePath == null)
     */
    @FXML
    public void hoverStart()
    {
        hoverRectangle.setVisible(true);
    }

    /**
     * Whenever the mouse exits selectionBox this method is called, and it will set the visibility
     * of hoverRectangle to false (which makes the icon go back to normal)
     */
    @FXML
    public void hoverEnd()
    {
        hoverRectangle.setVisible(false);
    }

    /**
     * Sets the selectionBox state to false
     */
    public void updateCheckBox()
    {
        selectionBox.setSelected(false);
    }
}
