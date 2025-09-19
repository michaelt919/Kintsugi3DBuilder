package kintsugi3d.builder.javafx.controllers.modals.createnewproject;

import javafx.stage.FileChooser;

import java.util.Collections;
import java.util.List;

public class RealityCaptureImportController extends ManualImportController
{
    @Override
    public List<FileChooser.ExtensionFilter> getCameraExtensionFilters()
    {
        return Collections.singletonList(new FileChooser.ExtensionFilter("Reality Capture CSV file", "*.csv"));
    }
}
