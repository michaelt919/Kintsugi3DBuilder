package tetzlaff.ibrelight.rendering;

import org.lwjgl.BufferUtils;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.core.FramebufferObject;
import tetzlaff.gl.core.FramebufferSize;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibrelight.core.SceneModel;
import tetzlaff.models.SceneViewport;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SceneViewportModel<ContextType extends Context<ContextType>> implements SceneViewport
{
    private final List<String> sceneObjectNameList;
    private final Map<String, Integer> sceneObjectIDLookup;
    private IntBuffer pixelObjectIDBuffer;
    private ShortBuffer pixelDepthBuffer;
    private FramebufferSize fboSize;

    private final SceneModel sceneModel;

    public SceneViewportModel(SceneModel sceneModel)
    {
        this.sceneModel = sceneModel;

        this.sceneObjectNameList = new ArrayList<>(32);
        this.sceneObjectIDLookup = new HashMap<>(32);

        this.sceneObjectNameList.add(null); // 0
    }

    void addSceneObjectType(String sceneObjectTag)
    {
        sceneObjectNameList.add(sceneObjectTag);

        // Reverse lookup table
        sceneObjectIDLookup.put(sceneObjectTag, sceneObjectNameList.size() - 1);
    }

    int lookupSceneObjectID(String sceneObjectTag)
    {
        return this.sceneObjectIDLookup.get(sceneObjectTag);
    }

    void refreshBuffers(FramebufferObject<ContextType> offscreenFBO)
    {
        fboSize = offscreenFBO.getSize();

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

        offscreenFBO.readIntegerColorBufferRGBA(1, pixelObjectIDBuffer);
        offscreenFBO.readDepthBuffer(pixelDepthBuffer);
    }

    @Override
    public Object getObjectAtCoordinates(double x, double y)
    {
        if (pixelObjectIDBuffer != null)
        {
            double xRemapped = Math.min(Math.max(x, 0), 1);
            double yRemapped = 1.0 - Math.min(Math.max(y, 0), 1);

            int index = 4 * (int)(Math.round((fboSize.height-1) * yRemapped) * fboSize.width + Math.round((fboSize.width-1) * xRemapped));
            return sceneObjectNameList.get(pixelObjectIDBuffer.get(index));
        }
        else
        {
            return null;
        }
    }


    private Matrix4 getProjectionInverse()
    {
        Matrix4 projection = sceneModel.getProjectionMatrix(fboSize);
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

            Matrix4 projectionInverse = getProjectionInverse();

            // Transform from screen space into camera space
            Vector4 unscaledPosition = projectionInverse
                    .times(new Vector4((float)(2 * x - 1), (float)(1 - 2 * y), 2 * (float)(0x0000FFFF & pixelDepthBuffer.get(index)) / (float)0xFFFF - 1, 1.0f));

            // Transform from camera space into world space.
            return sceneModel.getCurrentViewMatrix().quickInverse(0.01f)
                    .times(unscaledPosition.getXYZ().dividedBy(unscaledPosition.w).asPosition())
                    .getXYZ().dividedBy(sceneModel.getScale());
        }
        else
        {
            return null;
        }
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
        Vector4 projectedPoint = sceneModel.getProjectionMatrix(fboSize)
                .times(sceneModel.getCurrentViewMatrix())
                .times(point.times(sceneModel.getScale()).asPosition());

        return new Vector2(0.5f + projectedPoint.x / (2 * projectedPoint.w), 0.5f - projectedPoint.y / (2 * projectedPoint.w));
    }

    /**
     * Scale is returned in terms of the local object space defined in the geometry file
     * (not the scaled world space used by the user-facing IBRelight widgets).
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
