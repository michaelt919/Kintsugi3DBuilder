package kintsugi3d.builder.javafx.experience;

import javafx.stage.Window;
import kintsugi3d.builder.javafx.controllers.modals.viewselect.CurrentProjectViewSelectable;
import kintsugi3d.builder.javafx.controllers.modals.viewselect.OrientationViewSelectController;

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
    protected void open() throws IOException
    {
        buildPagedModal(new CurrentProjectViewSelectable())
            .<OrientationViewSelectController>then("/fxml/modals/createnewproject/PrimaryViewSelect.fxml")
            .finish();
    }
}
