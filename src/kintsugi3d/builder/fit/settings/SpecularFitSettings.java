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

package kintsugi3d.builder.fit.settings;

import kintsugi3d.builder.core.texture.TextureResolution;
import kintsugi3d.builder.resources.project.ImageCacheSettings;

import java.io.File;

public class SpecularFitSettings implements ReadonlySpecularFitSettings
{
    private final TextureResolution textureResolution;
    private final NormalOptimizationSettings normalOptimizationSettings = new NormalOptimizationSettings();
    private final ReconstructionSettings reconstructionSettings = new ReconstructionSettings();
    private final ImageCacheSettings imageCacheSettings = new ImageCacheSettings();
    private final ExportSettings exportSettings = new ExportSettings();
    //TODO Merge project and global preferences and attach to request

    private double convergenceTolerance = 0.00001;
    private double preliminaryConvergenceTolerance = 0.01;

    private boolean smithMaskingShadowingEnabled = true;
    private boolean shouldIncludeConstantTerm;

    private File priorSolutionDirectory;

    /**
     * Constructs an object to hold the settings for specular texture fitting.
     * @param width The width, in pixels, of the textures to be generated.
     * @param height The height, in pixels, of the textures to be generated.
     */
    public SpecularFitSettings(int width, int height)
    {
        this.textureResolution = new TextureResolution(width, height);

        imageCacheSettings.setTextureWidth(textureResolution.width);
        imageCacheSettings.setTextureHeight(textureResolution.height);
        imageCacheSettings.setTextureSubdiv( // TODO expose this in the interface
            (int)Math.ceil(Math.max(textureResolution.width, textureResolution.height) / 256.0));
        imageCacheSettings.setSampledSize(256); // TODO expose this in the interface
    }

    @Override
    public TextureResolution getTextureResolution()
    {
        return textureResolution;
    }

    @Override
    public NormalOptimizationSettings getNormalOptimizationSettings()
    {
        return normalOptimizationSettings;
    }

    @Override
    public ImageCacheSettings getImageCacheSettings()
    {
        return imageCacheSettings;
    }

    @Override
    public ReconstructionSettings getReconstructionSettings()
    {
        return reconstructionSettings;
    }

    @Override
    public ExportSettings getExportSettings()
    {
        return exportSettings;
    }

    @Override
    public double getConvergenceTolerance()
    {
        return this.convergenceTolerance;
    }

    /**
     * Sets the convergence tolerance used to determine whether the Levenberg-Marquardt algorithm for optimizing
     * the normal map has converged.
     * @param convergenceTolerance
     */
    public void setConvergenceTolerance(double convergenceTolerance)
    {
        if (convergenceTolerance < 0)
        {
            throw new IllegalArgumentException("Convergence tolerance must not be less than zero.");
        }

        this.convergenceTolerance = convergenceTolerance;
    }

    @Override
    public double getPreliminaryConvergenceTolerance()
    {
        return this.preliminaryConvergenceTolerance;
    }

    /**
     * Sets the convergence tolerance used to determine whether the Levenberg-Marquardt algorithm for optimizing
     * the normal map has converged.
     * @param preliminaryConvergenceTolerance
     */
    public void setPreliminaryConvergenceTolerance(double preliminaryConvergenceTolerance)
    {
        if (preliminaryConvergenceTolerance < 0)
        {
            throw new IllegalArgumentException("Convergence tolerance must not be less than zero.");
        }

        this.preliminaryConvergenceTolerance = preliminaryConvergenceTolerance;
    }

    @Override
    public boolean isSmithMaskingShadowingEnabled()
    {
        return smithMaskingShadowingEnabled;
    }

    /**
     * Whether or not to use height-correlated Smith for masking / shadowing.  Default is true.
     * @param smithMaskingShadowingEnabled
     */
    public void setSmithMaskingShadowingEnabled(boolean smithMaskingShadowingEnabled)
    {
        this.smithMaskingShadowingEnabled = smithMaskingShadowingEnabled;
    }

    @Override
    public boolean shouldIncludeConstantTerm()
    {
        return shouldIncludeConstantTerm;
    }

    public void setShouldIncludeConstantTerm(boolean shouldIncludeConstantTerm)
    {
        this.shouldIncludeConstantTerm = shouldIncludeConstantTerm;
    }

    @Override
    public File getPriorSolutionDirectory()
    {
        return priorSolutionDirectory;
    }

    /**
     * Sets the directory from which to load a prior solution
     * @param priorSolutionDirectory
     */
    public void setPriorSolutionDirectory(File priorSolutionDirectory)
    {
        this.priorSolutionDirectory = priorSolutionDirectory;
    }
}
