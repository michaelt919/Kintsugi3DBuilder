package tetzlaff.ibrelight;

public class SimpleLoadOptionsModel implements ReadonlyLoadOptionsModel
{
    private boolean colorImagesRequested;
    private boolean mipmapsRequested;
    private boolean compressionRequested;
    private boolean alphaRequested;
    private boolean depthImagesRequested;
    private int depthImageWidth;
    private int depthImageHeight;

    @Override
    public boolean areColorImagesRequested()
    {
        return this.colorImagesRequested;
    }

    public SimpleLoadOptionsModel setColorImagesRequested(boolean colorImagesRequested)
    {
        this.colorImagesRequested = colorImagesRequested;
        return this;
    }

    @Override
    public boolean areMipmapsRequested()
    {
        return this.mipmapsRequested;
    }

    public SimpleLoadOptionsModel setMipmapsRequested(boolean mipmapsRequested)
    {
        this.mipmapsRequested = mipmapsRequested;
        return this;
    }

    @Override
    public boolean isCompressionRequested()
    {
        return this.compressionRequested;
    }

    @Override
    public boolean isAlphaRequested()
    {
        return this.alphaRequested;
    }

    public SimpleLoadOptionsModel setAlphaRequested(boolean alphaRequested)
    {
        this.alphaRequested = alphaRequested;
        return this;
    }

    public SimpleLoadOptionsModel setCompressionRequested(boolean compressionRequested)
    {
        this.compressionRequested = compressionRequested;
        return this;
    }

    @Override
    public boolean areDepthImagesRequested()
    {
        return this.depthImagesRequested;
    }

    public SimpleLoadOptionsModel setDepthImagesRequested(boolean depthImagesRequested)
    {
        this.depthImagesRequested = depthImagesRequested;
        return this;
    }

    @Override
    public int getDepthImageWidth()
    {
        return this.depthImageWidth;
    }

    public SimpleLoadOptionsModel setDepthImageWidth(int depthImageWidth)
    {
        this.depthImageWidth = depthImageWidth;
        return this;
    }

    @Override
    public int getDepthImageHeight()
    {
        return this.depthImageHeight;
    }

    public SimpleLoadOptionsModel setDepthImageHeight(int depthImageHeight)
    {
        this.depthImageHeight = depthImageHeight;
        return this;
    }
}
