package tetzlaff.ibrelight.export.general;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.Drawable;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.Program;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.ReadonlySettingsModel;
import tetzlaff.ibrelight.rendering.IBRResources;

class MultiframeRenderRequest extends RenderRequestBase
{
    private final int frameCount;

    MultiframeRenderRequest(int width, int height, int frameCount, ReadonlySettingsModel settingsModel,
        File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, settingsModel, vertexShader, fragmentShader, outputDirectory);
        this.frameCount = frameCount;
    }

    static class Builder extends BuilderBase
    {
        private final int frameCount;

        Builder(int frameCount, ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            super(settingsModel, fragmentShader, outputDirectory);
            this.frameCount = frameCount;
        }

        @Override
        public IBRRequest create()
        {
            return new MultiframeRenderRequest(getWidth(), getHeight(), frameCount, getSettingsModel(),
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

        for (int i = 0; i < frameCount; i++)
        {
            program.setUniform("frame", i);
            program.setUniform("model_view", renderable.getActiveViewSet().getCameraPose(0));
            program.setUniform("projection",
                renderable.getActiveViewSet().getCameraProjection(
                        renderable.getActiveViewSet().getCameraProjectionIndex(0))
                    .getProjectionMatrix(renderable.getActiveViewSet().getRecommendedNearPlane(),
                        renderable.getActiveViewSet().getRecommendedFarPlane()));

            render(drawable, framebuffer);

            File exportFile = new File(getOutputDirectory(), String.format("%04d.png", i));
            getOutputDirectory().mkdirs();
            framebuffer.saveColorBufferToFile(0, "PNG", exportFile);

            if (callback != null)
            {
                callback.setProgress((double) i / (double) frameCount);
            }
        }
    }
}
