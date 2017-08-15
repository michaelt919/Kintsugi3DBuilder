package tetzlaff.ibr;//Created by alexk on 7/31/2017.

public interface ReadonlyLoadOptionsModel 
{
    public boolean areColorImagesRequested();
    public boolean areMipmapsRequested();
    public boolean isCompressionRequested();
    public boolean areDepthImagesRequested();
    public int getDepthImageWidth();
    public int getDepthImageHeight();
}
