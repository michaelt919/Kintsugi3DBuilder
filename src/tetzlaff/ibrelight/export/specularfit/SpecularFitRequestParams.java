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

package tetzlaff.ibrelight.export.specularfit;

import java.io.File;

import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.rendering.resources.ImageCacheSettings;
import tetzlaff.models.ReadonlySettingsModel;

public class SpecularFitRequestParams
{
    private final TextureFitSettings textureFitSettings;
    private final ReadonlySettingsModel ibrSettings;
    private final NormalOptimizationSettings normalOptimizationSettings = new NormalOptimizationSettings();
    private final SpecularBasisSettings specularBasisSettings = new SpecularBasisSettings();
    private final ReconstructionSettings reconstructionSettings = new ReconstructionSettings();
    private final ImageCacheSettings imageCacheSettings = new ImageCacheSettings();
    private final ExportSettings exportSettings = new ExportSettings();

    private double convergenceTolerance = 0.00001;
    private double preliminaryConvergenceTolerance = 0.01;

    private File priorSolutionDirectory = null;
    private File outputDirectory;


    /**
     * Constructs an object to hold the settings for specular texture fitting.
     * @param textureFitSettings General settings for texture fitting (resolution, output directory)
     */
    public SpecularFitRequestParams(TextureFitSettings textureFitSettings, ReadonlySettingsModel ibrSettings, File outputDirectory)
    {
        if (textureFitSettings == null)
        {
            throw new IllegalArgumentException("Texture fit settings cannot be null.");
        }
        else
        {
            this.textureFitSettings = textureFitSettings;
        }

        if (ibrSettings == null)
        {
            throw new IllegalArgumentException("IBR settings cannot be null.");
        }
        else
        {
            this.ibrSettings = ibrSettings;
        }

        if (outputDirectory == null)
        {
            throw new IllegalArgumentException("Output directory cannot be null.");
        }
        else
        {
            this.outputDirectory = outputDirectory;
        }
    }

    public TextureFitSettings getTextureFitSettings()
    {
        return textureFitSettings;
    }

    public ReadonlySettingsModel getIbrSettings()
    {
        return ibrSettings;
    }

    public SpecularBasisSettings getSpecularBasisSettings()
    {
        return specularBasisSettings;
    }

    public NormalOptimizationSettings getNormalOptimizationSettings()
    {
        return normalOptimizationSettings;
    }

    /**
     * Gets a modifiable reference to the image cache settings for the specular fit
     * @return
     */
    public ImageCacheSettings getImageCacheSettings()
    {
        return imageCacheSettings;
    }

    public ReconstructionSettings getReconstructionSettings()
    {
        return reconstructionSettings;
    }

    public ExportSettings getExportSettings()
    {
        return exportSettings;
    }

    /**
     * Gets the convergence tolerance used to determine whether the Levenberg-Marquardt algorithm for optimizing
     * the normal map has converged.
     * @return
     */
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

    /**
     * Gets the convergence tolerance used to determine whether the Levenberg-Marquardt algorithm for optimizing
     * the normal map has converged.
     * @return
     */
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

    /**
     * Gets the directory from which to load a prior solution
     * @return
     */
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

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory)
    {
        this.outputDirectory = outputDirectory;
    }
}