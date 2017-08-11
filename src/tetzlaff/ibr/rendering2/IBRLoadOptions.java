package tetzlaff.ibr.rendering2;//Created by alexk on 7/31/2017.

public interface IBRLoadOptions {
    public boolean areColorImagesRequested();
    public boolean areMipmapsRequested();
    public boolean isCompressionRequested();
    public boolean areDepthImagesRequested();
    public int getDepthImageWidth();
    public int getDepthImageHeight();
}
