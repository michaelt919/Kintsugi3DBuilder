package tetzlaff.ulf.app;

import tetzlaff.ulf.SampledLuminanceEncoding;

public class TestLuminanceMapping 
{
	public static void main(String[] args)
	{
//		new SampledLuminanceEncoding(
//				new double[] { 3.1, 9.0, 19.8, 36.2, 59.1, 90.0 }, 
//				new byte[] { 50, 104, (byte)142, (byte)166, (byte)175, (byte)186 },
//				2.2f)
//			.sampleDecodeFunction();
		
		new SampledLuminanceEncoding(2.2f).sampleDecodeFunction();
	}
}
