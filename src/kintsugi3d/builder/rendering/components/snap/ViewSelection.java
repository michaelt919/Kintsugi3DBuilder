package kintsugi3d.builder.rendering.components.snap;

import kintsugi3d.builder.core.Projection;
import kintsugi3d.builder.core.ReadonlyViewSet;
import kintsugi3d.gl.vecmath.Matrix4;

public interface ViewSelection
{
    ReadonlyViewSet getViewSet();

    int getSnapViewIndex();

    Matrix4 getViewForIndex(int index);

    default Matrix4 getSnapView()
    {
        return getViewForIndex(getSnapViewIndex());
    }

    default Matrix4 getSnapCameraPose()
    {
        return getViewSet().getCameraPose(getSnapViewIndex());
    }

    default Matrix4 getSnapCameraPoseInverse()
    {
        return getViewSet().getCameraPoseInverse(getSnapViewIndex());
    }

    default Projection getSnapCameraProjection()
    {
        ReadonlyViewSet viewSet = getViewSet();
        return viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(getSnapViewIndex()));
    }
}
