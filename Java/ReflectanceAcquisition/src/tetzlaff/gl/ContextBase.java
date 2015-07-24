package tetzlaff.gl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import tetzlaff.gl.builders.ColorTextureBuilder;

public abstract class ContextBase<ContextType extends Context<? super ContextType>> implements Context<ContextType>
{
	@Override
	public ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(File imageFile, boolean flipVertical) throws IOException
	{
		return get2DColorTextureBuilder(new FileInputStream(imageFile), flipVertical);
	}
	
	@Override
	public ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(InputStream imageStream, boolean flipVertical) throws IOException
	{
		return get2DColorTextureBuilder(imageStream, null, flipVertical);
	}
	
	@Override
	public ColorTextureBuilder<ContextType, ? extends Texture2D<ContextType>> get2DColorTextureBuilder(File imageFile, File maskFile, boolean flipVertical) throws IOException
	{
		return get2DColorTextureBuilder(new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical);
	}
}
