package tetzlaff.gl;

public class ColorFormat 
{
	public static enum DataType
	{
		NormalizedFixedPoint,
		SRGBFixedPoint,
		SignedFixedPoint,
		FloatingPoint,
		UnsignedInteger,
		SignedInteger
	}
	
	public final int redBits;
	public final int greenBits;
	public final int blueBits;
	public final int alphaBits;
	public final DataType dataType;
	
	public ColorFormat(int redBits, int greenBits, int blueBits, int alphaBits, DataType dataType) 
	{
		this.redBits = redBits;
		this.greenBits = greenBits;
		this.blueBits = blueBits;
		this.alphaBits = alphaBits;
		this.dataType = dataType;
	}
	
	public final static ColorFormat R8 = new ColorFormat(8, 0, 0, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat R8_SNORM = new ColorFormat(8, 0, 0, 0, DataType.SignedFixedPoint);
	public final static ColorFormat R16 = new ColorFormat(16, 0, 0, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat R16_SNORM = new ColorFormat(16, 0, 0, 0, DataType.SignedFixedPoint);
	
	public final static ColorFormat RG8 = new ColorFormat(8, 8, 0, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat RG8_SNORM = new ColorFormat(8, 8, 0, 0, DataType.SignedFixedPoint);
	public final static ColorFormat RG16 = new ColorFormat(16, 16, 0, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat RG16_SNORM = new ColorFormat(16, 16, 0, 0, DataType.SignedFixedPoint);
	
	public final static ColorFormat R3_G3_B2 = new ColorFormat(3, 3, 2, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGB4 = new ColorFormat(4, 4, 4, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGB5 = new ColorFormat(5, 5, 5, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGB565 = new ColorFormat(5, 6, 5, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGB8 = new ColorFormat(8, 8, 8, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGB8_SNORM = new ColorFormat(8, 8, 8, 0, DataType.SignedFixedPoint);
	public final static ColorFormat RGB10 = new ColorFormat(10, 10, 10, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGB12 = new ColorFormat(12, 12, 12, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGB16 = new ColorFormat(16, 16, 16, 0, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGB16_SNORM = new ColorFormat(16, 16, 16, 0, DataType.SignedFixedPoint);
	
	public final static ColorFormat RGBA2 = new ColorFormat(2, 2, 2, 2, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGBA4 = new ColorFormat(4, 4, 4, 4, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGB5_A1 = new ColorFormat(5, 5, 5, 1, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGBA8 = new ColorFormat(8, 8, 8, 8, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGBA8_SNORM = new ColorFormat(8, 8, 8, 8, DataType.SignedFixedPoint);
	public final static ColorFormat RGBA10_A2 = new ColorFormat(10, 10, 10, 2, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGBA10_A2UI = new ColorFormat(10, 10, 10, 2, DataType.UnsignedInteger);
	public final static ColorFormat RGBA12 = new ColorFormat(12, 12, 12, 12, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGBA16 = new ColorFormat(16, 16, 16, 16, DataType.NormalizedFixedPoint);
	public final static ColorFormat RGBA16_SNORM = new ColorFormat(16, 16, 16, 16, DataType.SignedFixedPoint);
	
	public final static ColorFormat SRGB8 = new ColorFormat(8, 8, 8, 0, DataType.SRGBFixedPoint);
	public final static ColorFormat SRGB8_ALPHA8 = new ColorFormat(8, 8, 8, 8, DataType.SRGBFixedPoint);
	
	public final static ColorFormat R16F = new ColorFormat(16, 0, 0, 0, DataType.FloatingPoint);
	public final static ColorFormat RG16F = new ColorFormat(16, 16, 0, 0, DataType.FloatingPoint);
	public final static ColorFormat RGB16F = new ColorFormat(16, 16, 16, 0, DataType.FloatingPoint);
	public final static ColorFormat RGBA16F = new ColorFormat(16, 16, 16, 16, DataType.FloatingPoint);
	public final static ColorFormat R32F = new ColorFormat(32, 0, 0, 0, DataType.FloatingPoint);
	public final static ColorFormat RG32F = new ColorFormat(32, 32, 0, 0, DataType.FloatingPoint);
	public final static ColorFormat RGB32F = new ColorFormat(32, 32, 32, 0, DataType.FloatingPoint);
	public final static ColorFormat RGBA32F = new ColorFormat(32, 32, 32, 32, DataType.FloatingPoint);
	public final static ColorFormat R11F_G11F_B10F = new ColorFormat(11, 11, 10, 0, DataType.FloatingPoint);
	
	public final static ColorFormat R8I = new ColorFormat(8, 0, 0, 0, DataType.SignedInteger);
	public final static ColorFormat R8UI = new ColorFormat(8, 0, 0, 0, DataType.UnsignedInteger);
	public final static ColorFormat R16I = new ColorFormat(16, 0, 0, 0, DataType.SignedInteger);
	public final static ColorFormat R16UI = new ColorFormat(16, 0, 0, 0, DataType.UnsignedInteger);
	public final static ColorFormat R32I = new ColorFormat(32, 0, 0, 0, DataType.SignedInteger);
	public final static ColorFormat R32UI = new ColorFormat(32, 0, 0, 0, DataType.UnsignedInteger);
	
	public final static ColorFormat RG8I = new ColorFormat(8, 8, 0, 0, DataType.SignedInteger);
	public final static ColorFormat RG8UI = new ColorFormat(8, 8, 0, 0, DataType.UnsignedInteger);
	public final static ColorFormat RG16I = new ColorFormat(16, 16, 0, 0, DataType.SignedInteger);
	public final static ColorFormat RG16UI = new ColorFormat(16, 16, 0, 0, DataType.UnsignedInteger);
	public final static ColorFormat RG32I = new ColorFormat(32, 32, 0, 0, DataType.SignedInteger);
	public final static ColorFormat RG32UI = new ColorFormat(32, 32, 0, 0, DataType.UnsignedInteger);
	
	public final static ColorFormat RGB8I = new ColorFormat(8, 8, 8, 0, DataType.SignedInteger);
	public final static ColorFormat RGB8UI = new ColorFormat(8, 8, 8, 0, DataType.UnsignedInteger);
	public final static ColorFormat RGB16I = new ColorFormat(16, 16, 16, 0, DataType.SignedInteger);
	public final static ColorFormat RGB16UI = new ColorFormat(16, 16, 16, 0, DataType.UnsignedInteger);
	public final static ColorFormat RGB32I = new ColorFormat(32, 32, 32, 0, DataType.SignedInteger);
	public final static ColorFormat RGB32UI = new ColorFormat(32, 32, 32, 0, DataType.UnsignedInteger);
	
	public final static ColorFormat RGBA8I = new ColorFormat(8, 8, 8, 8, DataType.SignedInteger);
	public final static ColorFormat RGBA8UI = new ColorFormat(8, 8, 8, 8, DataType.UnsignedInteger);
	public final static ColorFormat RGBA16I = new ColorFormat(16, 16, 16, 16, DataType.SignedInteger);
	public final static ColorFormat RGBA16UI = new ColorFormat(16, 16, 16, 16, DataType.UnsignedInteger);
	public final static ColorFormat RGBA32I = new ColorFormat(32, 32, 32, 32, DataType.SignedInteger);
	public final static ColorFormat RGBA32UI = new ColorFormat(32, 32, 32, 32, DataType.UnsignedInteger);
	
}
