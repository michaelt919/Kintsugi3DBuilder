package tetzlaff.ibr.export.general;

import java.io.File;

public interface RequestFactory
{
    RenderRequestBuilder buildSingleFrameRenderRequest(File fragmentShader, File outputDirectory, String outputImageName);
    RenderRequestBuilder buildMultiframeRenderRequest(File fragmentShader, File outputDirectory, int frameCount);
    RenderRequestBuilder buildMultiviewRenderRequest(File fragmentShader, File outputDirectory);
    RenderRequestBuilder buildMultiviewRetargetRenderRequest(File fragmentShader, File outputDirectory, File retargetViewSet);
}
