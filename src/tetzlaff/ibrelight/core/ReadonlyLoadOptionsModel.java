package tetzlaff.ibrelight.core;//Created by alexk on 7/31/2017.

public interface ReadonlyLoadOptionsModel 
{
    boolean areColorImagesRequested();
    boolean areMipmapsRequested();
    boolean isCompressionRequested();
    boolean areDepthImagesRequested();
    int getDepthImageWidth();
    int getDepthImageHeight();
}
