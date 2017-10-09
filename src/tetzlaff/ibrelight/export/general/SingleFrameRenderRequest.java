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
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

class SingleFrameRenderRequest extends RenderRequestBase
{
    private final String outputImageName;

    SingleFrameRenderRequest(int width, int height, String outputImageName, ReadonlySettingsModel settingsModel,
        File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, settingsModel, vertexShader, fragmentShader, outputDirectory);
        this.outputImageName = outputImageName;
    }

    static class Builder extends BuilderBase
    {
        private final String outputImageName;

        Builder(String outputImageName, ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            super(settingsModel, fragmentShader, outputDirectory);
            this.outputImageName = outputImageName;
        }

        @Override
        public IBRRequest create()
        {
            return new SingleFrameRenderRequest(getWidth(), getHeight(), outputImageName, getSettingsModel(),
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

        program.setUniform("model_view", renderable.getActiveViewSet().getCameraPose(0));
        program.setUniform("projection",
            renderable.getActiveViewSet().getCameraProjection(
                    renderable.getActiveViewSet().getCameraProjectionIndex(0))
                .getProjectionMatrix(renderable.getActiveViewSet().getRecommendedNearPlane(),
                    renderable.getActiveViewSet().getRecommendedFarPlane()));

        render(drawable, framebuffer);

        File exportFile = new File(getOutputDirectory(), outputImageName);
        getOutputDirectory().mkdirs();
        framebuffer.saveColorBufferToFile(0, "PNG", exportFile);
    }
}
