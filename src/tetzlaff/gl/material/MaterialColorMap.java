package tetzlaff.gl.material;

public class MaterialColorMap extends MaterialTextureMap
{
	private boolean gammaCorrectionRequired;
	
	public MaterialColorMap() 
	{
		gammaCorrectionRequired = false;
	}

	public boolean isGammaCorrectionRequired()
	{
		return gammaCorrectionRequired;
	}

	public void setGammaCorrectionRequired(boolean colorCorrectionEnabled)
	{
		this.gammaCorrectionRequired = colorCorrectionEnabled;
	}
}
