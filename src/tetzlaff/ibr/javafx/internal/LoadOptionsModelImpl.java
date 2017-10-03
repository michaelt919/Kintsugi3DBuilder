package tetzlaff.ibr.javafx.internal;//Created by alexk on 8/1/2017.

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import tetzlaff.ibr.core.ReadonlyLoadOptionsModel;

public class LoadOptionsModelImpl implements ReadonlyLoadOptionsModel
{
    public final BooleanProperty colorImages = new SimpleBooleanProperty(true);
    public final BooleanProperty mipmaps = new SimpleBooleanProperty(true);
    public final BooleanProperty compression = new SimpleBooleanProperty(true);
    public final BooleanProperty depthImages = new SimpleBooleanProperty(true);
    public final IntegerProperty depthWidth = new SimpleIntegerProperty(1024);
    public final IntegerProperty depthHeight = new SimpleIntegerProperty(1024);

    @Override
    public boolean areColorImagesRequested()
    {
        return colorImages.get();
    }

    @Override
    public boolean areMipmapsRequested()
    {
        return mipmaps.get();
    }

    @Override
    public boolean isCompressionRequested()
    {
        return compression.get();
    }

    @Override
    public boolean areDepthImagesRequested()
    {
        return depthImages.get();
    }

    @Override
    public int getDepthImageWidth()
    {
        return depthWidth.get();
    }

    @Override
    public int getDepthImageHeight()
    {
        return depthHeight.get();
    }
}
