/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.util;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;

import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.builder.resources.ibr.ReadonlyIBRResources;

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
    public float[] generateWeights(ReadonlyIBRResources<? extends kintsugi3d.gl.core.Context<?>> resources, Iterable<Integer> activeViewIndexList, Matrix4 targetView)
    {
        float[] viewWeights = new float[resources.getViewSet().getCameraPoseCount()];
        float viewWeightSum = 0.0f;

        Queue<WeightedView> viewPriority = new PriorityQueue<>(resources.getViewSet().getCameraPoseCount(), Comparator.reverseOrder());

        for (int i : activeViewIndexList)
        {
            Vector3 viewDir = resources.getViewSet().getCameraPose(i).times(
                    Objects.requireNonNull(resources.getGeometry()).getCentroid().asPosition())
                .getXYZ().negated().normalized();

            Vector3 targetDir = resources.getViewSet().getCameraPose(i).times(
                targetView.quickInverse(0.01f).getColumn(3).minus(
                    Objects.requireNonNull(resources.getGeometry()).getCentroid().asPosition()))
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
