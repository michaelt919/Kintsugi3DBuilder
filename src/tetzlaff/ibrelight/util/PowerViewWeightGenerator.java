package tetzlaff.ibrelight.util;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.rendering.IBRResources;

public class PowerViewWeightGenerator implements ViewWeightGenerator
{
    private final float power;

    public PowerViewWeightGenerator(float power)
    {
        this.power = power;
    }

    @Override
    public float[] generateWeights(IBRResources<?> resources, Iterable<Integer> activeViewIndexList, Matrix4 targetView)
    {
        float[] viewWeights = new float[resources.viewSet.getCameraPoseCount()];
        float viewWeightSum = 0.0f;

        for (int viewIndex : activeViewIndexList)
        {
            Vector3 viewDir = resources.viewSet.getCameraPose(viewIndex).times(
                    resources.geometry.getCentroid().asPosition())
                .getXYZ().negated().normalized();

            Vector3 targetDir = resources.viewSet.getCameraPose(viewIndex).times(
                    targetView.quickInverse(0.01f).getColumn(3).minus(resources.geometry.getCentroid().asPosition()))
                .getXYZ().normalized();

            viewWeights[viewIndex] = 1.0f / (float) Math.max(0.000001, 1.0 - Math.pow(Math.max(0.0, targetDir.dot(viewDir)), power)) - 1.0f;
            viewWeightSum += viewWeights[viewIndex];
        }

        for (int i = 0; i < viewWeights.length; i++)
        {
            viewWeights[i] /= Math.max(0.01, viewWeightSum);
        }

        return viewWeights;
    }
}
