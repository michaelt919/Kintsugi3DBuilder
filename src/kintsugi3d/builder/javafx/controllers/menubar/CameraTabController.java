package kintsugi3d.builder.javafx.controllers.menubar;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import kintsugi3d.builder.javafx.internal.CameraCardsModelImpl;
import kintsugi3d.builder.javafx.internal.CameraViewListModelImpl;
import kintsugi3d.builder.resources.ProjectDataCard;
import kintsugi3d.builder.state.CameraCardsModel;

import java.util.ArrayList;
import java.util.List;

public class CameraTabController {

    @FXML private TextField camera_searchbar;
    @FXML private VBox camera_vbox;

    private List<VBox> dataCards = new ArrayList<>();
    private List<CardController> cardControllers = new ArrayList<>();

    private CameraCardsModel cameraCardsModel;

    public void initialize() {
        this.cameraCardsModel = new CameraCardsModelImpl();

        List<ProjectDataCard> dummyCards = new ArrayList<>();
        dummyCards.add(new ProjectDataCard("cardone", "first_file_name", "200x200", "500 KB", "This is a description pertaining to the FIRST card.", null, "somepath"));
        dummyCards.add(new ProjectDataCard("cardtwo", "second_file_name", "1080x200", "1000 KB", "This is a description pertaining to the SECOND card.", null, "somepath"));
        dummyCards.add(new ProjectDataCard("cardthree", "third_file_name","720x200", "1500 KB", "This is a description pertaining to the THIRD card.", null, "somepath"));
        cameraCardsModel.setCameraCardsList(dummyCards);

        List<ProjectDataCard> cards = cameraCardsModel.getCameraCardsList();
        for (int i = 0; i < cards.size(); i++) {
            camera_vbox.getChildren().add(createDataCard(cards.get(i), i));
        }

        cameraCardsModel.getItems().addListener(new ListChangeListener<ProjectDataCard>() {
            @Override
            public void onChanged(Change<? extends ProjectDataCard> c) {
                camera_vbox.getChildren().clear();
                List<ProjectDataCard> cards = cameraCardsModel.getCameraCardsList();
                for (int i = 0; i < cards.size(); i++) {
                    camera_vbox.getChildren().add(createDataCard(cards.get(i), i));
                }
            }
        });
    }

    public void init(CameraCardsModel cameraCardsModel)
    {
        this.cameraCardsModel = cameraCardsModel;
        List<ProjectDataCard> cards = cameraCardsModel.getCameraCardsList();
        for (int i = 0; i < cards.size(); i++) {
            camera_vbox.getChildren().add(createDataCard(cards.get(i), i));
        }
    }


    private VBox createDataCard(ProjectDataCard card, int index) {
        VBox newCard = null;
        CardController newCardController = null;
        FXMLLoader loader = new FXMLLoader();
        try {
            loader.setLocation(getClass().getResource("/fxml/menubar/leftpanel/DataCard.fxml"));
            newCard = loader.load();
            newCardController = loader.getController();

            dataCards.add(newCard);
            cardControllers.add(newCardController);

            newCardController.setCardTitle(card.getHeaderName());
            newCardController.setFileName(card.getFileName());
            newCardController.setResolution(card.getResolution());
            newCardController.setDescription(card.getDescription());
            newCardController.setFileSize(card.getFileSize());

            newCardController.init(cameraCardsModel, index);
        } catch (Exception e) {
            // throw new RuntimeException(e);
        }
        return newCard;
    }

    @FXML
    public void searchBarAction(KeyEvent e) {
        for (CardController cc : cardControllers) {
            cc.setCardVisibility(cc.titleContainsString(camera_searchbar.getText()));
        }
    }
}
