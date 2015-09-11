package tetzlaff.imagebasedmicrofacet;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture2D;
import tetzlaff.ulf.UnstructuredLightField;

public class SampledMicrofacetField<ContextType extends Context<ContextType>>
{
	public final UnstructuredLightField<ContextType> ulf;
	public final Texture2D<ContextType> diffuseTexture;
	public final Texture2D<ContextType> normalTexture;
	
	public SampledMicrofacetField(UnstructuredLightField<ContextType> ulf, File diffuseFile, File normalFile, ContextType context) throws IOException
	{
		this.ulf = ulf;
		
		if (diffuseFile != null && diffuseFile.exists())
		{
			System.out.println("Diffuse texture found.");
			diffuseTexture = context.get2DColorTextureBuilder(diffuseFile, true)
					.setInternalFormat(ColorFormat.RGB8)
					.setMipmapsEnabled(true)
					.setLinearFilteringEnabled(true)
					.createTexture();
		}
		else
		{
			diffuseTexture = null;
		}
		
		if (normalFile != null && normalFile.exists())
		{
			System.out.println("Normal texture found.");
			normalTexture = context.get2DColorTextureBuilder(normalFile, true)
					.setInternalFormat(ColorFormat.RGB8)
					.setMipmapsEnabled(true)
					.setLinearFilteringEnabled(true)
					.createTexture();
		}
		else
		{
			normalTexture = null;
		}
	}
}
