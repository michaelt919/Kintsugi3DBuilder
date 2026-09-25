/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao, Joe Luther, Jakob Schmucki, Nathan Sunday
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.fit.decomposition;

import kintsugi3d.gl.vecmath.DoubleVector3;

import java.util.Arrays;

final class MutableBasisMaterialInfo implements IndexAssignableBasisMaterialInfo
{
    private final DoubleVector3 diffuseColor;

    /**
     * Should be immutable once initialized
     */
    private final double[] redBasis;

    /**
     * Should be immutable once initialized
     */
    private final double[] greenBasis;

    /**
     * Should be immutable once initialized
     */
    private final double[] blueBasis;

    /**
     * Must be final in order to be reliably used as a lookup key for efficient retrieval and removal
     */
    private final String name;

    private final String friendlyName;

    private boolean enabled;

    private int gpuIndex;

    private MutableBasisMaterialInfo(
        String name, int gpuIndex, String friendlyName, DoubleVector3 diffuseColor,
        double[] redBasis, double[] greenBasis, double[] blueBasis, boolean enabled)
    {
        this.name = name;
        this.gpuIndex = gpuIndex;
        this.friendlyName = friendlyName;
        this.diffuseColor = diffuseColor;
        this.redBasis = redBasis;
        this.greenBasis = greenBasis;
        this.blueBasis = blueBasis;
        this.enabled = enabled;
    }

    /**
     *
     * @param name
     * @param diffuseColor
     * @param redBasis An effectively immutable copy will be created
     * @param greenBasis An effectively immutable copy will be created
     * @param blueBasis An effectively immutable copy will be created
     * @param enabled
     * @return
     */
    public static MutableBasisMaterialInfo create(
        String name, int gpuIndex, String friendlyName, DoubleVector3 diffuseColor,
        double[] redBasis, double[] greenBasis, double[] blueBasis, boolean enabled)
    {
        return new MutableBasisMaterialInfo(
            name, gpuIndex, friendlyName, diffuseColor,
            Arrays.copyOf(redBasis, redBasis.length),
            Arrays.copyOf(greenBasis, greenBasis.length),
            Arrays.copyOf(blueBasis, blueBasis.length),
            enabled);
    }

    /**
     * Enabled by default.
     * @param name
     * @param diffuseColor
     * @param redBasis An effectively immutable copy will be created
     * @param greenBasis An effectively immutable copy will be created
     * @param blueBasis An effectively immutable copy will be created
     * @return
     */
    public static MutableBasisMaterialInfo create(
        String name, int gpuIndex, String friendlyName, DoubleVector3 diffuseColor,
        double[] redBasis, double[] greenBasis, double[] blueBasis)
    {
        return create(name, gpuIndex, friendlyName, diffuseColor, redBasis, greenBasis, blueBasis, true);
    }

    @Override
    public String getFriendlyName()
    {
        return this.friendlyName;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public DoubleVector3 getDiffuseColor()
    {
        return diffuseColor;
    }

    @Override
    public int getResolution()
    {
        return this.redBasis.length - 1;
    }

    @Override
    public double evaluateSpecularRed(int m)
    {
        return this.redBasis[m];
    }

    @Override
    public double evaluateSpecularGreen(int m)
    {
        return this.greenBasis[m];
    }

    @Override
    public double evaluateSpecularBlue(int m)
    {
        return this.blueBasis[m];
    }

    @Override
    public int getGPUIndex()
    {
        return gpuIndex;
    }

    @Override
    public void setGPUIndex(int gpuIndex)
    {
        this.gpuIndex = gpuIndex;
    }

    public MutableBasisMaterialInfo copy()
    {
        return copy(this.name);
    }

    public MutableBasisMaterialInfo copy(String newName)
    {
        // Don't need to copy arrays since they should be effectively immutable
        return new MutableBasisMaterialInfo(newName, gpuIndex, friendlyName, diffuseColor, redBasis, greenBasis, blueBasis, enabled);
    }
}
