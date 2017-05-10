package tetzlaff.gl.builders;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Cubemap2D;
import tetzlaff.gl.CubemapFace;
import tetzlaff.gl.helpers.ByteVertexList;
import tetzlaff.gl.helpers.DoubleVertexList;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.ShortVertexList;

public interface ColorCubemapBuilder <ContextType extends Context<ContextType>, TextureType extends Cubemap2D<ContextType>> extends ColorTextureBuilder<ContextType, TextureType>
{
	ColorCubemapBuilder<ContextType, TextureType> loadFace(CubemapFace face, InputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;
	ColorCubemapBuilder<ContextType, TextureType> loadFaceFromHDR(CubemapFace face, BufferedInputStream imageStream, InputStream maskStream, boolean flipVertical) throws IOException;
	
	default ColorCubemapBuilder<ContextType, TextureType> loadFace(CubemapFace face, InputStream imageStream, boolean flipVertical) throws IOException
	{
		return loadFace(face, imageStream, null, flipVertical);
	}
	
	default ColorCubemapBuilder<ContextType, TextureType> loadFace(CubemapFace face, File imageFile, File maskFile, boolean flipVertical) throws IOException
	{
		if (imageFile.getName().endsWith(".hdr"))
		{
			return loadFace(face, new BufferedInputStream(new FileInputStream(imageFile)), new FileInputStream(maskFile), flipVertical);
		}
		else
		{
			return loadFace(face, new FileInputStream(imageFile), new FileInputStream(maskFile), flipVertical);
		}
	}
	
	default ColorCubemapBuilder<ContextType, TextureType> loadFaceFromHDR(CubemapFace face, BufferedInputStream imageStream, boolean flipVertical) throws IOException
	{
		return loadFaceFromHDR(face, imageStream, null, flipVertical);
	}
	
	default ColorCubemapBuilder<ContextType, TextureType> loadFace(CubemapFace face, File imageFile, boolean flipVertical) throws IOException
	{
		if (imageFile.getName().endsWith(".hdr"))
		{
			return loadFaceFromHDR(face, new BufferedInputStream(new FileInputStream(imageFile)), flipVertical);
		}
		else
		{
			return loadFace(face, new FileInputStream(imageFile), flipVertical);
		}
	}
	
	ColorCubemapBuilder<ContextType, TextureType> loadFace(CubemapFace face, int width, int height, ByteVertexList data);
	ColorCubemapBuilder<ContextType, TextureType> loadFace(CubemapFace face, int width, int height, ShortVertexList data);
	ColorCubemapBuilder<ContextType, TextureType> loadFace(CubemapFace face, int width, int height, IntVertexList data);
	ColorCubemapBuilder<ContextType, TextureType> loadFace(CubemapFace face, int width, int height, FloatVertexList data);
	ColorCubemapBuilder<ContextType, TextureType> loadFace(CubemapFace face, int width, int height, DoubleVertexList data);
	
	ColorCubemapBuilder<ContextType, TextureType> setInternalFormat(ColorFormat format);
	ColorCubemapBuilder<ContextType, TextureType> setInternalFormat(CompressionFormat format);
	
	ColorCubemapBuilder<ContextType, TextureType> setMultisamples(int samples, boolean fixedSampleLocations);
	ColorCubemapBuilder<ContextType, TextureType> setMipmapsEnabled(boolean enabled);
	ColorCubemapBuilder<ContextType, TextureType> setLinearFilteringEnabled(boolean enabled);
	ColorCubemapBuilder<ContextType, TextureType> setMaxAnisotropy(float maxAnisotropy);
}
