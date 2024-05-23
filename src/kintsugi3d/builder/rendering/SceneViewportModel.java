/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.rendering;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kintsugi3d.builder.core.SceneModel;
import kintsugi3d.builder.state.SceneViewport;
import kintsugi3d.gl.core.Context;
import kintsugi3d.gl.core.FramebufferObject;
import kintsugi3d.gl.core.FramebufferSize;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector2;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.gl.vecmath.Vector4;
import org.lwjgl.*;

public class SceneViewportModel implements SceneViewport
{
    private final List<String> sceneObjectNameList;
    private final Map<String, Integer> sceneObjectIDLookup;
    private IntBuffer pixelObjectIDBuffer;
    private ShortBuffer pixelDepthBuffer;
    private FramebufferSize fboSize;
    private Matrix4 projection;

    private final SceneModel sceneModel;

    public SceneViewportModel(SceneModel sceneModel)
    {
        this.sceneModel = sceneModel;

        this.sceneObjectNameList = new ArrayList<>(32);
        this.sceneObjectIDLookup = new HashMap<>(32);

        this.sceneObjectNameList.add(null); // 0
    }

    public void addSceneObjectType(String sceneObjectTag)
    {
        sceneObjectNameList.add(sceneObjectTag);

        // Reverse lookup table
        sceneObjectIDLookup.put(sceneObjectTag, sceneObjectNameList.size() - 1);
    }

    public int lookupSceneObjectID(String sceneObjectTag)
    {
        return this.sceneObjectIDLookup.get(sceneObjectTag);
    }

    public <ContextType extends Context<ContextType>> void refreshBuffers(Matrix4 projection, FramebufferObject<ContextType> offscreenFBO)
    {
        this.projection = projection;
        this.fboSize = offscreenFBO.getSize();

        if (pixelObjectIDBuffer == null || pixelObjectIDBuffer.capacity() != 4 * fboSize.width * fboSize.height)
        {
            pixelObjectIDBuffer = BufferUtils.createIntBuffer(4 * fboSize.width * fboSize.height);
        }
        else
        {
            pixelObjectIDBuffer.clear();
        }

        if (pixelDepthBuffer == null || pixelDepthBuffer.capacity() != fboSize.width * fboSize.height)
        {
            pixelDepthBuffer = BufferUtils.createShortBuffer(fboSize.width * fboSize.height);
        }
        else
        {
            pixelDepthBuffer.clear();
        }

        offscreenFBO.getTextureReaderForColorAttachment(1).readIntegerRGBA(pixelObjectIDBuffer);
        offscreenFBO.getTextureReaderForDepthAttachment().read(pixelDepthBuffer);
    }

    @Override
    public Object getObjectAtCoordinates(double x, double y)
    {
        if (pixelObjectIDBuffer != null)
        {
            double xRemapped = Math.min(Math.max(x, 0), 1);
            double yRemapped = 1.0 - Math.min(Math.max(y, 0), 1);

            int index = 4 * (int)(Math.round((fboSize.height-1) * yRemapped) * fboSize.width + Math.round((fboSize.width-1) * xRemapped));

            if (index >= 0 && index < pixelObjectIDBuffer.limit())
            {
                int objectID = pixelObjectIDBuffer.get(index);

                if (objectID >= 0 && objectID < sceneObjectNameList.size())
                {
                    return sceneObjectNameList.get(objectID);
                }
            }
        }

        // If any conditions were false
        return null;
    }

    private Matrix4 getProjectionInverse()
    {
        return  Matrix4.fromRows(
                new Vector4(1.0f / projection.get(0, 0), 0, 0, 0),
                new Vector4(0, 1.0f / projection.get(1, 1), 0, 0),
                new Vector4(0, 0, 0, -1),
                new Vector4(0, 0, 1.0f, projection.get(2, 2))
                        .dividedBy(projection.get(2, 3)));
    }

    @Override
    public Vector3 get3DPositionAtCoordinates(double x, double y)
    {
        if (pixelDepthBuffer != null)
        {
            double xRemapped = Math.min(Math.max(x, 0), 1);
            double yRemapped = 1.0 - Math.min(Math.max(y, 0), 1);

            int index = (int)(Math.round((fboSize.height-1) * yRemapped) * fboSize.width + Math.round((fboSize.width-1) * xRemapped));

            if (index >= 0 && index < pixelDepthBuffer.limit())
            {
                Matrix4 projectionInverse = getProjectionInverse();

                // Transform from screen space into camera space
                Vector4 unscaledPosition = projectionInverse
                        .times(new Vector4((float) (2 * x - 1), (float) (1 - 2 * y), 2 * (float) (0x0000FFFF & pixelDepthBuffer.get(index)) / (float) 0xFFFF - 1, 1.0f));

                // Transform from camera space into world space.
                return sceneModel.getCurrentViewMatrix().quickInverse(0.01f)
                        .times(unscaledPosition.getXYZ().dividedBy(unscaledPosition.w).asPosition())
                        .getXYZ().dividedBy(sceneModel.getScale());
            }
        }

        // If any conditions were false
        return null;
    }

    @Override
    public Vector3 getViewingDirection(double x, double y)
    {
        Matrix4 projectionInverse = getProjectionInverse();

        // Take the position the pixel would have at the far clipping plane.
        // Transform from screen space into world space.
        Vector4 unscaledPosition = projectionInverse
                .times(new Vector4((float)(2 * x - 1), (float)(1 - 2 * y), 1.0f, 1.0f));

        // Transform from camera space into world space.
        // Interpret the vector as the direction from the origin (0,0,0) for this pixel.
        return sceneModel.getCurrentViewMatrix().quickInverse(0.01f)
                .times(unscaledPosition.getXYZ().dividedBy(unscaledPosition.w).asDirection())
                .getXYZ().normalized();
    }

    @Override
    public Vector3 getViewportCenter()
    {
        return sceneModel.getCurrentViewMatrix().quickInverse(0.01f)
                .getColumn(3)
                .getXYZ().dividedBy(sceneModel.getScale());
    }

    @Override
    public Vector2 projectPoint(Vector3 point)
    {
        Vector4 projectedPoint =
            projection.times(sceneModel.getCurrentViewMatrix())
                .times(point.times(sceneModel.getScale()).asPosition());

        return new Vector2(0.5f + projectedPoint.x / (2 * projectedPoint.w), 0.5f - projectedPoint.y / (2 * projectedPoint.w));
    }

    /**
     * Scale is returned in terms of the local object space defined in the geometry file
     * (not the scaled world space used by the user-facing widgets).
     * @param viewMatrix
     * @param size
     * @return
     */
    public float computeRawLightWidgetScale(Matrix4 viewMatrix, FramebufferSize size)
    {
        float cameraDistance = viewMatrix
                .times(sceneModel.getCameraModel().getTarget().times(sceneModel.getScale()).asPosition())
                .getXYZ().length();
        return cameraDistance * Math.min(sceneModel.getCameraModel().getHorizontalFOV(), sceneModel.getVerticalFieldOfView(size)) / 4;
    }

    @Override
    public float getLightWidgetScale()
    {
        return computeRawLightWidgetScale(sceneModel.getCurrentViewMatrix(), fboSize) / sceneModel.getScale();
    }
}
