package tetzlaff.ibr.gui2.controllers.menu_bar;//Created by alexk on 7/31/2017.

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.ResourceBundle;

public class LoadOptionsController implements Initializable{
    @FXML private CheckBox compressedImages;
    @FXML private CheckBox mipmaps;
    @FXML private CheckBox depthImages;
    @FXML private VBox root;
    @FXML private TextField depthWidth;
    @FXML private TextField depthHeight;

    private IntegerProperty w = new SimpleIntegerProperty(1024);
    private IntegerProperty h = new SimpleIntegerProperty(1024);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTextAndProp(w, depthWidth);
        setupTextAndProp(h, depthHeight);

        depthWidth.disableProperty().bind(depthImages.selectedProperty().not());
        depthHeight.disableProperty().bind(depthImages.selectedProperty().not());


    }

    private void setupTextAndProp(IntegerProperty prop, TextField tex){
        StringConverter<Number> ISC = new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                if (object != null) {
                    return Integer.toString(object.intValue());
                } else return "1024";
            }

            @Override
            public Number fromString(String string) {
                try {
                    return Integer.valueOf(string);
                }catch (NumberFormatException nfe){
                    return 1024;
                }
            }
        };
        tex.textProperty().bindBidirectional(prop, ISC);
        tex.focusedProperty().addListener((ob,o,n)->{
            if(o&&!n){
                tex.setText(prop.getValue().toString());
            }
        });
        prop.addListener((ob,o,n)->{
            if(n.intValue() < 1) prop.setValue(1);
        });
    }

    private LoadSettings loadSettingsCash = null;
    public void bind(LoadSettings loadSettings){
        compressedImages.selectedProperty().bindBidirectional(loadSettings.compression);
        mipmaps.selectedProperty().bindBidirectional(loadSettings.mipmaps);
        depthImages.selectedProperty().bindBidirectional(loadSettings.depthImages);
        w.bindBidirectional(loadSettings.depthWidth);
        h.bindBidirectional(loadSettings.depthHeight);
        loadSettingsCash = loadSettings;

        root.getScene().getWindow().setOnCloseRequest(param -> unbind());
    }

    private void unbind(){
        if(loadSettingsCash != null){
            compressedImages.selectedProperty().bindBidirectional(loadSettingsCash.compression);
            mipmaps.selectedProperty().bindBidirectional(loadSettingsCash.mipmaps);
            depthImages.selectedProperty().bindBidirectional(loadSettingsCash.depthImages);
            w.bindBidirectional(loadSettingsCash.depthWidth);
            h.bindBidirectional(loadSettingsCash.depthHeight);
        }
        System.out.println("unbind");
    }
}
