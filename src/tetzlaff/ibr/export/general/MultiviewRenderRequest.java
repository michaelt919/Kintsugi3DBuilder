package tetzlaff.ibr.export.general;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.Drawable;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.Program;
import tetzlaff.ibr.core.IBRRenderable;
import tetzlaff.ibr.core.IBRRequest;
import tetzlaff.ibr.core.LoadingMonitor;
import tetzlaff.ibr.core.SettingsModel;
import tetzlaff.ibr.rendering.IBRResources;

class MultiviewRenderRequest extends RenderRequestBase
{
    MultiviewRenderRequest(int width, int height, SettingsModel settingsModel,
        File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, settingsModel, vertexShader, fragmentShader, outputDirectory);
    }

    static class Builder extends BuilderBase
    {
        Builder(SettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            super(settingsModel, fragmentShader, outputDirectory);
        }

        @Override
        public IBRRequest create()
        {
            return new MultiviewRenderRequest(getWidth(), getHeight(), getSettingsModel(),
                getVertexShader(), getFragmentShader(), getOutputDirectory());
        }
    }

    @Override
    public <ContextType extends Context<ContextType>>
        void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
            throws IOException
    {
        IBRResources<ContextType> resources = renderable.getResources();
        Program<ContextType> program = createProgram(resources);
        Drawable<ContextType> drawable = createDrawable(program, resources);
        Framebuffer<ContextType> framebuffer = createFramebuffer(resources.context);

        for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
        {
            program.setUniform("viewIndex", i);
            program.setUniform("model_view", renderable.getActiveViewSet().getCameraPose(i));
            program.setUniform("projection",
                renderable.getActiveViewSet().getCameraProjection(
                        renderable.getActiveViewSet().getCameraProjectionIndex(i))
                    .getProjectionMatrix(renderable.getActiveViewSet().getRecommendedNearPlane(),
                        renderable.getActiveViewSet().getRecommendedFarPlane()));

            render(drawable, framebuffer);

            File exportFile = new File(getOutputDirectory(), String.format("%04d", i));
            getOutputDirectory().mkdirs();
            framebuffer.saveColorBufferToFile(0, "PNG", exportFile);

            if (callback != null)
            {
                callback.setProgress((double) i / (double) resources.viewSet.getCameraPoseCount());
            }
        }
    }
}
