package kintsugi3d.builder.javafx.controllers.sidebar;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import kintsugi3d.builder.javafx.internal.ObservableCardsModel;
import kintsugi3d.builder.state.ProjectDataCard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class CardTabController
{
    @FXML private VBox tab;
    @FXML private TextField searchbar;
    @FXML private VBox vbox;
    @FXML private ScrollPane scrollpane;

    private double scrollPosition = 0;

    private final ObservableList<CardController> cardControllers = FXCollections.observableArrayList();
    private final FilteredList<CardController> searchList = new FilteredList<>(cardControllers);

    private ObservableCardsModel cardsModel;

    public void init(ObservableCardsModel cardsModel)
    {
        this.cardsModel = cardsModel;
        Collection<VBox> displayCards = new ArrayList<>(cardsModel.getCardList().size());
        for (ProjectDataCard card : cardsModel.getCardList())
        {
            displayCards.add(createDataCard(card).getCard());
        }
        // Add all at once to avoid repeated listener triggers.
        vbox.getChildren().addAll(displayCards);
        createListeners();
    }

    private CardController createDataCard(ProjectDataCard card)
    {
        FXMLLoader loader = new FXMLLoader();
        try
        {
            loader.setLocation(getClass().getResource("/fxml/main/leftpanel/DataCard.fxml"));
            loader.load();
            CardController newCardController = loader.getController();
            newCardController.init(cardsModel, card);
            cardControllers.add(newCardController);
            return newCardController;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not load DataCard.fxml!", e);
        }
    }

    // For replace operations
    private CardController createDataCard(ProjectDataCard card, int index)
    {
        FXMLLoader loader = new FXMLLoader();
        try
        {
            loader.setLocation(getClass().getResource("/fxml/main/leftpanel/DataCard.fxml"));
            loader.load();
            CardController newCardController = loader.getController();
            newCardController.setCardVisibility(false);
            newCardController.init(cardsModel, card);
            cardControllers.set(index, newCardController);
            return newCardController;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to load DataCard.fxml", e);
        }
    }

    public void setVisible(boolean visibility)
    {
        tab.setVisible(visibility);
        tab.setManaged(visibility);
    }

    /**
     * Does not recreate the listener!!!
     */
    public void refreshCardList()
    {
        vbox.getChildren().clear();
        cardControllers.clear();
        for (ProjectDataCard card : cardsModel.getCardList())
        {
            vbox.getChildren().add(createDataCard(card).getCard());
        }
    }

    private void createListeners()
    {
        cardsModel.getCardList().addListener((ListChangeListener<ProjectDataCard>) change ->
        {
            while (change.next())
            {
                if (change.wasRemoved())
                {
                    for (int i = 0; i < change.getRemovedSize(); i++)
                    {
                        cardControllers.remove(change.getFrom());
                    }
                }
                if (change.wasAdded())
                {
                    for (int i = change.getFrom(); i < change.getTo(); i++)
                    {
                        createDataCard(cardsModel.getCardList().get(i));
                    }
                }
                if (change.wasReplaced())
                {
                    for (int i = change.getFrom(); i < change.getTo(); i++)
                    {
                        createDataCard(cardsModel.getCardList().get(i), i);
                    }
                }
            }
        });

        searchList.addListener((ListChangeListener<CardController>) change ->
        {
            while (change.next())
            {
                if (change.wasRemoved())
                {
                    for (int i = 0; i < change.getRemovedSize(); i++)
                    {
                        vbox.getChildren().remove(change.getFrom());
                    }
                }
                if (change.wasAdded())
                {
                    for (int i = change.getFrom(); i < change.getTo(); i++)
                    {
                        CardController cardController = change.getList().get(i);
                        vbox.getChildren().add(i, cardController.getCard());
                    }
                }
                if (change.wasReplaced())
                {
                    for (int i = change.getFrom(); i < change.getTo(); i++)
                    {
                        vbox.getChildren().set(i, change.getList().get(i).getCard());
                    }
                }
            }
        });

        // Fires when scrolling. Updates Viewport Visibility
        scrollpane.vvalueProperty().addListener((ChangeListener<? super Number>) (observableValue, oldValue, newValue) ->
        {
            scrollPosition = newValue.doubleValue();
            updateViewportVisibility();
        });

        // Search Bar Listener
        searchbar.textProperty().addListener((observable, oldValue, newValue) ->
            searchList.setPredicate(controller ->
            {
                // If search text is empty, display all items
                return controller.titleContainsString(newValue.toLowerCase(Locale.ROOT));
            }));

        // Updates the scrollpane after a datacard is added, removed, collapsed, etc.
        vbox.heightProperty().addListener(change -> updateViewportVisibility());

        // Fires when the viewport is resized
        scrollpane.viewportBoundsProperty().addListener(change -> updateViewportVisibility());
    }

    public void updateViewportVisibility()
    {
        double viewportHeight = scrollpane.getViewportBounds().getHeight();
        double contentHeight = vbox.getHeight();
        double scrollPointer = scrollPosition * contentHeight;
        for (int i = 0; i < searchList.size(); i++)
        {
            Bounds cardY = vbox.getChildren().get(i).getBoundsInParent();
            boolean inScrollRange = cardY.getMinY() < scrollPointer + viewportHeight && cardY.getMaxY() > scrollPointer - viewportHeight;
            searchList.get(i).setCardVisibility(inScrollRange);
        }
    }
}


