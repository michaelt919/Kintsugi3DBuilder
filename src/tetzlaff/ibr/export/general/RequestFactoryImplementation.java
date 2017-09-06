package tetzlaff.ibr.export.general;

import java.io.File;

import tetzlaff.ibr.core.SettingsModel;

public final class RequestFactoryImplementation implements RequestFactory
{
    private final SettingsModel settingsModel;

    private RequestFactoryImplementation(SettingsModel settingsModel)
    {
        this.settingsModel = settingsModel;
    }

    public static RequestFactoryImplementation create(SettingsModel settingsModel)
    {
        return new RequestFactoryImplementation(settingsModel);
    }

    @Override
    public RenderRequestBuilder buildSingleFrameRenderRequest(File fragmentShader, File outputDirectory, String outputImageName)
    {
        //noinspection UnnecessarilyQualifiedInnerClassAccess
        return new SingleFrameRenderRequest.Builder(outputImageName, settingsModel, fragmentShader, outputDirectory);
    }

    @Override
    public RenderRequestBuilder buildMultiframeRenderRequest(File fragmentShader, File outputDirectory, int frameCount)
    {
        //noinspection UnnecessarilyQualifiedInnerClassAccess
        return new MultiframeRenderRequest.Builder(frameCount, settingsModel, fragmentShader, outputDirectory);
    }

    @Override
    public RenderRequestBuilder buildMultiviewRenderRequest(File fragmentShader, File outputDirectory)
    {
        //noinspection UnnecessarilyQualifiedInnerClassAccess
        return new MultiviewRenderRequest.Builder(settingsModel, fragmentShader, outputDirectory);
    }

    @Override
    public RenderRequestBuilder buildMultiviewRetargetRenderRequest(File fragmentShader, File outputDirectory, File retargetViewSet)
    {
        //noinspection UnnecessarilyQualifiedInnerClassAccess
        return new MultiviewRetargetRenderRequest.Builder(retargetViewSet, settingsModel, fragmentShader, outputDirectory);
    }
}
