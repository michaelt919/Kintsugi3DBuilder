/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

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

    private Vector3 clearColor = Vector3.ZERO;

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

    public Vector3 getClearColor()
    {
        return clearColor;
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

    public void setClearColor(Vector3 clearColor)
    {
        this.clearColor = clearColor;
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

    public Matrix4 getEnvironmentMapMatrix(Matrix4 modelMatrix)
    {
        return getUnscaledMatrix(lightingModel.getEnvironmentMapMatrix()).times(modelMatrix);
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

    /**
     * Recenters and reorients the model using base model matrix, then applies the user-specified model transformation.
     * @return
     */
    public Matrix4 getFullModelMatrix()
    {
        return getUnscaledMatrix(this.objectModel.getTransformationMatrix()).times(getBaseModelMatrix());
    }

    /**
     * Just recentering and reorienting the model
     * @return
     */
    public Matrix4 getBaseModelMatrix()
    {
        return orientation.asMatrix4()
                .times(Matrix4.translate(this.centroid.negated()));
    }

    public Matrix4 getModelViewMatrix(Matrix4 view)
    {
        return view.times(getFullModelMatrix());
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
