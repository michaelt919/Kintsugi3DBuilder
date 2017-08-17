package tetzlaff.ibr;

public class IBRLoadOptions implements ReadonlyLoadOptionsModel
{
    private boolean colorImagesRequested;
    private boolean mipmapsRequested;
    private boolean compressionRequested;
    private boolean depthImagesRequested;
    private int depthImageWidth;
    private int depthImageHeight;

    public IBRLoadOptions()
    {
    }

    @Override
    public boolean areColorImagesRequested()
    {
        return this.colorImagesRequested;
    }

    public IBRLoadOptions setColorImagesRequested(boolean colorImagesRequested)
    {
        this.colorImagesRequested = colorImagesRequested;
        return this;
    }

    @Override
    public boolean areMipmapsRequested()
    {
        return this.mipmapsRequested;
    }

    public IBRLoadOptions setMipmapsRequested(boolean mipmapsRequested)
    {
        this.mipmapsRequested = mipmapsRequested;
        return this;
    }

    @Override
    public boolean isCompressionRequested()
    {
        return this.compressionRequested;
    }

    public IBRLoadOptions setCompressionRequested(boolean compressionRequested)
    {
        this.compressionRequested = compressionRequested;
        return this;
    }

    @Override
    public boolean areDepthImagesRequested()
    {
        return this.depthImagesRequested;
    }

    public IBRLoadOptions setDepthImagesRequested(boolean depthImagesRequested)
    {
        this.depthImagesRequested = depthImagesRequested;
        return this;
    }

    @Override
    public int getDepthImageWidth()
    {
        return this.depthImageWidth;
    }

    public IBRLoadOptions setDepthImageWidth(int depthImageWidth)
    {
        this.depthImageWidth = depthImageWidth;
        return this;
    }

    @Override
    public int getDepthImageHeight()
    {
        return this.depthImageHeight;
    }

    public IBRLoadOptions setDepthImageHeight(int depthImageHeight)
    {
        this.depthImageHeight = depthImageHeight;
        return this;
    }
}
