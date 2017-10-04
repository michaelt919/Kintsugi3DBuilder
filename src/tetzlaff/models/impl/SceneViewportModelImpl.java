package tetzlaff.models.impl;

import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.SceneViewport;
import tetzlaff.models.SceneViewportModel;

public class SceneViewportModelImpl implements SceneViewportModel
{
    private static final SceneViewport SENTINEL = new SceneViewport()
    {
        @Override
        public Object getObjectAtCoordinates(double x, double y)
        {
            return null;
        }

        @Override
        public Vector3 get3DPositionAtCoordinates(double x, double y)
        {
            return Vector3.ZERO;
        }

        @Override
        public Vector3 getViewingDirection(double x, double y)
        {
            return Vector3.ZERO;
        }

        @Override
        public Vector3 getViewportCenter()
        {
            return Vector3.ZERO;
        }

        @Override
        public Vector2 projectPoint(Vector3 point)
        {
            return Vector2.ZERO;
        }

        @Override
        public float getLightWidgetScale()
        {
            return 1.0f;
        }
    };

    private SceneViewport sceneViewport = SENTINEL;

    @Override
    public SceneViewport getSceneViewport()
    {
        return sceneViewport;
    }

    @Override
    public void setSceneViewport(SceneViewport sceneViewport)
    {
        this.sceneViewport = sceneViewport == null ? SENTINEL : sceneViewport;
    }
}
