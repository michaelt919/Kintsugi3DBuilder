package tetzlaff.gl.builders.framebuffer;

public abstract class AttachmentSpec
{
	private int multisamples = 1;
	private boolean fixedMultisampleLocations = true;
	private boolean mipmapsEnabled = false;
	private boolean linearFilteringEnabled = false;
	
	public int getMultisamples()
	{
		return multisamples;
	}
	
	public boolean areMultisampleLocationsFixed()
	{
		return fixedMultisampleLocations;
	}
	
	public boolean areMipmapsEnabled()
	{
		return mipmapsEnabled;
	}
	
	public boolean isLinearFilteringEnabled()
	{
		return linearFilteringEnabled;
	}
	
	public AttachmentSpec setMultisamples(int samples, boolean fixedSampleLocations)
	{
		multisamples = samples;
		fixedMultisampleLocations = fixedSampleLocations;
		return this;
	}
	
	public AttachmentSpec setMipmapsEnabled(boolean enabled)
	{
		mipmapsEnabled = enabled;
		return this;
	}
	
	public AttachmentSpec setLinearFilteringEnabled(boolean enabled)
	{
		linearFilteringEnabled = enabled;
		return this;
	}
}
