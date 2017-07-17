package tetzlaff.ibr.gui2.controllers.scene.lights;//Created by alexk on 7/16/2017.

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class SettingsLightSceneController implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("SLSC started.");
        theChoiceBox.setItems(
                new ObservableListWrapper<LightType>(
                        Arrays.asList(
                                LightType.values()
                        )
                )
        );
        theChoiceBox.setConverter(LightType.converter);

        Property<LightType> g = theChoiceBox.valueProperty();

        g.addListener(new ChangeListener<LightType>() {
            @Override
            public void changed(ObservableValue<? extends LightType> observable, LightType oldValue, LightType newValue) {
                System.out.println("Change from " + LightType.converter.toString(oldValue)
                + " to " + LightType.converter.toString(newValue));
            }
        });




    }

    public ChoiceBox<LightType> theChoiceBox;


}
