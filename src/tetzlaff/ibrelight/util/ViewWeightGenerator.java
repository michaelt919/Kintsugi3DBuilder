package tetzlaff.ibrelight.util;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.ibrelight.rendering.IBRResources;

@FunctionalInterface
public interface ViewWeightGenerator
{
    float[] generateWeights(IBRResources<?> resources, Iterable<Integer> activeViewIndexList, Matrix4 targetView);
}
