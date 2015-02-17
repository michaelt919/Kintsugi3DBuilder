package tetzlaff.gl;

public class AlphaBlendingFunction 
{
	public enum Weight
	{
		ZERO,
		ONE,
		SRC_COLOR,
		ONE_MINUS_SRC_COLOR,
		DST_COLOR,
		ONE_MINUS_DST_COLOR,
		SRC_ALPHA,
		ONE_MINUS_SRC_ALPHA,
		DST_ALPHA,
		ONE_MINUS_DST_ALPHA
	}
	
	public final Weight sourceWeightFunction;
	public final Weight destinationWeightFunction;

	public AlphaBlendingFunction(Weight source, Weight destination) 
	{
		this.sourceWeightFunction = source;
		this.destinationWeightFunction = destination;
	}

}
