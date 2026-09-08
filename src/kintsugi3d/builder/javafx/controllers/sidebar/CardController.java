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
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.javafx.internal.ObservableCardsModel;
import kintsugi3d.builder.state.cards.ProjectDataCard;

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

    private UUID cardId;
    private ObservableCardsModel<?> cardsModel;

    private final ObjectProperty<Image> previewImage = new SimpleObjectProperty<>(MainApplication.getIcon());
    private File currentPreviewImageFile;
    private File loadedPreviewImageFile;

    public void init(ObservableCardsModel<?> cardsModel, ProjectDataCard dataCard)
    {
        this.cardsModel = cardsModel;
        this.setCardVisibility(false);

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

    public void refresh(ProjectDataCard dataCard)
    {
        this.cardId = dataCard.getCardId();

        if (dataCard.isDisabled())
        {
            dataCardPane.pseudoClassStateChanged(PseudoClass.getPseudoClass("disabled"), true);
            cardTitle.pseudoClassStateChanged(PseudoClass.getPseudoClass("disabled"), true);
        }
        else
        {
            dataCardPane.pseudoClassStateChanged(PseudoClass.getPseudoClass("disabled"), false);
            cardTitle.pseudoClassStateChanged(PseudoClass.getPseudoClass("disabled"), false);
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

        // Style when selected
        borderBox.styleProperty().bind(Bindings.when(selected)
            .then("-fx-border-color: black; -fx-border-width: 2px;")
            .otherwise(""));
        dataCardPane.styleProperty().bind(Bindings.when(selected)
            .then("-fx-padding: 2px;")
            .otherwise("-fx-padding: 4px"));

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

        ActionButtonFactory.createActionButtons(dataCard.getActions(), buttonBox,
            "card-button", "card-separator");

        currentPreviewImageFile = new File(dataCard.getImagePath());

        // Invalidate any previously loaded image file in case the refresh was requested to display a file modification on disk.
        loadedPreviewImageFile = null;

        // If the card was already visible, load its preview image right away; otherwise wait for lazy loading.
        if (dataCardPane.isVisible())
        {
            // Defer loading the new image until next tick so that it doesn't slow down a bulk refresh (like enable/disable all)
            Platform.runLater(this::loadPreviewImage);
        }
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
}
