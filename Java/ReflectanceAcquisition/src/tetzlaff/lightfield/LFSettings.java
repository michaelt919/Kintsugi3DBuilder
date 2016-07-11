/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.lightfield;

/**
 * A model of the rendering settings for light fields.
 * @author Michael Tetzlaff
 *
 */
public class LFSettings 
{
    /**
     * The exponent of the gamma curve when applying color correction on the final rendering and inverse color correction on the input images.
     */
    private float gamma = 2.2f;
    
    /**
     * The exponent to use in the view weighting formula.
     */
    private float weightExponent = 16.0f;
    
    /**
     * Whether or not visibility testing is enabled.
     */
    private boolean occlusionEnabled = true;
    
    /**
     * The depth bias to use when performing visibility testing.
     */
    private float occlusionBias = 0.0025f;

    /**
     * Creates a settings model with the default options.
     */
	public LFSettings() 
	{
	}

	/**
	 * Gets the exponent of the gamma curve when applying color correction on the final rendering and inverse color correction on the input images.
	 * @return The gamma curve exponent.
	 */
	public float getGamma() 
	{
		return this.gamma;
	}

	/**
	 * Gets the exponent of the gamma curve when applying color correction on the final rendering and inverse color correction on the input images.
	 * @param gamma The gamma curve exponent.
	 */
	public void setGamma(float gamma) 
	{
		this.gamma = gamma;
	}

	/**
	 * Gets the exponent to use in the view weighting formula.
	 * @return The view weighting exponent.
	 */
	public float getWeightExponent() 
	{
		return this.weightExponent;
	}

	/**
	 * Sets the exponent to use in the view weighting formula.
	 * @param weightExponent The view weighting exponent.
	 */
	public void setWeightExponent(float weightExponent) 
	{
		this.weightExponent = weightExponent;
	}

	/**
	 * Gets whether or not visibility testing is enabled.
	 * @return true if visibility testing is enabled, false otherwise.
	 */
	public boolean isOcclusionEnabled() 
	{
		return this.occlusionEnabled;
	}

	/**
	 * Sets whether or not visibility testing is enabled.
	 * @param occlusionEnabled true if visibility testing is enabled, false otherwise.
	 */
	public void setOcclusionEnabled(boolean occlusionEnabled) 
	{
		this.occlusionEnabled = occlusionEnabled;
	}

	/**
	 * Gets the depth bias to use when performing visibility testing.
	 * @return The depth bias value.
	 */
	public float getOcclusionBias() 
	{
		return this.occlusionBias;
	}

	/**
	 * Sets the depth bias to use when performing visibility testing.
	 * @param occlusionBias The depth bias value.
	 */
	public void setOcclusionBias(float occlusionBias) 
	{
		this.occlusionBias = occlusionBias;
	}
}
