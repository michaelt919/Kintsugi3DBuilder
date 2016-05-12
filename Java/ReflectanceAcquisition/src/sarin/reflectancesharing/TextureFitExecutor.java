package sarin.reflectancesharing;

//import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
//import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
//import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
//import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//import javax.imageio.ImageIO;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.VertexBuffer;
//import tetzlaff.gl.helpers.Matrix3;
//import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector2;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.ulf.ViewSet;

import Jama.*;
import java.util.Random;
//import java.util.Set;

public class TextureFitExecutor<ContextType extends Context<ContextType>>
{
	// Debug parameters
	private static final boolean DEBUG = false;

	private ContextType context;
	private File vsetFile;
	private File objFile;
	private File imageDir;
	private File maskDir;
	private File rescaleDir;
	private File outputDir;
	private Vector3 lightOffset;
	private Vector3 lightIntensity;
	private TextureFitParameters param;
	
	public TextureFitExecutor(ContextType context, File vsetFile, File objFile, File imageDir, File maskDir, File rescaleDir, File outputDir,
			Vector3 lightOffset, Vector3 lightIntensity, TextureFitParameters param) 
	{
		this.context = context;
		this.vsetFile = vsetFile;
		this.objFile = objFile;
		this.imageDir = imageDir;
		this.maskDir = maskDir;
		this.rescaleDir = rescaleDir;
		this.outputDir = outputDir;
		this.lightOffset = lightOffset;
		this.lightIntensity = lightIntensity;
		this.param = param;
	}

	@SuppressWarnings("unused")
	public void execute() throws IOException
	{
		final int DEBUG_PIXEL_X = 322;
		final int DEBUG_PIXEL_Y = param.getTextureSize() - 365;

    	System.out.println("Max vertex uniform components across all blocks:" + context.getMaxCombinedVertexUniformComponents());
    	System.out.println("Max fragment uniform components across all blocks:" + context.getMaxCombinedFragmentUniformComponents());
    	System.out.println("Max size of a uniform block in bytes:" + context.getMaxUniformBlockSize());
    	System.out.println("Max texture array layers:" + context.getMaxArrayTextureLayers());
		
		System.out.println("Loading view set...");
    	Date timestamp = new Date();
		
    	ViewSet<ContextType> viewSet = null;
    	String[] vsetFileNameParts = vsetFile.getName().split("\\.");
    	String fileExt = vsetFileNameParts[vsetFileNameParts.length-1];
    	if (fileExt.equalsIgnoreCase("vset"))
    	{
    		System.out.println("Loading from VSET file.");
    		viewSet = ViewSet.loadFromVSETFile(vsetFile, context);
    	}
    	else if (fileExt.equalsIgnoreCase("xml"))
    	{
    		System.out.println("Loading from Agisoft Photoscan XML file.");
    		viewSet = ViewSet.loadFromAgisoftXMLFile(vsetFile, null, lightOffset, lightIntensity, context, null);
    	}
    	else
    	{
    		System.out.println("Unrecognized file type, aborting.");
    		return;
    	}
    	
    	outputDir.mkdir();
    	if (DEBUG)
    	{
    		new File(outputDir, "debug").mkdir();
    	}
    	
    	System.out.println("Loading view set completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	
    	System.out.println("Loading and compiling shader programs...");
    	timestamp = new Date();
		
		context.enableDepthTest();
    	//context.enableBackFaceCulling();
		context.disableBackFaceCulling();
    	
    	Program<ContextType> depthRenderingProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders/depth.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders/depth.frag"))
    			.createProgram();
    	
    	Program<ContextType> projTexProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", "projtex_single.frag"))
    			.createProgram();
    	
    	Program<ContextType> diffuseFitProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", param.isImagePreprojectionUseEnabled() ? "diffusefit_texspace.frag" : "diffusefit_imgspace.frag"))
    			.createProgram();
		
    	Program<ContextType> specularFitProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", param.isImagePreprojectionUseEnabled() ? "specularfit_texspace.frag" : "specularfit_imgspace.frag"))
    			.createProgram();
		
    	Program<ContextType> diffuseDebugProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", "projtex_multi.frag"))
    			.createProgram();
		
    	Program<ContextType> specularDebugProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "texspace.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", "speculardebug_imgspace.frag"))
    			.createProgram();
		
    	Program<ContextType> textureRectProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "texturerect.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", "simpletexture.frag"))
    			.createProgram();
		
    	Program<ContextType> holeFillProgram = context.getShaderProgramBuilder()
    			.addShader(ShaderType.VERTEX, new File("shaders", "texturerect.vert"))
    			.addShader(ShaderType.FRAGMENT, new File("shaders", "holefill.frag"))
    			.createProgram();
		
    	System.out.println("Shader compilation completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	
    	System.out.println("Loading mesh...");
    	timestamp = new Date();
    	
    	VertexMesh mesh = new VertexMesh("OBJ", objFile);
    	VertexBuffer<ContextType> positionBuffer = context.createVertexBuffer().setData(mesh.getVertices());
    	VertexBuffer<ContextType> texCoordBuffer = context.createVertexBuffer().setData(mesh.getTexCoords());
    	VertexBuffer<ContextType> normalBuffer = context.createVertexBuffer().setData(mesh.getNormals());
    	VertexBuffer<ContextType> orthoTangentsBuffer = context.createVertexBuffer().setData(mesh.getTangents());
    	
    	System.out.println("Loading mesh completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	
    	File tmpDir = new File(outputDir, "tmp");
    	
    	Texture3D<ContextType> viewTextures = null;
    	Texture3D<ContextType> depthTextures = null;
    	
//    	if (param.isImagePreprojectionUseEnabled() && param.isImagePreprojectionGenerationEnabled())
    	{
    		System.out.println("Pre-projecting images into texture space...");
	    	timestamp = new Date();
	    	
	    	FramebufferObject<ContextType> projTexFBO = 
    			context.getFramebufferObjectBuilder(param.getTextureSize() / param.getTextureSubdivision(), param.getTextureSize() / param.getTextureSubdivision())
    				.addColorAttachments(ColorFormat.RGBA32F, 6)
    				.createFramebufferObject();
	    	Renderable<ContextType> projTexRenderable = context.createRenderable(projTexProgram);
	    	
	    	projTexRenderable.addVertexBuffer("position", positionBuffer);
	    	projTexRenderable.addVertexBuffer("texCoord", texCoordBuffer);
	    	projTexRenderable.addVertexBuffer("normal", normalBuffer);
	    	projTexRenderable.addVertexBuffer("tangent", orthoTangentsBuffer);
	    	
	    	projTexRenderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
	    	projTexRenderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
	    	
	    	tmpDir.mkdir();
	    	
	    	// Write c, lambda, and u,v,w to File 
    		Writer coefficients = new BufferedWriter( new OutputStreamWriter(
					new FileOutputStream("C:\\Users\\Sarin\\Downloads\\coefficients.txt"), "utf-8"));
    		Writer lambdas = new BufferedWriter( new OutputStreamWriter(
					new FileOutputStream("C:\\Users\\Sarin\\Downloads\\lambdas.txt"), "utf-8"));
    		Writer uvw = new BufferedWriter( new OutputStreamWriter(
					new FileOutputStream("C:\\Users\\Sarin\\Downloads\\uvw.txt"), "utf-8"));
    		// initializing matrices to solve for the coefficients

    		int numberOfPoints = 100;
    		double[][] g; //= new double[numberOfPoints*viewSet.getCameraPoseCount()][3];
    		double[][] p; //= new double[numberOfPoints*viewSet.getCameraPoseCount()][4];
    		
    		int sampleIndex = 0;
    		int tempCount = 0;
    		
    		// sumG is sum of original, which is RGB colors 
    		Map<UvwBin, Vector3> sumG = new HashMap<UvwBin, Vector3>();
    		Map<UvwBin, Integer> gCount = new HashMap<UvwBin, Integer>();
    		// sumUvw is sum of temp, which is uvwMapping 
    		Map<UvwBin, Vector3> sumUvw = new HashMap<UvwBin, Vector3>();
    		Map<UvwBin, ArrayList<UvwBin>> arrayUvw = new HashMap<UvwBin, ArrayList<UvwBin>>();
    		
    		// viewSet.getCameraPoseCount() = 100
	    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
	    	{
		    	File viewDir = new File(tmpDir, String.format("%04d", i));
		    	viewDir.mkdir();
		    	
		    	File imageFile = new File(imageDir, viewSet.getImageFileName(i));
		    	System.out.println(viewSet.getImageFileName(i));
				if (!imageFile.exists())
				{
					String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
			    	filenameParts[filenameParts.length - 1] = "png";
			    	String pngFileName = String.join(".", filenameParts);
			    	imageFile = new File(imageDir, pngFileName);
				}
		    	
		    	Texture2D<ContextType> viewTexture;
		    	if (maskDir == null)
		    	{
		    		viewTexture = context.get2DColorTextureBuilder(imageFile, true)
		    						.setLinearFilteringEnabled(true)
		    						.setMipmapsEnabled(true)
		    						.createTexture();
		    	}
		    	else
		    	{
		    		File maskFile = new File(maskDir, viewSet.getImageFileName(i));
					if (!maskFile.exists())
					{
						String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
				    	filenameParts[filenameParts.length - 1] = "png";
				    	String pngFileName = String.join(".", filenameParts);
				    	maskFile = new File(maskDir, pngFileName);
					}
					
		    		viewTexture = context.get2DColorTextureBuilder(imageFile, maskFile, true)
		    						.setLinearFilteringEnabled(true)
		    						.setMipmapsEnabled(true)
		    						.createTexture();
		    	}
		    	
		    	FramebufferObject<ContextType> depthFBO = 
	    			context.getFramebufferObjectBuilder(viewTexture.getWidth(), viewTexture.getHeight())
	    				.addDepthAttachment()
	    				.createFramebufferObject();
		    	
		    	Renderable<ContextType> depthRenderable = context.createRenderable(depthRenderingProgram);
		    	depthRenderable.addVertexBuffer("position", positionBuffer);
		    	
	        	depthRenderingProgram.setUniform("model_view", viewSet.getCameraPose(i));
	    		depthRenderingProgram.setUniform("projection", 
    				viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
	    				.getProjectionMatrix(
    						viewSet.getRecommendedNearPlane(), 
    						viewSet.getRecommendedFarPlane()
						)
				);
	        	
	    		depthFBO.clearDepthBuffer();
	        	depthRenderable.draw(PrimitiveMode.TRIANGLES, depthFBO);
	        	
	        	projTexRenderable.program().setUniform("cameraPose", viewSet.getCameraPose(i));
	        	projTexRenderable.program().setUniform("cameraProjection", 
	        			viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
	        				.getProjectionMatrix(viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
		   
	        	
	        	// Sarin: added "lightPosition" variable to projTexRenderable (projtex_single.frag)
	        	projTexRenderable.program().setUniform( "lightPosition", viewSet.getLightPosition(viewSet.getLightPositionIndex(i)));
	        	
		    	projTexRenderable.program().setTexture("viewImage", viewTexture);
		    	projTexRenderable.program().setTexture("depthImage", depthFBO.getDepthAttachmentTexture());
		    
		    	
		    	//System.out.println( param.getTextureSubdivision()); // size is 1
		    	for (int row = 0; row < param.getTextureSubdivision(); row++)
		    	{
			    	for (int col = 0; col < param.getTextureSubdivision(); col++)
		    		{
			    		projTexRenderable.program().setUniform("minTexCoord", 
			    				new Vector2((float)col / (float)param.getTextureSubdivision(), (float)row / (float)param.getTextureSubdivision()));
			    		
			    		projTexRenderable.program().setUniform("maxTexCoord", 
			    				new Vector2((float)(col+1) / (float)param.getTextureSubdivision(), (float)(row+1) / (float)param.getTextureSubdivision()));
			    		
			    		projTexFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
			    		projTexFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
			    		projTexFBO.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
			    		projTexFBO.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
			    		projTexFBO.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);
			    		projTexFBO.clearColorBuffer(5, 0.0f, 0.0f, 0.0f, 0.0f);
			    		projTexRenderable.draw(PrimitiveMode.TRIANGLES, projTexFBO);
			    		
			    		projTexFBO.saveColorBufferToFile(0, "PNG", new File(viewDir, String.format("r%04dc%04d.png", row, col)));
			    		projTexFBO.saveColorBufferToFile(1, "PNG", new File(viewDir, String.format("output1_r%04dc%04d.png", row, col)));
			    		projTexFBO.saveColorBufferToFile(2, "PNG", new File(viewDir, String.format("output2_r%04dc%04d.png", row, col)));
			    		projTexFBO.saveColorBufferToFile(3, "PNG", new File(viewDir, String.format("output3_r%04dc%04d.png", row, col)));
			    		projTexFBO.saveColorBufferToFile(4, "PNG", new File(viewDir, String.format("predicted_r%04dc%04d.png", row, col)));
			    		projTexFBO.saveColorBufferToFile(5, "PNG", new File(viewDir, String.format("uvwMapping%04dc%04d.png", row, col)));
			    		
			    		// this temp array will get the RGBA colors in an array, where each 4 consecutive arrays represent one pixel
			    		// with R, G, B, A channels.
			    		float[] temp = projTexFBO.readFloatingPointColorBufferRGBA(5);
			    		float[] original = projTexFBO.readFloatingPointColorBufferRGBA(0);
			    		
			    		Random random = new Random();
			    		int totalLength = original.length/4;
			    		int intervals = totalLength / (numberOfPoints);
			    		
			    		//System.out.println( "totalLength: " + totalLength );
			    		
			    		for( int j = 0; j < totalLength; j ++ ){
			    			if( original[j*4+3] == 0.0 ){
			    				continue;
			    			}
			    			Vector3 gColor = new Vector3( original[j*4], original[j*4+1], original[j*4+2]);
			    			//float phong = (float)Math.pow(Math.max(0.0, 1.0 - (temp[j*4]* temp[j*4] +temp[j*4+1]*temp[j*4+1])), 10);
			    			//Vector3 gColor = new Vector3(phong, phong, phong);
			    			Vector3 uvwCoord = new Vector3( temp[j*4], temp[j*4+1], temp[j*4+2] );
			    			UvwBin binKey = new UvwBin(uvwCoord.x, uvwCoord.y, uvwCoord.z);
			    			System.out.println(binKey + " " + uvwCoord.x + ", " + uvwCoord.y + ", " + uvwCoord.z);
			    			if( sumG.containsKey(binKey) ){
			    				Vector3 gValue = sumG.get(binKey);
			    				Vector3 newGValue = new Vector3(gValue.x + gColor.x, gValue.y + gColor.y, gValue.z + gColor.z );
			    				sumG.put(binKey, newGValue);
			    				int count = gCount.get(binKey);
			    				count ++;
			    				gCount.put(binKey, count);
			    				Vector3 uvwValue = sumUvw.get(binKey);
			    				Vector3 newUvwValue = new Vector3(uvwValue.x + uvwCoord.x, uvwValue.y + uvwCoord.y, uvwValue.z + uvwCoord.z);
			    				sumUvw.put(binKey, newUvwValue);
			    			}
			    			else{
			    				sumG.put(binKey, gColor);
			    				gCount.put(binKey, 1);
			    				sumUvw.put(binKey, uvwCoord);
			    				tempCount ++;
			    			}
			    		}
			    		
			    		//System.out.println( "sumG count: " + sumG.size() );
			    		//System.out.println( "gCount count: " + gCount.size() );
			    		
			    		/*
			    		for( int j = 0; j < numberOfPoints; j ++ ){
				    		
			    			// generate random numbers between 1 and 1048576/4=262144
			    			int randomNumber = random.nextInt(original.length/4);
			    			// generate random number until we get the corresponding pixel so that alpha of original image is not 0.
			    			int numTries = 0;
			    			while( original[randomNumber*4+3] == 0.0 && numTries < 10000){ 
			    				randomNumber = random.nextInt(original.length/4);
			    				numTries++;
			    			}
			    			if (numTries >= 10000) {
			    				continue;
			    			}
			    			
			    			p[sampleIndex][0] = 1;
			    			p[sampleIndex][1] = temp[randomNumber*4];
			    			p[sampleIndex][2] = temp[randomNumber*4+1];
			    			p[sampleIndex][3] = temp[randomNumber*4+2];
			    			g[sampleIndex][0] = original[randomNumber*4];
			    			g[sampleIndex][1] = original[randomNumber*4+1];
			    			g[sampleIndex][2] = original[randomNumber*4+2];
			    			sampleIndex++;
			    			
			    		}*/
			    	}
	    		}
		    	
		    	viewTexture.delete();
	        	depthFBO.delete();
		    	
		    	System.out.println("Completed " + (i+1) + "/" + viewSet.getCameraPoseCount());
	    	}
	    	
    		/*g = new double[2000][3];
    		p = new double[2000][4];
	    	int inc = 0;
	    	Set<UvwBin> uvwBinSet = sumG.keySet();
	    	ArrayList<UvwBin> uvwBinList = new ArrayList<UvwBin>(uvwBinSet);
	    	Collections.shuffle(uvwBinList);
	    	for( UvwBin coord : uvwBinList ){
	    		inc++;
	    		if( inc > g.length ){
	    			break;
	    		}
	    		
	    		Vector3 gVector = sumG.get(coord);
	    		g[sampleIndex][0] = gVector.x / gCount.get(coord);
	    		g[sampleIndex][1] = gVector.y / gCount.get(coord);
	    		g[sampleIndex][2] = gVector.z / gCount.get(coord);
	    		Vector3 uvwVector = sumUvw.get(coord);
	    		p[sampleIndex][0] = 1;
	    		p[sampleIndex][1] = uvwVector.x / gCount.get(coord);
	    		p[sampleIndex][2] = uvwVector.y / gCount.get(coord);
	    		p[sampleIndex][3] = uvwVector.z / gCount.get(coord);
	    		sampleIndex ++;

	    	}
	    	
	    	System.out.println("Grabbed " + sampleIndex + " samples");
	    	*/
	    	
	    	
    		// Greedy Algorithm 
    		// - compute radial basis function for each point
    		// - find point with largest error 
    		// - append point to samples/centers
	    	
	    	/*
	    	 * 	Initialize variables
	    	 */
	    	int numberOfSamples = 1000;
    		g = new double[numberOfSamples][3];
    		p = new double[numberOfSamples][4];
    		double[][] psi = null;
    		Matrix Coefficient = null;
    		Vector3[] constants = null; 
    		Vector3[] lambdasConstants = null;
	    	int inc = 0;
	    	UvwBin maxErrorBin = null;
	    	Map<UvwBin, Integer> maxChosen = new HashMap<UvwBin, Integer>();
	    	
	    	Iterator<UvwBin> coordIT = sumG.keySet().iterator();
	    	
	    /*	Map<Float, Integer> sampleDensityTable = new HashMap<Float, Integer>();
	    	sampleDensityTable.put(0f, 4);
	    	sampleDensityTable.put(0.1f, 5);
	    	sampleDensityTable.put(0.2f, 6);
	    	sampleDensityTable.put(0.3f, 8);
	    	sampleDensityTable.put(0.4f, 9);
	    	sampleDensityTable.put(0.5f, 11);
	    	sampleDensityTable.put(0.6f, 12);
	    	sampleDensityTable.put(0.7f, 14);
	    	sampleDensityTable.put(0.8f, 16);*/
	    	int[] sampleDensityTable = {4, 5, 6, 8, 9, 11, 12, 14, 16};
	    	
	    	float SCALE = 1f;
	    	
	    	for(float w = 0f; w <= 0.8f; w += 0.1f ){
	    		int numRs = sampleDensityTable[(int) Math.round(w*10)]; 
	    		int numThetas = sampleDensityTable[(int) Math.round(w*10)]; 
	    		float deltaR = 1.0f / (numRs - 1);
	    		float deltaTheta = (float) (2 * Math.PI) / numThetas;
	    		
	    		// add sample at (0, 0, w)
	    		UvwBin coord = new UvwBin(0f*SCALE, 0f*SCALE, w*SCALE);
	    		Vector3 gVector = sumG.get(coord);
	    		if( gVector != null && !maxChosen.containsKey(coord) ){
		    		g[sampleIndex][0] = gVector.x / gCount.get(coord);
		    		g[sampleIndex][1] = gVector.y / gCount.get(coord);
		    		g[sampleIndex][2] = gVector.z / gCount.get(coord);
		    		Vector3 uvwVector = sumUvw.get(coord);
		    		p[sampleIndex][0] = 1;
		    		p[sampleIndex][1] = uvwVector.x / gCount.get(coord);
		    		p[sampleIndex][2] = uvwVector.y / gCount.get(coord);
		    		p[sampleIndex][3] = uvwVector.z / gCount.get(coord);
		    		sampleIndex ++;
		    		maxChosen.put(coord, 1);
	    		}
	    		for(int k = 1; k < numRs; k ++ ){
	    			float startTheta;
	    			if( (k % 2) == 1){
	    				startTheta = deltaTheta / 2;
	    			}
	    			else{
	    				startTheta = 0f;
	    			}
	    			float r = k * deltaR;
	    			for( float theta = startTheta; theta < ((float) startTheta + 2 * Math.PI); theta += deltaTheta){
	    				float u = (float) (r * Math.cos((double) theta)); 
	    				float v = (float) (r * Math.sin((double) theta));
	    				// compute dot(n, l) > 0
	    				UvwBin newCoord = new UvwBin(u*SCALE, v*SCALE, w*SCALE);
	    	    		Vector3 newVector = sumG.get(newCoord);
	    	    		if( newVector != null && !maxChosen.containsKey(newCoord) ){
		    	    		g[sampleIndex][0] = newVector.x / gCount.get(newCoord);
		    	    		g[sampleIndex][1] = newVector.y / gCount.get(newCoord);
		    	    		g[sampleIndex][2] = newVector.z / gCount.get(newCoord);
		    	    		Vector3 newUvwVector = sumUvw.get(newCoord);
		    	    		p[sampleIndex][0] = 1;
		    	    		p[sampleIndex][1] = newUvwVector.x / gCount.get(newCoord);
		    	    		p[sampleIndex][2] = newUvwVector.y / gCount.get(newCoord);
		    	    		p[sampleIndex][3] = newUvwVector.z / gCount.get(newCoord);
		    	    		sampleIndex ++ ;
		    	    		maxChosen.put(newCoord, 1);
	    	    		}
	    			}
	    		}
	    		
	    	}
	    	
	    	System.out.println("SampleIndex: " + sampleIndex);
	    	
	    	psi = new double[sampleIndex][sampleIndex];
    		
	    	for( int j = 0; j < psi.length; j ++ )
	    	{
	    		for( int k = 0; k < psi.length; k ++ )
	    		{
    				// distance formula 
    				psi[j][k] = Math.sqrt((p[j][1]-p[k][1])*(p[j][1]-p[k][1]) + (p[j][2]-p[k][2])*(p[j][2]-p[k][2])
    								+ (p[j][3]-p[k][3])*(p[j][3]-p[k][3]));
	    		}
	    	}
	    	// create matrix of zero of size Psi.row+PT.row by Psi.col+P.col
    		//Matrix BigMatrix = new Matrix( psi.length + p[0].length, psi[0].length + p[0].length);
	    	// change 1
	    	Matrix BigMatrix = new Matrix( psi.length + 1, psi[0].length + 1 );
    		for( int j = 0; j < psi.length; j ++ ){
    			for( int k = 0; k < psi[0].length; k++ ){
    				BigMatrix.set(j, k, psi[j][k]);
    			}
    		}
    		int a = 0;
    		int b = 0;
    		double[][] pt = new double[4][sampleIndex];
    		//for( int j = psi.length; j < BigMatrix.getRowDimension(); j ++ ){
    		// change 2
    		for( int j = psi.length; j < psi.length + 1; j ++ ){
    			for( int k = 0; k < sampleIndex; k ++ ){
    				// transpose of P
    				BigMatrix.set( j, k, p[b][a]); //PT.get(a, b))
    				b ++;
    			}
    			a ++;
    			b = 0;
    		}
    		
    		// change 3
    		for( int j = 0; j < 4; j ++ ){
    			for( int k = 0; k < sampleIndex; k ++ ){
    				pt[j][k] = p[k][j];
    			}
    		} 
    		
    		b = 0;
    		for( int j = 0; j < sampleIndex; j ++ ){
    			//for( int k = psi[0].length; k < BigMatrix.getColumnDimension(); k ++ ){
    			// change 4
    			for( int k = psi[0].length; k < psi[0].length + 1; k ++ ){
    				BigMatrix.set( j, k, p[j][b]);
    				b ++;
    			}
    			b = 0;
    		}

    		Coefficient = new Matrix( BigMatrix.getRowDimension(), 3 );
    		// create matrix to store G and zeros so that equation is compatible
    		Matrix GZero = new Matrix( BigMatrix.getRowDimension(), 3 );
    		for( int j = 0; j < sampleIndex; j ++ ){
    			for( int k = 0; k < g[0].length; k ++ ){
    				GZero.set(j, k, g[j][k]);
    			}
    		}
    		
    		System.out.println(BigMatrix.getRowDimension());
    		System.out.println(BigMatrix.getColumnDimension());
    		try {
    			Coefficient = BigMatrix.solve(GZero);
    		} catch(RuntimeException e) {
    			//coordIT.remove();
    			System.out.println("Singular matrix cause by " + maxErrorBin);
    			maxChosen.put(maxErrorBin, 1);
    			sampleIndex--;
    			maxErrorBin = null;
    			
    		}
    		System.out.println( "Solved! ");
    		
    		constants = new Vector3[1];
    		lambdasConstants = new Vector3[sampleIndex];
    		Vector3[] thetas = new Vector3[sampleIndex];
    		int m = 0;
    		// write lambda and c to file in the order of left to write and up to down from the Coefficient matrix
    		for( int j = 0; j < psi.length ; j ++ ){
    			double[] temp = new double[3];
    			for ( int k = 0; k < 3; k ++ ){
    				temp[k] = Coefficient.get(j, k);
    			}
    			lambdasConstants[m] = new Vector3((float)temp[0], (float)temp[1], (float)temp[2]); 
    			m ++ ;
    		}
    		m = 0;
    		for( int j = psi.length; j < Coefficient.getRowDimension() ; j ++ ){
    			double[] temp = new double[3];
    			for ( int k = 0; k < 3; k ++ ){
    				temp[k] = Coefficient.get(j, k);
    			}
    			constants[m] = new Vector3((float)temp[0], (float)temp[1], (float)temp[2]);
    			m ++ ;
    		}
    		m = 0;
    		for( int j = 0; j < sampleIndex; j ++ ){
    			double[] temp = new double[3];
    			// k start from 1 because in p matrix, the first column is all 1's. 
    			for( int k = 1; k < p[0].length; k ++ ){
    				temp[k-1] = p[j][k];
    			}
    			thetas[m] = new Vector3((float)temp[0], (float)temp[1], (float)temp[2]);
    			m ++ ;
    		}

    	
    		for( int j = 0; j < psi.length ; j ++ ){
    			for ( int k = 0; k < 3; k ++ ){
    				lambdas.write(Coefficient.get(j, k) + " ");
    				}
    			lambdas.write("\r\n");
    		}
    		for( int j = psi.length; j < Coefficient.getRowDimension() ; j ++ ){
    			for ( int k = 0; k < 3; k ++ ){
    				coefficients.write(Coefficient.get(j, k) + " ");
    			}
    			coefficients.write("\r\n");
    		}
    		for( int j = 0; j < sampleIndex; j ++ ){
    			// k start from 1 because in p matrix, the first column is all 1's. 
    			for( int k = 1; k < p[0].length; k ++ ){
    				uvw.write(p[j][k] + " ");
    			}
    			uvw.write("\r\n");
    		}

    		lambdas.close();
    		coefficients.close();
    		uvw.close();
	    	
	    	
	    	//for( UvwBin coord: uvwBinList ){
	    	/*while(coordIT.hasNext()) {
	    		UvwBin coord = coordIT.next();
	    		
	    		if( sampleIndex >= numberOfSamples ){
	    			break;
	    		}
	    		// get first 20 random points
	    		if( sampleIndex < 2 ){
	    			Vector3 gVector = sumG.get(coord);
		    		g[sampleIndex][0] = gVector.x / gCount.get(coord);
		    		g[sampleIndex][1] = gVector.y / gCount.get(coord);
		    		g[sampleIndex][2] = gVector.z / gCount.get(coord);
		    		Vector3 uvwVector = sumUvw.get(coord);
		    		p[sampleIndex][0] = 1;
		    		p[sampleIndex][1] = uvwVector.x / gCount.get(coord);
		    		p[sampleIndex][2] = uvwVector.y / gCount.get(coord);
		    		p[sampleIndex][3] = uvwVector.z / gCount.get(coord);
	    			sampleIndex++;
	    			maxChosen.put(coord, 1);
	    			continue;
	    		}
	    		// if have maximum bin that does not fit the RBF follow, include as sample point
	    		if( maxErrorBin != null ){
	    			//System.out.println( "Max Error Bin: " + maxErrorBin.u + " " + maxErrorBin.v + " " + maxErrorBin.w );
	    			Vector3 gVector = sumG.get(maxErrorBin);
		    		g[sampleIndex][0] = gVector.x / gCount.get(maxErrorBin);
		    		g[sampleIndex][1] = gVector.y / gCount.get(maxErrorBin);
		    		g[sampleIndex][2] = gVector.z / gCount.get(maxErrorBin);
		    		Vector3 uvwVector = sumUvw.get(maxErrorBin);
		    		p[sampleIndex][0] = 1;
		    		p[sampleIndex][1] = uvwVector.x / gCount.get(maxErrorBin);
		    		p[sampleIndex][2] = uvwVector.y / gCount.get(maxErrorBin);
		    		p[sampleIndex][3] = uvwVector.z / gCount.get(maxErrorBin);
		    		System.out.println("Adding max error bin: " + maxErrorBin + "(" + p[sampleIndex][1] + ", " + p[sampleIndex][2] + ", " + p[sampleIndex][3] + ")");
	    			sampleIndex++;
	    		}*/
	    		/*
	    		 *  Compute coefficients for this matrix of this sample size
	    		 */
	    		/*psi = new double[sampleIndex][sampleIndex];
	    		
		    	for( int j = 0; j < psi.length; j ++ )
		    	{
		    		for( int k = 0; k < psi.length; k ++ )
		    		{
	    				// distance formula 
	    				psi[j][k] = Math.sqrt((p[j][1]-p[k][1])*(p[j][1]-p[k][1]) + (p[j][2]-p[k][2])*(p[j][2]-p[k][2])
	    								+ (p[j][3]-p[k][3])*(p[j][3]-p[k][3]));
		    		}
		    	}
		    	// create matrix of zero of size Psi.row+PT.row by Psi.col+P.col
	    		//Matrix BigMatrix = new Matrix( psi.length + p[0].length, psi[0].length + p[0].length);
		    	// change 1
		    	Matrix BigMatrix = new Matrix( psi.length + 1, psi[0].length + 1 );
	    		for( int j = 0; j < psi.length; j ++ ){
	    			for( int k = 0; k < psi[0].length; k++ ){
	    				BigMatrix.set(j, k, psi[j][k]);
	    			}
	    		}
	    		int a = 0;
	    		int b = 0;
	    		double[][] pt = new double[4][sampleIndex];
	    		//for( int j = psi.length; j < BigMatrix.getRowDimension(); j ++ ){
	    		// change 2
	    		for( int j = psi.length; j < psi.length + 1; j ++ ){
	    			for( int k = 0; k < sampleIndex; k ++ ){
	    				// transpose of P
	    				BigMatrix.set( j, k, p[b][a]); //PT.get(a, b))
	    				b ++;
	    			}
	    			a ++;
	    			b = 0;
	    		}
	    		*/
	    		// change 3
	    	/*	for( int j = 0; j < 4; j ++ ){
	    			for( int k = 0; k < sampleIndex; k ++ ){
	    				pt[j][k] = p[k][j];
	    			}
	    		} */
	    		/*
	    		b = 0;
	    		for( int j = 0; j < sampleIndex; j ++ ){
	    			//for( int k = psi[0].length; k < BigMatrix.getColumnDimension(); k ++ ){
	    			// change 4
	    			for( int k = psi[0].length; k < psi[0].length + 1; k ++ ){
	    				BigMatrix.set( j, k, p[j][b]);
	    				b ++;
	    			}
	    			b = 0;
	    		}
	
	    		//System.out.println("Rank of Psi: " + new Matrix(psi).rank());
	    		//System.out.println("Row of Pt: " + new Matrix(pt).getRowDimension());
	    		//System.out.println("Rank of Pt: " + new Matrix(pt).rank());
	    		//System.out.println("Rank: " + BigMatrix.rank());
	    		//System.out.println("Condition: " + BigMatrix.cond());
	    		// create matrix to store the coefficients that is being solved 
	    		Coefficient = new Matrix( BigMatrix.getRowDimension(), 3 );
	    		// create matrix to store G and zeros so that equation is compatible
	    		Matrix GZero = new Matrix( BigMatrix.getRowDimension(), 3 );
	    		for( int j = 0; j < sampleIndex; j ++ ){
	    			for( int k = 0; k < g[0].length; k ++ ){
	    				GZero.set(j, k, g[j][k]);
	    			}
	    		}
	    		
	    		//SingularValueDecomposition BigSVD = BigMatrix.svd();
	    		//Coefficient = BigSVD.getV().times(BigSVD.getS().inverse()).times(BigSVD.getU().transpose()).times(GZero);
	    		System.out.println(BigMatrix.getRowDimension());
	    		System.out.println(BigMatrix.getColumnDimension());
	    		//System.out.println(BigMatrix.rank());
	    		try {
	    			Coefficient = BigMatrix.solve(GZero);
	    		} catch(RuntimeException e) {
	    			//coordIT.remove();
	    			System.out.println("Singular matrix cause by " + maxErrorBin);
	    			maxChosen.put(maxErrorBin, 1);
	    			sampleIndex--;
	    			maxErrorBin = null;
	    			continue;
	    		}
	    		System.out.println( "Solved! ");
	    		//Matrix solver = BigMatrix.times(Coefficient);
	    	
	    		//constants = new Vector3[4];
	    		// change 5
	    		constants = new Vector3[1];
	    		lambdasConstants = new Vector3[sampleIndex];
	    		Vector3[] thetas = new Vector3[sampleIndex];
	    		int m = 0;
	    		// write lambda and c to file in the order of left to write and up to down from the Coefficient matrix
	    		for( int j = 0; j < psi.length ; j ++ ){
	    			double[] temp = new double[3];
	    			for ( int k = 0; k < 3; k ++ ){
	    				temp[k] = Coefficient.get(j, k);
	    			}
	    			lambdasConstants[m] = new Vector3((float)temp[0], (float)temp[1], (float)temp[2]); 
	    			m ++ ;
	    		}
	    		m = 0;
	    		for( int j = psi.length; j < Coefficient.getRowDimension() ; j ++ ){
	    			double[] temp = new double[3];
	    			for ( int k = 0; k < 3; k ++ ){
	    				temp[k] = Coefficient.get(j, k);
	    			}
	    			constants[m] = new Vector3((float)temp[0], (float)temp[1], (float)temp[2]);
	    			m ++ ;
	    		}
	    		m = 0;
	    		for( int j = 0; j < sampleIndex; j ++ ){
	    			double[] temp = new double[3];
	    			// k start from 1 because in p matrix, the first column is all 1's. 
	    			for( int k = 1; k < p[0].length; k ++ ){
	    				temp[k-1] = p[j][k];
	    			}
	    			thetas[m] = new Vector3((float)temp[0], (float)temp[1], (float)temp[2]);
	    			m ++ ;
	    		}

	    		// this variable stores the UvwBin with maximum error in RBF formula
	    		maxErrorBin = null;
	    		float maxError = 0;
	    		// this loop computes RBF at each UvwBin 
	    		for( UvwBin bin: sumG.keySet()){
	    			if (!maxChosen.containsKey(bin)) {
	    			
		    			if( maxErrorBin == null ){
		    				System.out.println( "Got in here!");
		    				
		    				maxErrorBin = bin;
		    			}
		    			Vector3 pCoord = sumUvw.get( bin ).dividedBy(gCount.get(bin));
		    			*/
		    			// change 6
		    			/*Vector3 uPart = constants[1].times(pCoord.x);
		    			Vector3 vPart = constants[2].times(pCoord.y);
		    			Vector3 zPart = constants[3].times(pCoord.z);
			    		Vector3 firstPart = constants[0].plus(uPart).plus(vPart).plus(zPart);*/
		    			/*Vector3 firstPart = constants[0];
		    			Vector3 sum = firstPart;
			    		for( int n = 0; n < sampleIndex; n++ ){
			    			Vector3 newTheta = new Vector3(thetas[n].x, thetas[n].y, thetas[n].z );
			    			Vector3 secondPart = lambdasConstants[n].times(pCoord.minus(newTheta).length());
			    			sum = sum.plus(secondPart);
		    			}
			    		Vector3 gVector = sumG.get(bin);
			    		gVector = gVector.dividedBy(gCount.get(bin));
			    		// TODO: may need more than one point from the same bin: because before I had check to see if bin had 
			    		// already been chosen, were getting duplicate bins. That means that even when that bin is added to the samples, 
			    		// it's value still has the maximum error in RBF formula. 
			    		if( sum.minus(gVector).length() > maxError ){
			    			maxErrorBin = bin;
			    			//maxError =  Math.abs( sum.length() - gVector.length());
			    			maxError = sum.minus(gVector).length();
			    		}
	    			}
	    		}
	    		maxChosen.put(maxErrorBin, 1);
	    		System.out.println("Highest Error Bin: " + maxErrorBin);
	    		System.out.println("Max Error: " + maxError );
	    	}*/
	    	
	    	/*// write lambda and c to file in the order of left to write and up to down from the Coefficient matrix
    		for( int j = 0; j < psi.length ; j ++ ){
    			for ( int k = 0; k < 3; k ++ ){
    				lambdas.write(Coefficient.get(j, k) + " ");
    				}
    			lambdas.write("\r\n");
    		}
    		for( int j = psi.length; j < Coefficient.getRowDimension() ; j ++ ){
    			for ( int k = 0; k < 3; k ++ ){
    				coefficients.write(Coefficient.get(j, k) + " ");
    			}
    			coefficients.write("\r\n");
    		}
    		Vector3[] thetas = new Vector3[sampleIndex];
    		for( int j = 0; j < sampleIndex; j ++ ){
    			// k start from 1 because in p matrix, the first column is all 1's. 
    			for( int k = 1; k < p[0].length; k ++ ){
    				uvw.write(p[j][k] + " ");
    			}
    			uvw.write("\r\n");
    		}

    		lambdas.close();
    		coefficients.close();
    		uvw.close();*/
    		
	    	System.out.println("Pre-projections completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	}

		projTexProgram.delete();
		diffuseFitProgram.delete();
		specularFitProgram.delete();
		diffuseDebugProgram.delete();
		specularDebugProgram.delete();
    	depthRenderingProgram.delete();
	}
}
