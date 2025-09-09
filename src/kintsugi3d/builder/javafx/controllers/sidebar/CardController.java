package kintsugi3d.builder.javafx.controllers.sidebar;

import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import kintsugi3d.builder.javafx.core.MainApplication;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CardsModel;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

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
    private CardsModel cardsModel;
    private Image preview;

    public void init(CardsModel cardsModel, ProjectDataCard dataCard)
    {
        this.cardsModel = cardsModel;
        this.cardId = dataCard.getCardId();
        this.setCardVisibility(false);

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
            Text label = new Text(String.format("%s:", key));
            label.setFont(Font.font("Segoe UI Semibold", 12));

            Text caption = new Text(value);
            caption.getStyleClass().add("wireframeCaption");
            TextFlow flow = new TextFlow(caption);
            flow.setPrefWidth(200);

            textContent.getChildren().add(label);
            textContent.getChildren().add(flow);
            VBox.setMargin(flow, new Insets(0, 0, 4, 5));
        });

        buttonBox.getChildren().clear();
        dataCard.getActions().forEach(group ->
        {
            Separator separator = new Separator();
            separator.setPrefWidth(200.0);
            separator.getStyleClass().add("card-separator");
            separator.setPadding(new Insets(16.0, 8.0, 16, 8.0)); // Top, Right, Bottom, Left
            buttonBox.getChildren().add(separator);
            group.forEach((label, action) ->
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
                Button button = new Button(label);
                button.setGraphicTextGap(8.0);
                button.setMnemonicParsing(false);
                button.setStyle("-fx-text-fill: #CECECE;");
                button.getStyleClass().add("card-button");
                button.getStylesheets().add("file:./kintsugiStyling.css");
                button.setOnAction(event -> action.run());

                button.setFont(new Font("Segoe UI Semibold", 12.0));
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
    }

    public void setCardVisibility(boolean visibility)
    {
        dataCardPane.setVisible(visibility);
    }

    public void setCardId(UUID uuid)
    {
        this.cardId = uuid;
    }

    public void setCardTitle(String title)
    {
        this.cardTitle.setText(title);
    }

    public String getCardTitle()
    {
        return this.cardTitle.getText();
    }

    public UUID getCardId()
    {
        return this.cardId;
    }

    public boolean titleContainsString(String str)
    {
        return cardTitle.getText().toLowerCase(Locale.ROOT).contains(str.toLowerCase(Locale.ROOT));
    }

    @FXML
    public void cardClicked(MouseEvent e)
    {
//        if (cameraCardsModel.isSelected(cardId))
//        {
//            cameraCardsModel.deselectCard(cardId);
//        }
//        else
//        {
//            cameraCardsModel.selectCard(cardId);
//        }

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

    @FXML
    public void delete()
    {
        cardsModel.deleteCard(cardId);
    }

    public VBox getCard()
    {
        return dataCardPane;
    }
}
