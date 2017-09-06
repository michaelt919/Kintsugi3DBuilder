package tetzlaff.ibr.export.general;

import java.io.File;

import tetzlaff.ibr.core.IBRRequest;

public interface RenderRequestBuilder
{
    RenderRequestBuilder useTextureSpaceVertexShader();
    RenderRequestBuilder useCameraSpaceVertexShader();
    RenderRequestBuilder useCustomVertexShader(File vertexShader);

    IBRRequest create();
    RenderRequestBuilder setWidth(int width);
    RenderRequestBuilder setHeight(int height);
}
