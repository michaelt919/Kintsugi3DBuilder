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
package tetzlaff.gl;

/**
 * Enumerates the possible hardware compression schemes that can be requested from the GL.
 * The actual compression algorithm used can be freely chosen by the GL, 
 * but it should ensure that all the requested components are available within the specified bits-per-pixel (bpp) limits.
 * Note: BPP values are an asymptotic measure and may vary slightly, particularly with smaller textures. 
 * @author Michael Tetzlaff
 *
 */
public enum CompressionFormat 
{
	/**
	 * A single unsigned red channel should be compressed at 4 bits-per-pixel.
	 */
	RED_4BPP,
	
	/**
	 * A single signed red channel should be compressed at 4 bits-per-pixel.
	 */
	SIGNED_RED_4BPP,
	
	/**
	 * Two unsigned channels, red and green, should be compressed independently at 4 bits-per-pixel each, for a total of 8 bits-per-pixel.
	 */
	RED_4BPP_GREEN_4BPP,
	
	/**
	 * Two signed channels, red and green, should be compressed independently at 4 bits-per-pixel each, for a total of 8 bits-per-pixel.
	 */
	SIGNED_RED_4BPP_GREEN_4BPP,
	
	/**
	 * Three unsigned channels for red, green, and blue should be compressed together, in a linear color space, at 4 bits-per-pixel total.
	 */
	RGB_4BPP,
	
	/**
	 * Three unsigned channels for red, green, and blue should be compressed together, in the sRGB color space, at 4 bits-per-pixel total.
	 */
	SRGB_4BPP,
	
	/**
	 * Three unsigned channels for red, green, and blue should be compressed together in a linear color space, 
	 * along with a punch-through alpha bit, at 4 bits-per-pixel total.
	 */
	RGB_PUNCHTHROUGH_ALPHA1_4BPP,
	
	/**
	 * Three unsigned channels for red, green, and blue should be compressed together in the sRGB color space, 
	 * along with a punch-through alpha bit, at 4 bits-per-pixel total.
	 */
	SRGB_PUNCHTHROUGH_ALPHA1_4BPP,
	
	/**
	 * Three unsigned channels for red, green, and blue should be compressed together, in a linear color space, at 4 bits-per-pixel,
	 * along with with an independently compressed alpha channel at 4 bits-per-pixel, for a total of 8 bits-per-pixel.
	 */
	RGB_4BPP_ALPHA_4BPP,
	
	/**
	 * Three unsigned channels for red, green, and blue should be compressed together, in the sRGB color space, at 4 bits-per-pixel,
	 * along with with an independently compressed alpha channel at 4 bits-per-pixel, for a total of 8 bits-per-pixel.
	 */
	SRGB_4BPP_ALPHA_4BPP
}
