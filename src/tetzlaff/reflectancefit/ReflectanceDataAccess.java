package tetzlaff.reflectancefit;

import java.awt.image.BufferedImage;
import java.util.Optional;

import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.imagedata.ViewSet;

@SuppressWarnings("ProhibitedExceptionDeclared")
public interface ReflectanceDataAccess
{
    String getDefaultMaterialName();
    VertexGeometry retrieveMesh() throws Exception;
    void initializeViewSet() throws Exception;
    ViewSet getViewSet();
    BufferedImage retrieveImage(int index) throws Exception;
    Optional<BufferedImage> retrieveMask(int index) throws Exception;
}
