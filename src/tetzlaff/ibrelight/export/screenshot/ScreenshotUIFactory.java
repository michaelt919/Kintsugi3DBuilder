package tetzlaff.ibrelight.export.screenshot;

import java.io.IOException;

import javafx.stage.Window;
import tetzlaff.ibrelight.core.IBRelightModels;
import tetzlaff.ibrelight.export.screenshot.ScreenshotRequest.BuilderImplementation;

public final class ScreenshotUIFactory
{
    private ScreenshotUIFactory()
    {
    }

    public static ScreenshotUI create(Window window, IBRelightModels modelAccess) throws IOException
    {
        ScreenshotUI ui = ScreenshotUI.create(window, modelAccess);
        ui.setBuilderSupplier(BuilderImplementation::new);
        return ui;
    }
}
