package tetzlaff.ibrelight.export.general;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.Drawable;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.gl.core.Program;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.ViewSet;
import tetzlaff.ibrelight.rendering.IBRResources;
import tetzlaff.models.ReadonlySettingsModel;

class MultiviewRetargetRenderRequest extends RenderRequestBase
{
    private final File targetViewSetFile;

    MultiviewRetargetRenderRequest(int width, int height, ReadonlySettingsModel settingsModel,
        File targetViewSet, File vertexShader, File fragmentShader, File outputDirectory)
    {
        super(width, height, settingsModel, vertexShader, fragmentShader, outputDirectory);
        this.targetViewSetFile = targetViewSet;
    }

    static class Builder extends BuilderBase
    {
        private final File targetViewSet;

        Builder(File targetViewSet, ReadonlySettingsModel settingsModel, File fragmentShader, File outputDirectory)
        {
            super(settingsModel, fragmentShader, outputDirectory);
            this.targetViewSet = targetViewSet;
        }

        @Override
        public IBRRequest create()
        {
            return new MultiviewRetargetRenderRequest(getWidth(), getHeight(), getSettingsModel(),
                targetViewSet, getVertexShader(), getFragmentShader(), getOutputDirectory());
        }
    }

    @Override
    public <ContextType extends Context<ContextType>>
        void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback)
            throws IOException
    {
        ViewSet targetViewSet = ViewSet.loadFromVSETFile(targetViewSetFile);

        IBRResources<ContextType> resources = renderable.getResources();

        try
        (
            Program<ContextType> program = createProgram(resources);
            FramebufferObject<ContextType> framebuffer = createFramebuffer(resources.context)
        )
        {
            Drawable<ContextType> drawable = createDrawable(program, resources);
            for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
            {
                program.setUniform("viewIndex", i);
                program.setUniform("model_view", targetViewSet.getCameraPose(i));
                program.setUniform("projection",
                    targetViewSet.getCameraProjection(targetViewSet.getCameraProjectionIndex(i))
                        .getProjectionMatrix(targetViewSet.getRecommendedNearPlane(), targetViewSet.getRecommendedFarPlane()));

                render(drawable, framebuffer);

                String fileName = targetViewSet.getImageFileName(i);

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
}
