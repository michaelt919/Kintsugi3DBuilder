package kintsugi3d.builder.javafx.experience;

import javafx.stage.Window;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.OrientationViewSelectController;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.CurrentProjectInputSource;
import kintsugi3d.builder.javafx.controllers.modals.createnewproject.inputsources.InputSource;
import kintsugi3d.builder.javafx.controllers.paged.SimpleDataReceiverPage;

import java.io.IOException;

public class ObjectOrientationReferenceView extends ExperienceBase
{
    public void initialize(Window parentWindow)
    {
        // State isn't used so we can initialize with just the parentWindow.
        initialize(parentWindow, null);
    }

    @Override
    public String getName()
    {
        return "Select Orientation Reference View";
    }

    @Override
    public void open() throws IOException
    {
        openPagedModel("/fxml/modals/createnewproject/PrimaryViewSelect.fxml",
            (fxmlFile, fxmlLoader) ->
            {
                var page = new SimpleDataReceiverPage<InputSource, OrientationViewSelectController>(fxmlFile, fxmlLoader);
                page.receiveData(new CurrentProjectInputSource());
                return page;
            });
    }
}
