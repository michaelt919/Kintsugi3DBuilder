package tetzlaff.ibr.app.old;

import tetzlaff.ibr.core.ReadonlyLoadOptionsModel;

public class IBRLoadOptionsModelImpl implements ReadonlyLoadOptionsModel
{
    private boolean colorImagesRequested;
    private boolean mipmapsRequested;
    private boolean compressionRequested;
    private boolean depthImagesRequested;
    private int depthImageWidth;
    private int depthImageHeight;

    @Override
    public boolean areColorImagesRequested()
    {
        return this.colorImagesRequested;
    }

    public IBRLoadOptionsModelImpl setColorImagesRequested(boolean colorImagesRequested)
    {
        this.colorImagesRequested = colorImagesRequested;
        return this;
    }

    @Override
    public boolean areMipmapsRequested()
    {
        return this.mipmapsRequested;
    }

    public IBRLoadOptionsModelImpl setMipmapsRequested(boolean mipmapsRequested)
    {
        this.mipmapsRequested = mipmapsRequested;
        return this;
    }

    @Override
    public boolean isCompressionRequested()
    {
        return this.compressionRequested;
    }

    public IBRLoadOptionsModelImpl setCompressionRequested(boolean compressionRequested)
    {
        this.compressionRequested = compressionRequested;
        return this;
    }

    @Override
    public boolean areDepthImagesRequested()
    {
        return this.depthImagesRequested;
    }

    public IBRLoadOptionsModelImpl setDepthImagesRequested(boolean depthImagesRequested)
    {
        this.depthImagesRequested = depthImagesRequested;
        return this;
    }

    @Override
    public int getDepthImageWidth()
    {
        return this.depthImageWidth;
    }

    public IBRLoadOptionsModelImpl setDepthImageWidth(int depthImageWidth)
    {
        this.depthImageWidth = depthImageWidth;
        return this;
    }

    @Override
    public int getDepthImageHeight()
    {
        return this.depthImageHeight;
    }

    public IBRLoadOptionsModelImpl setDepthImageHeight(int depthImageHeight)
    {
        this.depthImageHeight = depthImageHeight;
        return this;
    }
}
