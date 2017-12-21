package tetzlaff.ibrelight.export.general;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.Framebuffer;
import tetzlaff.gl.core.Program;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

class MultiviewRenderRequest extends RenderRequestBase
{
    MultiviewRenderRequest(int width, int height, ReadonlySettingsModel settingsModel,
        File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, settingsModel, vertexShader, fragmentShader, outputDirectory);
    }

    static class Builder extends BuilderBase
    {
        Builder(ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
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

            String fileName = renderable.getActiveViewSet().getImageFileName(i);

            if (!fileName.endsWith(".png"))
            {
                String[] parts = fileName.split("\\.");
                parts[parts.length - 1] = "png";
                fileName = String.join(".", parts);
            }

            File exportFile = new File(getOutputDirectory(), fileName);
            getOutputDirectory().mkdirs();
            framebuffer.saveColorBufferToFile(0, "PNG", exportFile);

            if (callback != null)
            {
                callback.setProgress((double) i / (double) resources.viewSet.getCameraPoseCount());
            }
        }
    }
}
