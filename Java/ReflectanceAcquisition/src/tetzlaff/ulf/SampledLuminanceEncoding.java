package tetzlaff.ulf;

import java.util.function.DoubleUnaryOperator;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture1D;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.helpers.CubicHermiteSpline;

public class SampledLuminanceEncoding 
{
	public final DoubleUnaryOperator decodeFunction;
	
	public SampledLuminanceEncoding(float gamma)
	{
		this.decodeFunction = (encoded) -> Math.pow(encoded / 255.0f, gamma);
	}
	
	public SampledLuminanceEncoding(double[] linear, byte[] encoded, float gammaEstimate)
	{
		if (linear.length != encoded.length)
		{
			throw new IllegalArgumentException("Input arrays must be of equal length.");
		}
		
		double[] x = new double[encoded.length + 2];
		x[0] = 0;
		for (int k = 1; k < x.length-1; k++)
		{
			x[k] = (double)(0xFF & (int)encoded[k-1]);
		}
		x[x.length-1] = 255.0;
		
		double scale = (float)Math.pow(x[x.length-2] / 255.0, gammaEstimate) / linear[linear.length-1];
		
		double[] y = new double[linear.length + 2];
		y[0] = 0;
		for (int k = 1; k < y.length-1; k++)
		{
			y[k] = linear[k-1] * scale;
		}
		y[y.length-1] = 1;
		
		this.decodeFunction = new CubicHermiteSpline(x, y, true);
	}
	
	public SampledLuminanceEncoding(DoubleUnaryOperator decodeFunction)
	{
		this.decodeFunction = decodeFunction;
	}
	
	public FloatVertexList sampleDecodeFunction()
	{
		FloatVertexList sampledDecodeFunction = new FloatVertexList(1, 256);
		for (int i = 0; i < 256; i++)
		{
			sampledDecodeFunction.set(i, 0, (float)decodeFunction.applyAsDouble(i));
		}
		
		return sampledDecodeFunction;
	}
	
	public <ContextType extends Context<ContextType>> Texture1D<ContextType> createLuminanceMap(ContextType context)
	{
		return context.get1DColorTextureBuilder(sampleDecodeFunction())
				.setInternalFormat(ColorFormat.R32F)
				.setLinearFilteringEnabled(true)
				.createTexture();
	}
}
