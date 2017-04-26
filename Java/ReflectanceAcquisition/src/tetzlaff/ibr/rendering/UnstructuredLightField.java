package tetzlaff.ibr.rendering;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.Context;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.ibr.IBRLoadOptions;
import tetzlaff.ibr.IBRLoadingMonitor;
import tetzlaff.ibr.IBRSettings;
import tetzlaff.ibr.ViewSet;
import tetzlaff.ibr.ViewSetImageOptions;

public class UnstructuredLightField<ContextType extends Context<ContextType>>
{
	public static <ContextType extends Context<ContextType>> 
		UnstructuredLightField<ContextType> loadFromAgisoftXMLFile(File xmlFile, File meshFile, ContextType context) throws IOException
	{
		return UnstructuredLightField.loadFromAgisoftXMLFile(xmlFile, meshFile, new IBRLoadOptions(new ViewSetImageOptions(null, false, false, false), false, 0, 0), null, context);
	}

	public static <ContextType extends Context<ContextType>>
		UnstructuredLightField<ContextType> loadFromAgisoftXMLFile(File xmlFile, File meshFile, IBRLoadOptions loadOptions, IBRLoadingMonitor loadingCallback, ContextType context) throws IOException
	{
		ViewSet viewSet;
		VertexMesh proxy;
		
		File directoryPath = xmlFile.getParentFile();
        proxy = new VertexMesh("OBJ", meshFile);
        viewSet = ViewSet.loadFromAgisoftXMLFile(xmlFile, loadOptions.getImageOptions(), context, loadingCallback);
        
        
    	return new UnstructuredLightField<ContextType>(viewSet, proxy);
	}
	
	public static <ContextType extends Context<ContextType>>
		UnstructuredLightField<ContextType> loadFromVSETFile(File vsetFile, ContextType context) throws IOException
	{
		return UnstructuredLightField.loadFromVSETFile(vsetFile, new IBRLoadOptions(new ViewSetImageOptions(null, false, false, false), false, 0, 0), null, context);
	}

	public static <ContextType extends Context<ContextType>>
		UnstructuredLightField<ContextType> loadFromVSETFile(File vsetFile, IBRLoadOptions loadOptions, IBRLoadingMonitor loadingCallback, ContextType context) throws IOException
	{
		ViewSet viewSet;
		VertexMesh proxy;
		
		File directoryPath = vsetFile.getParentFile();
        
        
        
        
        
        
    	return new UnstructuredLightField<ContextType>(viewSet, proxy);
	}
}
