package tetzlaff.ibr.gui2.controllers.menu_bar.load_options;//Created by alexk on 7/31/2017.

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import tetzlaff.ibr.rendering2.to_sort.IBRLoadOptions2;
import tetzlaff.ibr.util.U;
import tetzlaff.util.SafeIntegerStringConverter;
import tetzlaff.util.SafeNumberStringConverter;

import javax.swing.event.ChangeListener;
import java.net.URL;
import java.util.ResourceBundle;

public class LoadOptionsController implements Initializable, IBRLoadOptions2{
    @FXML private CheckBox compressedImages;
    @FXML private CheckBox mipmaps;
    @FXML private CheckBox depthImages;
    @FXML private GridPane root;
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
            if(n.intValue() < 0) prop.setValue(0);
        });
    }


    @Override
    public boolean areColorImagesRequested() {
        return true;
    }

    @Override
    public boolean areMipmapsRequested() {
        return mipmaps.isSelected();
    }

    @Override
    public boolean isCompressionRequested() {
        return compressedImages.isSelected();
    }

    @Override
    public boolean areDepthImagesRequested() {
        return depthImages.isSelected();
    }

    @Override
    public int getDepthImageWidth() {
        return w.get();
    }

    @Override
    public int getDepthImageHeight() {
        return h.get();
    }
}
