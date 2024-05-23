package kintsugi3d.builder.rendering.components.snap;

import kintsugi3d.builder.core.Projection;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.gl.vecmath.Matrix4;

public interface ViewSelection
{
    ReadonlyViewSet getViewSet();

    int getSelectedViewIndex();

    /**
     * Gets the view matrix for a particular view index, relative to the world space used by rendered components.
     * This is not generally the same as the camera pose matrix in the view set as it is in reference to a
     * recentered, reoriented, and rescaled model.
     * @param index
     * @return
     */
    Matrix4 getViewForIndex(int index);

    default Matrix4 getSelectedView()
    {
        return getViewForIndex(getSelectedViewIndex());
    }

    default Matrix4 getSelectedCameraPose()
    {
        return getViewSet().getCameraPose(getSelectedViewIndex());
    }

    default Matrix4 getSelectedCameraPoseInverse()
    {
        return getViewSet().getCameraPoseInverse(getSelectedViewIndex());
    }

    default Projection getSelectedCameraProjection()
    {
        ReadonlyViewSet viewSet = getViewSet();
        return viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(getSelectedViewIndex()));
    }
}
