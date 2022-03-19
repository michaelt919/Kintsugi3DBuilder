package tetzlaff.ibrelight.core;

import tetzlaff.gl.core.FramebufferSize;
import tetzlaff.gl.vecmath.Matrix3;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.*;
import tetzlaff.models.impl.DefaultSettingsModel;
import tetzlaff.models.impl.SafeSettingsModelWrapperFactory;

public class SceneModel
{
    private ReadonlyObjectModel objectModel;
    private ReadonlyCameraModel cameraModel;
    private ReadonlyLightingModel lightingModel;
    private SafeReadonlySettingsModel settingsModel = new DefaultSettingsModel();

    private Vector3 centroid = Vector3.ZERO;
    private Matrix3 orientation = Matrix3.IDENTITY;
    private float scale = 1.0f;

    public ReadonlyObjectModel getObjectModel()
    {
        return objectModel;
    }

    public ReadonlyCameraModel getCameraModel()
    {
        return cameraModel;
    }

    public ReadonlyLightingModel getLightingModel()
    {
        return lightingModel;
    }

    public SafeReadonlySettingsModel getSettingsModel()
    {
        return this.settingsModel;
    }

    public Vector3 getCentroid()
    {
        return centroid;
    }

    public Matrix3 getOrientation()
    {
        return orientation;
    }

    public float getScale()
    {
        return this.scale;
    }

    public void setObjectModel(ReadonlyObjectModel objectModel)
    {
        this.objectModel = objectModel;
    }

    public void setCameraModel(ReadonlyCameraModel cameraModel)
    {
        this.cameraModel = cameraModel;
    }

    public void setLightingModel(ReadonlyLightingModel lightingModel)
    {
        this.lightingModel = lightingModel;
    }

    public void setSettingsModel(ReadonlySettingsModel settingsModel)
    {
        this.settingsModel = SafeSettingsModelWrapperFactory.getInstance().wrapUnsafeModel(settingsModel);
    }

    public void setCentroid(Vector3 centroid)
    {
        this.centroid = centroid;
    }

    public void setOrientation(Matrix3 orientation)
    {
        this.orientation = orientation;
    }

    public void setScale(float scale)
    {
        this.scale = scale;
    }

    public Matrix4 getLightViewMatrix(int lightIndex)
    {
        return getUnscaledMatrix(lightingModel.getLightMatrix(lightIndex));
    }

    public Matrix4 getLightModelViewMatrix(int lightIndex)
    {
        return getUnscaledMatrix(
            lightingModel.getLightMatrix(lightIndex)
                .times(objectModel.getTransformationMatrix()))
            .times(getBaseModelMatrix());
    }

    public Matrix4 getEnvironmentMapMatrix()
    {
        return getUnscaledMatrix(
                lightingModel.getEnvironmentMapMatrix()
                        .times(objectModel.getTransformationMatrix()))
                .times(getBaseModelMatrix());
    }
    public Matrix4 getCurrentViewMatrix()
    {
        return getUnscaledMatrix(cameraModel.getLookMatrix());
    }

    public Matrix4 getViewMatrixFromCameraPose(Matrix4 cameraPoseMatrix)
    {
        return cameraPoseMatrix
                .times(Matrix4.translate(this.centroid))
                .times(orientation.asMatrix4().quickInverse(0.01f));
    }

    public Matrix4 getCameraPoseFromViewMatrix(Matrix4 cameraPoseMatrix)
    {
        return cameraPoseMatrix
                .times(getBaseModelMatrix());
    }

    public Matrix4 getUnscaledMatrix(Matrix4 scaledMatrix)
    {
        return Matrix4.scale(scale)
                .times(scaledMatrix)
                .times(Matrix4.scale(1.0f / scale));
    }

    public Matrix4 getBaseModelMatrix()
    {
        return orientation.asMatrix4()
                .times(Matrix4.translate(this.centroid.negated()));
    }

    public Matrix4 getModelViewMatrix(Matrix4 view)
    {
        return view
                .times(getUnscaledMatrix(this.objectModel.getTransformationMatrix()))
                .times(getBaseModelMatrix());
    }

    public Matrix4 getViewFromModelViewMatrix(Matrix4 modelViewMatrix)
    {
        return modelViewMatrix
                .times(Matrix4.translate(this.centroid))
                .times(orientation.asMatrix4().quickInverse(0.01f))
                .times(getUnscaledMatrix(this.objectModel.getTransformationMatrix().quickInverse(0.01f)));
    }

    public float getVerticalFieldOfView(FramebufferSize size)
    {
        return 2 * (float)Math.atan(Math.tan(cameraModel.getHorizontalFOV() / 2) * size.height / size.width);
    }
}
