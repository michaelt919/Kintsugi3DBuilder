/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.util;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibrelight.rendering.resources.IBRResources;

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
