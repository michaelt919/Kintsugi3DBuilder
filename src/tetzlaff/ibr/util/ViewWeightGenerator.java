package tetzlaff.ibr.util;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.ibr.rendering.IBRResources;

@FunctionalInterface
public interface ViewWeightGenerator
{
    float[] generateWeights(IBRResources<?> resources, Iterable<Integer> activeViewIndexList, Matrix4 targetView);
}
