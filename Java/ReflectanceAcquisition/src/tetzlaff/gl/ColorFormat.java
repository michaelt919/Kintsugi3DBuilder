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
 * Represents an internal color format to be used by a framebuffer or texture.
 * Implementations should attempt to match this format as much as possible; 
 * if the format specified is not available, the smallest format with at least as much precision in each color component should be used instead, if such a format exists.
 * If no format exists with as much precision as requested, the closest format with the maximum possible precision should be used.
 * @author Michael Tetzlaff
 *
 */
public class ColorFormat
{
	/**
	 * Enumerates the possible internal data types
	 * @author Michael Tetzlaff
	 *
	 */
	public static enum DataType
	{
		/**
		 * Each color component is stored as a non-negative fixed point number, and interpreted as a "normalized" value between 0 and 1.
		 */
		NORMALIZED_FIXED_POINT,
		
		/**
		 * Each color component is stored as a non-negative fixed point number, and interpreted as a "normalized" value between 0 and 1.
		 * The red, green, and blue color components are interpreted in the non-linear sRGB color space and converted to a linear color space when queried by a shader. 
		 */
		SRGB_FIXED_POINT,
		
		/**
		 * Each color component is stored as a possibly negative fixed point number, and interpreted as a "normalized" value between -1 and 1.
		 */
		SIGNED_FIXED_POINT,
		
		/**
		 * Each color component is stored as a floating-point number between 0 and 1.
		 */
		FLOATING_POINT,
		
		/**
		 * Each color component is stored as an unsigned integer, and must be queried as an integer by shaders.
		 */
		UNSIGNED_INTEGER,
		
		/**
		 * Each color component is stored as an signed integer, and must be queried as an integer by shaders.
		 */
		SIGNED_INTEGER
	}
	
	/**
	 * The number of bits to be allocated for the red component of the color.
	 */
	public final int redBits;
	
	/**
	 * The number of bits to be allocated for the green component of the color.
	 */
	public final int greenBits;
	
	/**
	 * The number of bits to be allocated for the blue component of the color.
	 */
	public final int blueBits;
	
	/**
	 * The number of bits to be allocated for the alpha component of the color.
	 */
	public final int alphaBits;
	
	/**
	 * The data type of each color component.
	 */
	public final DataType dataType;
	
	/**
	 * Creates a new color format.
	 * @param redBits The number of bits to be allocated for the red component of the color.
	 * @param greenBits The number of bits to be allocated for the green component of the color.
	 * @param blueBits The number of bits to be allocated for the blue component of the color.
	 * @param alphaBits The number of bits to be allocated for the alpha component of the color.
	 * @param dataType The data type of each color component.
	 */
	public ColorFormat(int redBits, int greenBits, int blueBits, int alphaBits, DataType dataType) 
	{
		this.redBits = redBits;
		this.greenBits = greenBits;
		this.blueBits = blueBits;
		this.alphaBits = alphaBits;
		this.dataType = dataType;
	}
	
	/**
	 * An 8-bit format consisting of a single 8-bit fixed-point red component
	 */
	public final static ColorFormat R8 = new ColorFormat(8, 0, 0, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * An 8-bit format consisting of a single 8-bit fixed-point signed red component
	 */
	public final static ColorFormat R8_SNORM = new ColorFormat(8, 0, 0, 0, DataType.SIGNED_FIXED_POINT);
	
	/**
	 * A 16-bit format consisting of a single 16-bit fixed-point red component
	 */
	public final static ColorFormat R16 = new ColorFormat(16, 0, 0, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 16-bit format consisting of a single 16-bit fixed-point signed red component
	 */
	public final static ColorFormat R16_SNORM = new ColorFormat(16, 0, 0, 0, DataType.SIGNED_FIXED_POINT);
	
	/**
	 * A 16-bit format consisting of two 8-bit fixed-point components, red and green.
	 */
	public final static ColorFormat RG8 = new ColorFormat(8, 8, 0, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 32-bit format consisting of two 8-bit fixed-point signed components, red and green.
	 */
	public final static ColorFormat RG8_SNORM = new ColorFormat(8, 8, 0, 0, DataType.SIGNED_FIXED_POINT);
	
	/**
	 * A 16-bit format consisting of two 16-bit fixed-point components, red and green.
	 */
	public final static ColorFormat RG16 = new ColorFormat(16, 16, 0, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 32-bit format consisting of two 16-bit fixed-point signed components, red and green.
	 */
	public final static ColorFormat RG16_SNORM = new ColorFormat(16, 16, 0, 0, DataType.SIGNED_FIXED_POINT);
	
	/**
	 * An 8-bit format consisting of three fixed-point components for red, green, and blue, with 3 bits allocated for red, 3 bits allocated for green, and 2 bits allocated for blue.
	 */
	public final static ColorFormat R3_G3_B2 = new ColorFormat(3, 3, 2, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 12-bit format consisting of three 4-bit fixed-point components for red, green, and blue.
	 */
	public final static ColorFormat RGB4 = new ColorFormat(4, 4, 4, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 15-bit format consisting of three 5-bit fixed-point components for red, green, and blue.
	 */
	public final static ColorFormat RGB5 = new ColorFormat(5, 5, 5, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 16-bit format consisting of three fixed-point components for red, green, and blue, with 5 bits allocated for red, 6 bits allocated for green, and 5 bits allocated for blue.
	 */
	public final static ColorFormat RGB565 = new ColorFormat(5, 6, 5, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 24-bit format consisting of three 8-bit fixed-point components for red, green, and blue.
	 */
	public final static ColorFormat RGB8 = new ColorFormat(8, 8, 8, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 24-bit format consisting of three 8-bit fixed-point signed components for red, green, and blue.
	 */
	public final static ColorFormat RGB8_SNORM = new ColorFormat(8, 8, 8, 0, DataType.SIGNED_FIXED_POINT);
	
	/**
	 * A 30-bit format consisting of three 10-bit fixed-point components for red, green, and blue.
	 */
	public final static ColorFormat RGB10 = new ColorFormat(10, 10, 10, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 36-bit format consisting of three 12-bit fixed-point components for red, green, and blue.
	 */
	public final static ColorFormat RGB12 = new ColorFormat(12, 12, 12, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 48-bit format consisting of three 16-bit fixed-point components for red, green, and blue.
	 */
	public final static ColorFormat RGB16 = new ColorFormat(16, 16, 16, 0, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 48-bit format consisting of three 16-bit fixed-point signed components for red, green, and blue.
	 */
	public final static ColorFormat RGB16_SNORM = new ColorFormat(16, 16, 16, 0, DataType.SIGNED_FIXED_POINT);
	
	/**
	 * An 8-bit format consisting of four 2-bit fixed-point components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA2 = new ColorFormat(2, 2, 2, 2, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 16-bit format consisting of four 4-bit fixed-point components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA4 = new ColorFormat(4, 4, 4, 4, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 16-bit format consisting of four fixed-point components: 5-bit red, green, and blue, and 1 bit for alpha.
	 */
	public final static ColorFormat RGB5_A1 = new ColorFormat(5, 5, 5, 1, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 32-bit format consisting of four 8-bit fixed-point components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA8 = new ColorFormat(8, 8, 8, 8, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 32-bit format consisting of four 8-bit fixed-point signed components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA8_SNORM = new ColorFormat(8, 8, 8, 8, DataType.SIGNED_FIXED_POINT);
	
	/**
	 * A 32-bit format consisting of four fixed-point components: 10-bit red, green, and blue, and 2 bits for alpha.
	 */
	public final static ColorFormat RGBA10_A2 = new ColorFormat(10, 10, 10, 2, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 32-bit integer format consisting of four unsigned components: 10-bit red, green, and blue, and 2 bits for alpha.
	 */
	public final static ColorFormat RGBA10_A2UI = new ColorFormat(10, 10, 10, 2, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 48-bit format consisting of four 12-bit fixed-point components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA12 = new ColorFormat(12, 12, 12, 12, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 64-bit format consisting of four 16-bit fixed-point components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA16 = new ColorFormat(16, 16, 16, 16, DataType.NORMALIZED_FIXED_POINT);
	
	/**
	 * A 64-bit format consisting of four 16-bit fixed-point signed components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA16_SNORM = new ColorFormat(16, 16, 16, 16, DataType.SIGNED_FIXED_POINT);
	
	/**
	 * A 32-bit format consisting of four 8-bit fixed-point components for red, green, blue, and alpha, stored in the sRGB color space.
	 */
	public final static ColorFormat SRGB8 = new ColorFormat(8, 8, 8, 0, DataType.SRGB_FIXED_POINT);
	
	/**
	 * A 32-bit format consisting of four 8-bit fixed-point components for red, green, blue, and alpha, with red, green, and blue stored in the sRGB color space.
	 */
	public final static ColorFormat SRGB8_ALPHA8 = new ColorFormat(8, 8, 8, 8, DataType.SRGB_FIXED_POINT);
	
	/**
	 * A 16-bit format consisting of a single 16-bit floating-point red component
	 */
	public final static ColorFormat R16F = new ColorFormat(16, 0, 0, 0, DataType.FLOATING_POINT);
	
	/**
	 * A 32-bit format consisting of two 16-bit floating-point components, red and green.
	 */
	public final static ColorFormat RG16F = new ColorFormat(16, 16, 0, 0, DataType.FLOATING_POINT);
	
	/**
	 * A 48-bit format consisting of three 16-bit floating-point components for red, green, and blue.
	 */
	public final static ColorFormat RGB16F = new ColorFormat(16, 16, 16, 0, DataType.FLOATING_POINT);
	
	/**
	 * A 64-bit format consisting of four 16-bit floating-point components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA16F = new ColorFormat(16, 16, 16, 16, DataType.FLOATING_POINT);
	
	/**
	 * A 32-bit format consisting of a single 32-bit floating-point red component
	 */
	public final static ColorFormat R32F = new ColorFormat(32, 0, 0, 0, DataType.FLOATING_POINT);
	
	/**
	 * A 64-bit format consisting of two 32-bit floating-point components, red and green.
	 */
	public final static ColorFormat RG32F = new ColorFormat(32, 32, 0, 0, DataType.FLOATING_POINT);
	
	/**
	 * A 96-bit format consisting of three 16-bit floating-point components for red, green, and blue.
	 */
	public final static ColorFormat RGB32F = new ColorFormat(32, 32, 32, 0, DataType.FLOATING_POINT);
	
	/**
	 * A 128-bit format consisting of four 32-bit floating-point components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA32F = new ColorFormat(32, 32, 32, 32, DataType.FLOATING_POINT);
	
	/**
	 * A 32-bit format consisting of a three floating-point red components for red, green, and blue, with 11 bits allocated for red, 11 bits allocated for green, and 10 bits allocated for blue.
	 */
	public final static ColorFormat R11F_G11F_B10F = new ColorFormat(11, 11, 10, 0, DataType.FLOATING_POINT);
	
	/**
	 * An 8-bit integer format consisting of a single 8-bit signed red component
	 */
	public final static ColorFormat R8I = new ColorFormat(8, 0, 0, 0, DataType.SIGNED_INTEGER);
	
	/**
	 * An 8-bit integer format consisting of a single 8-bit unsigned red component
	 */
	public final static ColorFormat R8UI = new ColorFormat(8, 0, 0, 0, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 16-bit integer format consisting of a single 16-bit signed red component
	 */
	public final static ColorFormat R16I = new ColorFormat(16, 0, 0, 0, DataType.SIGNED_INTEGER);
	
	/**
	 * A 16-bit integer format consisting of a single 16-bit unsigned red component
	 */
	public final static ColorFormat R16UI = new ColorFormat(16, 0, 0, 0, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 32-bit integer format consisting of a single 32-bit signed red component
	 */
	public final static ColorFormat R32I = new ColorFormat(32, 0, 0, 0, DataType.SIGNED_INTEGER);
	
	/**
	 * A 32-bit integer format consisting of a single 32-bit unsigned red component
	 */
	public final static ColorFormat R32UI = new ColorFormat(32, 0, 0, 0, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 16-bit integer format consisting of two 8-bit signed components, red and green.
	 */
	public final static ColorFormat RG8I = new ColorFormat(8, 8, 0, 0, DataType.SIGNED_INTEGER);
	
	/**
	 * A 16-bit integer format consisting of two 8-bit unsigned components, red and green.
	 */
	public final static ColorFormat RG8UI = new ColorFormat(8, 8, 0, 0, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 32-bit integer format consisting of two 16-bit signed components, red and green.
	 */
	public final static ColorFormat RG16I = new ColorFormat(16, 16, 0, 0, DataType.SIGNED_INTEGER);
	
	/**
	 * A 32-bit integer format consisting of two 16-bit unsigned components, red and green.
	 */
	public final static ColorFormat RG16UI = new ColorFormat(16, 16, 0, 0, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 64-bit integer format consisting of two 32-bit signed components, red and green.
	 */
	public final static ColorFormat RG32I = new ColorFormat(32, 32, 0, 0, DataType.SIGNED_INTEGER);
	
	/**
	 * A 64-bit integer format consisting of two 32-bit unsigned components, red and green.
	 */
	public final static ColorFormat RG32UI = new ColorFormat(32, 32, 0, 0, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 24-bit integer format consisting of three 8-bit signed components for red, green, and blue.
	 */
	public final static ColorFormat RGB8I = new ColorFormat(8, 8, 8, 0, DataType.SIGNED_INTEGER);
	
	/**
	 * A 24-bit integer format consisting of three 8-bit unsigned components for red, green, and blue.
	 */
	public final static ColorFormat RGB8UI = new ColorFormat(8, 8, 8, 0, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 48-bit integer format consisting of three 16-bit signed components for red, green, and blue.
	 */
	public final static ColorFormat RGB16I = new ColorFormat(16, 16, 16, 0, DataType.SIGNED_INTEGER);
	
	/**
	 * A 48-bit integer format consisting of three 16-bit unsigned components for red, green, and blue.
	 */
	public final static ColorFormat RGB16UI = new ColorFormat(16, 16, 16, 0, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 96-bit integer format consisting of three 32-bit signed components for red, green, and blue.
	 */
	public final static ColorFormat RGB32I = new ColorFormat(32, 32, 32, 0, DataType.SIGNED_INTEGER);
	
	/**
	 * A 96-bit integer format consisting of three 32-bit unsigned components for red, green, and blue.
	 */
	public final static ColorFormat RGB32UI = new ColorFormat(32, 32, 32, 0, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 32-bit integer format consisting of four 8-bit signed components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA8I = new ColorFormat(8, 8, 8, 8, DataType.SIGNED_INTEGER);
	
	/**
	 * A 32-bit integer format consisting of four 8-bit unsigned components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA8UI = new ColorFormat(8, 8, 8, 8, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 64-bit integer format consisting of four 16-bit signed components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA16I = new ColorFormat(16, 16, 16, 16, DataType.SIGNED_INTEGER);
	
	/**
	 * A 64-bit integer format consisting of four 16-bit unsigned components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA16UI = new ColorFormat(16, 16, 16, 16, DataType.UNSIGNED_INTEGER);
	
	/**
	 * A 128-bit integer format consisting of four 32-bit signed components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA32I = new ColorFormat(32, 32, 32, 32, DataType.SIGNED_INTEGER);
	
	/**
	 * A 128-bit integer format consisting of four 32-bit unsigned components for red, green, blue, and alpha.
	 */
	public final static ColorFormat RGBA32UI = new ColorFormat(32, 32, 32, 32, DataType.UNSIGNED_INTEGER);
	
}
