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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import kintsugi3d.builder.javafx.internal.ObservableCardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCard;
import kintsugi3d.builder.util.AppIcon;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

    private UUID cardId;

    private ObservableCardsModel<?> cardsModel;

    private final ObjectProperty<Image> previewImage = new SimpleObjectProperty<>(AppIcon.getImage());
    private File currentPreviewImageFile;
    private File loadedPreviewImageFile;

    public void init(ObservableCardsModel<?> cardsModel, ProjectDataCard dataCard)
    {
        this.cardsModel = cardsModel;

        this.setCardVisibility(false);

        if (dataCard.getFullResImageFilePath() == null) //if file path is null it disables the checkboxes for the icons
        {
            selectionBox.setVisible(false);
            selectionBox.setManaged(false);
        }

        cardIcon.imageProperty().bind(previewImage);
        mainImage.imageProperty().bind(previewImage);
        mainImage.fitWidthProperty().bind(dataCardPane.widthProperty().divide(2));

        // Load the image for each card when it becomes visible.
        dataCardPane.visibleProperty().addListener((change, oldVal, newVal) ->
        {
            if (newVal)
            {
                loadPreviewImage();
            }
        });

        refresh(dataCard);
    }

    private void loadPreviewImage()
    {
        if (currentPreviewImageFile.exists() && !Objects.equals(loadedPreviewImageFile, currentPreviewImageFile))
        {
            previewImage.set(new Image(currentPreviewImageFile.toURI().toString()));
            loadedPreviewImageFile = currentPreviewImageFile;
        }
    }

    public void refresh(ProjectDataCard refreshedDataCard)
    {
        this.cardId = refreshedDataCard.getCardId();

        if (refreshedDataCard.isEnabled())
        {
            dataCardPane.pseudoClassStateChanged(PseudoClass.getPseudoClass("disabled"), false);
            cardTitle.pseudoClassStateChanged(PseudoClass.getPseudoClass("disabled"), false);
        }
        else
        {
            dataCardPane.pseudoClassStateChanged(PseudoClass.getPseudoClass("disabled"), true);
            cardTitle.pseudoClassStateChanged(PseudoClass.getPseudoClass("disabled"), true);
        }

        cardTitle.setText(refreshedDataCard.getTitle());

        if (refreshedDataCard.getActions().stream().allMatch(Map::isEmpty))
        {
            // Hide button box if no actions are available.
            buttonBox.setVisible(false);
            buttonBox.setManaged(false);
        }

        BooleanExpression expanded = cardsModel.createExpandedBinding(cardId);
        BooleanExpression selected = cardsModel.createSelectedBinding(cardId);

        cardBody.visibleProperty().bind(expanded);
        cardBody.managedProperty().bind(expanded);

        // Style when selected
        borderBox.styleProperty().bind(Bindings.when(selected)
            .then("-fx-border-color: black; -fx-border-width: 2px;")
            .otherwise(""));
        dataCardPane.styleProperty().bind(Bindings.when(selected)
            .then("-fx-padding: 2px;")
            .otherwise("-fx-padding: 4px"));

        textContent.getChildren().clear();
        refreshedDataCard.getTextContent().forEach((key, value) ->
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

        ActionButtonFactory.createActionButtons(refreshedDataCard.getActions(), buttonBox,
            "card-button", "card-separator");

        currentPreviewImageFile = new File(refreshedDataCard.getThumbnailPath());

        // Invalidate any previously loaded image file in case the refresh was requested to display a file modification on disk.
        loadedPreviewImageFile = null;

        // If the card was already visible, load its preview image right away; otherwise wait for lazy loading.
        if (dataCardPane.isVisible())
        {
            // Defer loading the new image until next tick so that it doesn't slow down a bulk refresh (like enable/disable all)
            Platform.runLater(this::loadPreviewImage);
        }

        createBindings(cardIcon, selectionBox); //easy method to bind Checkbox to ImageView
        createBindings(cardIcon, hoverRectangle);//easy method to bind Rectangle to ImageView
    }

    public void setCardVisibility(boolean visibility)
    {
        dataCardPane.setVisible(visibility);
    }

    public boolean doesTitleContainString(String str)
    {
        return cardTitle.getText().toLowerCase(Locale.ROOT).contains(str.toLowerCase(Locale.ROOT));
    }

    @FXML
    public void cardClicked()
    {
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
        cardClicked();
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
    public void select()
    {
        if (cardsModel.isSelected(cardId))
        {
            cardsModel.deselectCard(cardId);
        }
        else
        {
            cardsModel.selectCard(cardId);
        }
    }

    /**
     * createBindings() will bind the checkbox width and height to imageviews width and height.
     * The method calls create imageBindings to find the proper width
     * and height needed to bind to the width properties of checkBox.
     * @param iView
     * @param cBox
     */
    private static void createBindings(ImageView iView, CheckBox cBox)
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
    private static void createBindings(ImageView iView, Rectangle rBox)
    {
        rBox.widthProperty().bind(createImageBindingWidth(iView));
        rBox.heightProperty().bind(createImageBindingHeight(iView));
    }

    /**
     * Gets the doubleBinding needed for binding width from an imageView
     * @param iView
     * @return
     */
    private static DoubleBinding createImageBindingWidth(ImageView iView)
    {
        return Bindings.createDoubleBinding(() -> iView.getLayoutBounds().getWidth(), iView.layoutBoundsProperty());
    }

    /**
     * Gets the doubleBinding needed for binding height from an imageView
     * @param iView
     * @return
     */
    private static DoubleBinding createImageBindingHeight(ImageView iView)
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
