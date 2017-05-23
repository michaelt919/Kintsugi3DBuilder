package tetzlaff.util;

import java.util.function.DoubleUnaryOperator;

public class CubicHermiteSpline implements DoubleUnaryOperator
{
	private double[] x;
	private double[] y;
	private double[] m;
	
	public CubicHermiteSpline(double[] x, double[] y, boolean monotone)
	{
		if (x.length != y.length)
		{
			throw new IllegalArgumentException("Input arrays must be of equal length.");
		}
		
		if (x.length < 2)
		{
			throw new IllegalArgumentException("Input arrays must have length of at least 2.");
		}
		
		this.x = x;
		this.y = y;
		
		double[] d = new double[x.length-1];
		for (int k = 0; k < d.length; k++)
		{
			d[k] = (y[k+1] - y[k]) / (x[k+1] - x[k]);
		}
		
		this.m = new double[x.length];
		m[0] = d[0];
		for (int k = 1; k < d.length; k++)
		{
			m[k] = (d[k-1] + d[k]) / 2;
		}
		m[m.length-1] = d[d.length-1];
		
		if (monotone)
		{
			for (int k = 0; k < d.length-1; k++)
			{
				if (d[k] == 0.0)
				{
					m[k] = 0;
					m[k+1] = 0;
				}
				
				double a = m[k] / d[k];
				double b = m[k+1] / d[k];
				
				if (a < 0)
				{
					m[k] = 0;
				}
				
				if (b < 0) 
				{
					m[k+1] = 0;
				}
				
				double distSq = a*a+b*b;
				if (distSq > 9)
				{
					double t = 3 / Math.sqrt(distSq);
					m[k] = t*a*d[k];
					m[k+1] = t*b*d[k];
				}
			}
		}
	}
	
	public CubicHermiteSpline(double[] x, double[] y, double[] m)
	{
		if (x.length != y.length || x.length != m.length)
		{
			throw new IllegalArgumentException("Input arrays must all be of equal length.");
		}
		
		if (x.length < 2)
		{
			throw new IllegalArgumentException("Input arrays must have length of at least 2.");
		}
		
		this.x = x;
		this.y = y;
		this.m = m;
	}
	
	public double applyAsDouble(double value)
	{
		int k = 0;
		while (k < x.length-2 && value > x[k+1])
		{
			k++;
		}
		
		double t = (value - x[k]) / (x[k+1] - x[k]);
		double tSq = t*t;
		
		return h00(t,tSq) * y[k] +
				h10(t,tSq) * (x[k+1] - x[k]) * m[k] +
				h01(t,tSq) * y[k+1] +
				h11(t,tSq) * (x[k+1] - x[k]) * m[k+1];
	}
	
	private static double h00(double t, double tSq)
	{
		return (2*t-3)*tSq + 1;
	}
	
	private static double h10(double t, double tSq)
	{
		return (t-2)*tSq + t;
	}
	
	private static double h01(double t, double tSq)
	{
		return (3-2*t)*tSq;
	}
	
	private static double h11(double t, double tSq)
	{
		return (t-1)*tSq;
	}
}
