package tetzlaff.gl.helpers;

public class MaterialScalarMap extends MaterialTextureMap
{
	private MaterialTextureChannel channel;

	public MaterialScalarMap() 
	{
		channel = MaterialTextureChannel.Unspecified;
	}

	public MaterialTextureChannel getChannel() 
	{
		return channel;
	}

	public void setChannel(MaterialTextureChannel channel) 
	{
		this.channel = channel;
	}
}
