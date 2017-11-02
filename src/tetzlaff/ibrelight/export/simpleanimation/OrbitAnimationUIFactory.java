package tetzlaff.ibrelight.export.simpleanimation;

import java.io.IOException;

import javafx.stage.Window;
import tetzlaff.ibrelight.core.IBRelightModels;
import tetzlaff.ibrelight.export.simpleanimation.OrbitAnimationRequest.BuilderImplementation;

public final class OrbitAnimationUIFactory
{
    private OrbitAnimationUIFactory()
    {
    }

    public static SimpleAnimationUI create(Window window, IBRelightModels modelAccess) throws IOException
    {
        SimpleAnimationUI ui = SimpleAnimationUI.create(window, modelAccess);
        ui.setBuilderSupplier(BuilderImplementation::new);
        return ui;
    }
}
