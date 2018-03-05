package tetzlaff.ibrelight.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.Program;
import tetzlaff.gl.interactive.InteractiveRenderable;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.*;
import tetzlaff.util.AbstractImage;

public interface IBRRenderable<ContextType extends Context<ContextType>> extends InteractiveRenderable<ContextType>
{
    void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewOverride, Matrix4 projectionOverride, int subdivWidth, int subdivHeight);
    void setLoadingMonitor(LoadingMonitor loadingMonitor);

    ViewSet getActiveViewSet();
    VertexGeometry getActiveGeometry();

    SceneViewport getSceneViewportModel();

    SafeReadonlySettingsModel getSettingsModel();
    void setSettingsModel(ReadonlySettingsModel settingsModel);

    void setProgram(Program<ContextType> program);
    void reloadHelperShaders();

    void loadBackplate(File backplateFile) throws FileNotFoundException;
    Optional<AbstractImage> loadEnvironmentMap(File environmentFile) throws FileNotFoundException;

    ReadonlyObjectModel getObjectModel();
    ReadonlyCameraModel getCameraModel();
    ReadonlyLightingModel getLightingModel();

    void setObjectModel(ReadonlyObjectModel objectModel);
    void setCameraModel(ReadonlyCameraModel cameraModel);
    void setLightingModel(ReadonlyLightingModel lightingModel);

    void setMultiTransformationModel(List<Matrix4> multiTransformationModel);
    void setReferenceScene(VertexGeometry scene);

    IBRResources<ContextType> getResources();

    Matrix4 getAbsoluteViewMatrix(Matrix4 relativeViewMatrix);

    void draw(Framebuffer<ContextType> framebuffer, Matrix4 viewOverride, Matrix4 projectionOverride);

    @Override
    default void draw(Framebuffer<ContextType> framebuffer)
    {
        draw(framebuffer, null, null);
    }

    void setTonemapping(double[] linearLuminanceValues, byte[] encodedLuminanceValues);
    void applyLightCalibration();
}
