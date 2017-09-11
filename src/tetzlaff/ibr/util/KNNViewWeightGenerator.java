package tetzlaff.ibr.util;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.rendering.IBRResources;

public class KNNViewWeightGenerator implements ViewWeightGenerator
{
    private final int k;

    public KNNViewWeightGenerator(int k)
    {
        this.k = k;
    }

    private static final class WeightedView implements Comparable<WeightedView>
    {
        public final int viewIndex;
        public final float weight;

        private WeightedView(int viewIndex, float weight)
        {
            this.viewIndex = viewIndex;
            this.weight = weight;
        }

        @Override
        public int compareTo(WeightedView o)
        {
            return Float.compare(weight, o.weight);
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof WeightedView && this.compareTo((WeightedView) obj) == 0;
        }

        @Override
        public int hashCode()
        {
            int result = viewIndex;
            result = 31 * result + (weight == 0.0f ? 0 : Float.floatToIntBits(weight));
            return result;
        }
    }

    @Override
    public float[] generateWeights(IBRResources<?> resources, Iterable<Integer> activeViewIndexList, Matrix4 targetView)
    {
        float[] viewWeights = new float[resources.viewSet.getCameraPoseCount()];
        float viewWeightSum = 0.0f;

        Queue<WeightedView> viewPriority = new PriorityQueue<>(resources.viewSet.getCameraPoseCount(), Comparator.reverseOrder());

        for (int i : activeViewIndexList)
        {
            Vector3 viewDir = resources.viewSet.getCameraPose(i).times(
                    resources.geometry.getCentroid().asPosition())
                .getXYZ().negated().normalized();

            Vector3 targetDir = resources.viewSet.getCameraPose(i).times(
                    targetView.quickInverse(0.01f).getColumn(3).minus(resources.geometry.getCentroid().asPosition()))
                .getXYZ().normalized();

            viewPriority.add(new WeightedView(i, targetDir.dot(viewDir)));
        }

        for (int i = 0; i < k; i++)
        {
            WeightedView next = viewPriority.poll();
            viewWeights[next.viewIndex] = 1.0f / (float)Math.max(0.000001, 1.0 - next.weight);
        }

        WeightedView thresholdView = viewPriority.poll();
        float threshold = 1.0f / (float)Math.max(0.000001, 1.0 - thresholdView.weight);

        for (int i = 0; i < viewWeights.length; i++)
        {
            if (viewWeights[i] > 0)
            {
                viewWeights[i] -= threshold;
                viewWeightSum += viewWeights[i];
            }
        }

        for (int i = 0; i < viewWeights.length; i++)
        {
            viewWeights[i] /= Math.max(0.01, viewWeightSum);
        }

        return viewWeights;
    }
}
