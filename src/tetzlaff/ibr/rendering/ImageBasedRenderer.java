package tetzlaff.ibr.rendering;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import tetzlaff.gl.AlphaBlendingFunction;
import tetzlaff.gl.AlphaBlendingFunction.Weight;
import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.ColorFormat.DataType;
import tetzlaff.gl.Context;
import tetzlaff.gl.Cubemap;
import tetzlaff.gl.CubemapFace;
import tetzlaff.gl.Drawable;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.TextureWrapMode;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.nativelist.NativeFloatVectorList;
import tetzlaff.gl.nativelist.NativeIntVectorList;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Matrix3;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibr.IBRLoadingMonitor;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.IBRSettings;
import tetzlaff.ibr.ViewSet;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.ReadonlyLightModel;
import tetzlaff.util.EnvironmentMap;

public class ImageBasedRenderer<ContextType extends Context<ContextType>> implements IBRRenderable<ContextType>
{
	private ContextType context;
	private Program<ContextType> program;
	private Program<ContextType> shadowProgram;
	private ReadonlyLightModel lightModel;
	private IBRLoadingMonitor callback;
	private boolean suppressErrors = false;
	private IBRSettings settings;

	private IBRResources.Builder<ContextType> resourceBuilder;
	private IBRResources<ContextType> resources;
	
	private Texture3D<ContextType> shadowMaps;
	private FramebufferObject<ContextType> shadowFramebuffer;
	private Drawable<ContextType> shadowDrawable;

	private Program<ContextType> lightProgram;
	private VertexBuffer<ContextType> lightVertices;
	private Texture2D<ContextType> lightTexture;
	private Drawable<ContextType> lightDrawable;
	
    private boolean btfRequested;
    private int btfWidth, btfHeight;
    private File btfExportPath;
    
    private String id;
    private Drawable<ContextType> mainDrawable;
    private ReadonlyCameraModel cameraModel;

    private Vector3 clearColor;
    private boolean halfResEnabled;
    private Program<ContextType> simpleTexProgram;
    private Drawable<ContextType> simpleTexDrawable;
	private FramebufferObject<ContextType> offscreenFBO = null;
    
    private File newEnvironmentFile = null;
    private boolean environmentMapEnabled;
    private Cubemap<ContextType> environmentMap;
    private Matrix4 envMapMatrix = null;

    private Program<ContextType> environmentBackgroundProgram;
    private Drawable<ContextType> environmentBackgroundDrawable;

    private boolean resampleRequested;
    private int resampleWidth, resampleHeight;
    private File resampleVSETFile;
    private File resampleExportPath;
    
	private Consumer<Matrix4> resampleSetupCallback;
	private Runnable resampleCompleteCallback;

	private boolean fidelityRequested;
    private File fidelityExportPath;
    private File fidelityVSETFile;
    
	private Consumer<Integer> fidelitySetupCallback;
	private Runnable fidelityCompleteCallback;
    
    private boolean multisamplingEnabled = false;
    
    private List<Matrix4> transformationMatrices;
    private Vector3 centroid;
    private float boundingRadius;
    
    private VertexGeometry referenceScene = null;
    private boolean referenceSceneChanged = false;
    private VertexBuffer<ContextType> refScenePositions = null;
    private VertexBuffer<ContextType> refSceneTexCoords = null;
    private VertexBuffer<ContextType> refSceneNormals = null;
    private Texture2D<ContextType> refSceneTexture = null;
	
	ImageBasedRenderer(String id, ContextType context, Program<ContextType> program, 
			ReadonlyCameraModel cameraModel, ReadonlyLightModel lightModel,
			IBRResources.Builder<ContextType> resourceBuilder)
    {
    	this.id = id;
		this.context = context;
		this.program = program;
    	this.resourceBuilder = resourceBuilder;
    	this.cameraModel = cameraModel;
    	this.lightModel = lightModel;
    	
    	this.clearColor = new Vector3(0.0f);
    	this.transformationMatrices = new ArrayList<Matrix4>();
    	this.transformationMatrices.add(Matrix4.identity());
    	this.settings = new IBRSettings();
    }

	@Override
	public void initialize() 
	{
		if (this.program == null)
    	{
	    	try
	        {
	    		this.program = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/relight/relight.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}

    	if (this.simpleTexProgram == null)
    	{
	    	try
	        {
	    		this.simpleTexProgram = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/common/texture.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}
		
    	if (this.environmentBackgroundProgram == null)
    	{
	    	try
	        {
	    		this.environmentBackgroundProgram = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/common/envbackgroundtexture.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}
    	
    	try 
    	{
    		this.resources = resourceBuilder.create();
    		
	    	this.mainDrawable = context.createDrawable(program);
	    	this.mainDrawable.addVertexBuffer("position", this.resources.positionBuffer);
	    	
	    	if (this.resources.normalBuffer != null)
	    	{
	    		this.mainDrawable.addVertexBuffer("normal", this.resources.normalBuffer);
	    	}
	    	
	    	if (this.resources.texCoordBuffer != null)
	    	{
	    		this.mainDrawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
	    	}
	    	
	    	if (this.resources.tangentBuffer != null)
	    	{
	    		this.mainDrawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
	    	}
	    				
	    	this.simpleTexDrawable = context.createDrawable(simpleTexProgram);
	    	this.simpleTexDrawable.addVertexBuffer("position", context.createRectangle());
			
	    	this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
	    	this.environmentBackgroundDrawable.addVertexBuffer("position", context.createRectangle());
	    	
			context.flush();

			if (this.callback != null)
			{
				this.callback.setMaximum(0.0); // make indeterminate
			}
		} 
    	catch (IOException e) 
    	{
			e.printStackTrace();
		}
    	
    	// New code begins:
	
		if (this.shadowProgram == null)
		{
	        try
	        {
	    		shadowProgram = context.getShaderProgramBuilder()
		    			.addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
		    			.addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
	    				.createProgram();
	    		
	    		shadowDrawable = context.createDrawable(shadowProgram);
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        	throw new IllegalStateException("The shader program could not be initialized.", e);
	        }
		}
		
		if (this.lightProgram == null)
    	{
	    	try
	        {
	    		this.lightProgram = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/relight/light.frag"))
	    				.createProgram();
	    		this.lightVertices = context.createRectangle();
	    		this.lightDrawable = context.createDrawable(this.lightProgram);
	    		this.lightDrawable.addVertexBuffer("position", lightVertices);
	    		
	    		NativeFloatVectorList lightTextureData = new NativeFloatVectorList(1, 4096);
	    		
	    		int k = 0;
	    		for (int i = 0; i < 64; i++)
	    		{
    				double x = i * 2.0 / 63.0 - 1.0;
	    			
	    			for (int j = 0; j < 64; j++)
	    			{
	    				double y = j * 2.0 / 63.0 - 1.0;
	    				
	    				double rSq = x*x + y*y;
	    				lightTextureData.set(k, 0, (float)(Math.cos(Math.min(Math.sqrt(rSq), 1.0) * Math.PI) + 1.0) * 0.5f);
	    				k++;
	    			}
	    		}
	    		
	    		this.lightTexture = context.get2DColorTextureBuilder(64, 64, lightTextureData)
    					.setInternalFormat(ColorFormat.R8)
    					.setLinearFilteringEnabled(true)
    					.setMipmapsEnabled(true)
    					.createTexture();
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}
		
		// TODO do these callbacks even need to be variables or could they be private methods?
		this.resampleSetupCallback = (modelView) ->
		{
			setupForRelighting();
			
			if (lightModel instanceof CameraBasedLightModel)
			{
		    	float scale = new Vector3(resources.viewSet.getCameraPose(0)
		    			.times(new Vector4(resources.geometry.getCentroid(), 1.0f))).length();
				
				((CameraBasedLightModel)lightModel).overrideCameraPose(
						Matrix4.scale(1.0f / scale)
							.times(modelView)
							.times(Matrix4.translate(resources.geometry.getCentroid()))
			    			.times(new Matrix4(new Matrix3(resources.viewSet.getCameraPose(0).transpose())))
							.times(Matrix4.scale(scale)));
			}
			
			for (int i = 0; i < lightModel.getLightCount(); i++)
			{
				generateShadowMaps(i);
				setupLight(i, 0);
			}
		};
		
		this.resampleCompleteCallback = () -> 
		{
			if (lightModel instanceof CameraBasedLightModel)
			{
				((CameraBasedLightModel)lightModel).removeCameraPoseOverride();
			}
		};
		
		this.fidelitySetupCallback = (index) ->
		{
			setupForRelighting();
			
			ViewSet vset = resources.viewSet;
			Vector3 lightIntensity = vset.getLightIntensity(vset.getLightIndex(index));
			Vector4 lightPos = vset.getCameraPose(index).quickInverse(0.002f).times(new Vector4(vset.getLightPosition(vset.getLightIndex(index)), 1.0f));
			
			setupLightForFidelity(lightIntensity, new Vector3(lightPos));
		};
		
		shadowDrawable.addVertexBuffer("position", resources.positionBuffer);

		shadowMaps = context.get2DDepthTextureArrayBuilder(2048, 2048, lightModel.getLightCount()).createTexture();
		shadowFramebuffer = context.getFramebufferObjectBuilder(2048, 2048)
			.addDepthAttachment()
			.createFramebufferObject();
		
		this.updateCentroidAndRadius();
		
		// Make sure that everything is loaded onto the graphics card before announcing that loading is complete.
		this.draw();
		
		if (this.callback != null)
		{
			this.callback.loadingComplete();
		}
	}

	@Override
	public void update() 
	{
    	if (this.btfRequested)
    	{
    		try
    		{
				this.exportBTF();
			} 
    		catch (Exception e) 
    		{
				e.printStackTrace();
			}
    		this.btfRequested = false;
    		
    		if (callback != null)
    		{
    			callback.loadingComplete();
    		}
    	}
    	else
    	{
    		this.updateCentroidAndRadius();
    		
    		if (this.newEnvironmentFile != null)
    		{
    			File environmentFile = this.newEnvironmentFile;
    			this.newEnvironmentFile = null;
    			
    			try
    			{
    				System.out.println("Loading new environment texture.");
    				
//    				ColorCubemapBuilderFromHDRFile<ContextType, ? extends Cubemap<ContextType>> cubemapBuilder;
//    				
//    				if(environmentFile.getName().endsWith("_zvc.hdr"))
//    				{
//    					// Use Michael Ludwig's code to convert the cross from a cube map to a panorama
//    					EnvironmentMap envMap = EnvironmentMap.createFromHDRFile(environmentFile);
//    					float[] pixels = EnvironmentMap.toPanorama(envMap.getData(), envMap.getSide(), envMap.getSide() * 4, envMap.getSide() * 2);
//    					FloatVertexList pixelList = new FloatVertexList(3, pixels.length / 3, pixels);
//    					textureBuilder = context.get2DColorTextureBuilder(envMap.getSide() * 4, envMap.getSide() * 2, pixelList);
//    					textureBuilder.setInternalFormat(ColorFormat.RGB32F);
//    					
//    					// Uncomment to save the panorama as an image (i.e. for a figure in a paper)
////    					BufferedImage img = new BufferedImage(envMap.getSide() * 4, envMap.getSide() * 2, BufferedImage.TYPE_3BYTE_BGR);
////    					int k = 0;
////    					
////    					for (int j = 0; j < envMap.getSide() * 2; j++)
////    					{
////    						for (int i = 0; i < envMap.getSide() * 4; i++)
////    						{
////    							img.setRGB(i,  j, ((int)(Math.pow(pixels[3 * k + 0], 1.0 / 2.2) * 255) << 16)
////    									| ((int)(Math.pow(pixels[3 * k + 1], 1.0 / 2.2) * 255) << 8) 
////    									| (int)(Math.pow(pixels[3 * k + 2], 1.0 / 2.2) * 255));
////    							k++;
////    						}
////    					}
////    					ImageIO.write(img, "PNG", new File(environmentFile.getParentFile(), environmentFile.getName().replace("_zvc.hdr", "_pan.hdr")));
//    				}
//    				else
//    				{
//    					// Load the panorama directly
//    					textureBuilder = context.get2DColorTextureBuilder(environmentFile, true);
//    					if (environmentFile.getName().endsWith(".hdr"))
//    					{
//    						textureBuilder.setInternalFormat(ColorFormat.RGB32F);
//    					}
//    					else
//    					{
//    						textureBuilder.setInternalFormat(ColorFormat.RGB8);
//    					}
//    				}
//    				
//    				Texture2D<ContextType> newEnvironmentTexture = 
//    					textureBuilder
//    						.setMipmapsEnabled(true)
//    						.setLinearFilteringEnabled(true)
//    						.createTexture();
    				
    				// Use Michael Ludwig's code to convert to a cube map (supports either cross or panorama input)
					EnvironmentMap envMap = EnvironmentMap.createFromHDRFile(environmentFile);
					float[][] sides = envMap.getData();
					
//					// Uncomment to save the panorama as an image (i.e. for a figure in a paper)
//					float[] pixels = EnvironmentMap.toPanorama(envMap.getData(), envMap.getSide(), envMap.getSide() * 4, envMap.getSide() * 2);
//					BufferedImage img = new BufferedImage(envMap.getSide() * 4, envMap.getSide() * 2, BufferedImage.TYPE_3BYTE_BGR);
//					int k = 0;
//					
//					for (int j = 0; j < envMap.getSide() * 2; j++)
//					{
//						for (int i = 0; i < envMap.getSide() * 4; i++)
//						{
//							img.setRGB(i,  j, ((int)(Math.pow(pixels[3 * k + 0], 1.0 / 2.2) * 255) << 16)
//									| ((int)(Math.pow(pixels[3 * k + 1], 1.0 / 2.2) * 255) << 8) 
//									| (int)(Math.pow(pixels[3 * k + 2], 1.0 / 2.2) * 255));
//							k++;
//						}
//					}
//					ImageIO.write(img, "PNG", new File(environmentFile.getParentFile(), environmentFile.getName().replace("_zvc.hdr", "_IBRelight_pan.hdr")));
    				
    				Cubemap<ContextType> newEnvironmentTexture = 
    						context.getColorCubemapBuilder(envMap.getSide())
    							.setInternalFormat(ColorFormat.RGB32F)
    							.loadFace(CubemapFace.PositiveX, new NativeFloatVectorList(3, sides[EnvironmentMap.PX].length / 3, sides[EnvironmentMap.PX]))
    							.loadFace(CubemapFace.NegativeX, new NativeFloatVectorList(3, sides[EnvironmentMap.NX].length / 3, sides[EnvironmentMap.NX]))
    							.loadFace(CubemapFace.PositiveY, new NativeFloatVectorList(3, sides[EnvironmentMap.PY].length / 3, sides[EnvironmentMap.PY]))
    							.loadFace(CubemapFace.NegativeY, new NativeFloatVectorList(3, sides[EnvironmentMap.NY].length / 3, sides[EnvironmentMap.NY]))
    							.loadFace(CubemapFace.PositiveZ, new NativeFloatVectorList(3, sides[EnvironmentMap.PZ].length / 3, sides[EnvironmentMap.PZ]))
    							.loadFace(CubemapFace.NegativeZ, new NativeFloatVectorList(3, sides[EnvironmentMap.NZ].length / 3, sides[EnvironmentMap.NZ]))
								.setMipmapsEnabled(true)
								.setLinearFilteringEnabled(true)
								.createTexture();
					
    				newEnvironmentTexture.setTextureWrap(TextureWrapMode.Repeat, TextureWrapMode.None);
    	
    				if (this.environmentMap != null)
    				{
    					this.environmentMap.close();
    				}
    				
    				this.environmentMap = newEnvironmentTexture;
    			}
    			catch (Exception e) 
        		{
    				e.printStackTrace();
    			}
    		}
    		
    		if (this.referenceSceneChanged && this.referenceScene != null)
    		{
    			this.referenceSceneChanged = false;
    			
    			try
    			{
    				System.out.println("Using new reference scene.");
    				
    				if (this.refScenePositions != null)
    				{
    					this.refScenePositions.close();
    					this.refScenePositions = null;
    				}
    				
    				if (this.refSceneTexCoords != null)
    				{
    					this.refSceneTexCoords.close();
    					this.refSceneTexCoords = null;
    				}
    				
    				if (this.refSceneNormals != null)
    				{
    					this.refSceneNormals.close();
    					this.refSceneNormals = null;
    				}
    				
    				if (this.refSceneTexture != null)
    				{
    					this.refSceneTexture.close();
    					this.refSceneTexture = null;
    				}
    				
    				this.refScenePositions = context.createVertexBuffer().setData(referenceScene.getVertices());
    				this.refSceneTexCoords = context.createVertexBuffer().setData(referenceScene.getTexCoords());
    				this.refSceneNormals = context.createVertexBuffer().setData(referenceScene.getNormals());
    				this.refSceneTexture = context.get2DColorTextureBuilder(
    						new File(referenceScene.getFilename().getParentFile(), referenceScene.getMaterial().getDiffuseMap().getMapName()), true)
    					.setMipmapsEnabled(true)
    					.setLinearFilteringEnabled(true)
    					.createTexture();
    			}
    			catch (Exception e)
    			{
    				e.printStackTrace();
    			}
    		}
    		
        	if (this.resampleRequested)
        	{
        		try
        		{
    				this.resample();
    			} 
        		catch (Exception e) 
        		{
    				e.printStackTrace();
    			}
        		this.resampleRequested = false;
        		if (this.callback != null)
    			{
    				this.callback.loadingComplete();
    			}
        	}
        	else if (this.fidelityRequested)
        	{
        		if (this.callback != null)
    			{
    				this.callback.startLoading();
    			}
        		
        		try
        		{
    				this.executeFidelityComputation();
    			} 
        		catch (Exception e) 
        		{
    				e.printStackTrace();
    			}
        		
        		this.fidelityRequested = false;
        		
        		if (this.callback != null)
    			{
    				this.callback.loadingComplete();
    			}
        	}
    	}
	}
	
	private void setupForDraw()
	{
		this.setupForDraw(this.mainDrawable.program());
	}
	
	private void setupForDraw(Program<ContextType> program)
	{
		program.setTexture("viewImages", resources.colorTextures);
		program.setUniformBuffer("CameraWeights", resources.cameraWeightBuffer);
		program.setUniformBuffer("CameraPoses", resources.cameraPoseBuffer);
		program.setUniformBuffer("CameraProjections", resources.cameraProjectionBuffer);
		program.setUniformBuffer("CameraProjectionIndices", resources.cameraProjectionIndexBuffer);
    	if (resources.lightPositionBuffer != null && resources.lightIntensityBuffer != null && resources.lightIndexBuffer != null)
    	{
    		program.setUniformBuffer("LightPositions", resources.lightPositionBuffer);
    		program.setUniformBuffer("LightIntensities", resources.lightIntensityBuffer);
    		program.setUniformBuffer("LightIndices", resources.lightIndexBuffer);
    	}
    	program.setUniform("viewCount", resources.viewSet.getCameraPoseCount());
    	program.setUniform("infiniteLightSources", true);  /* TODO figure out why I did this.  It should get overwritten in setupForRelighting */
    	if (resources.depthTextures != null)
		{
    		program.setTexture("depthImages", resources.depthTextures);
		}
    	
    	program.setUniform("gamma", this.settings.getGamma());
    	program.setUniform("weightExponent", this.settings.getWeightExponent());
    	program.setUniform("isotropyFactor", this.settings.getIsotropyFactor());
    	program.setUniform("occlusionEnabled", this.resources.depthTextures != null && this.settings.isOcclusionEnabled());
    	program.setUniform("occlusionBias", this.settings.getOcclusionBias());
    	program.setUniform("imageBasedRenderingEnabled", this.settings.isIBREnabled());
    	program.setUniform("relightingEnabled", this.settings.isRelightingEnabled());
    	program.setUniform("pbrGeometricAttenuationEnabled", this.settings.isPBRGeometricAttenuationEnabled());
    	program.setUniform("fresnelEnabled", this.settings.isFresnelEnabled());
    	program.setUniform("shadowsEnabled", this.settings.areShadowsEnabled());
    	
    	program.setTexture("luminanceMap", this.resources.luminanceMap);
	}
	
	
	private void setupForRelighting()
	{
		this.setupForRelighting(this.program);
	}
	
	private void setupForRelighting(Program<ContextType> p)
	{
		if (resources.normalTexture == null)
		{
			p.setUniform("useNormalTexture", false);
			p.setTexture("normalMap", null);
		}
		else
		{
			p.setUniform("useNormalTexture", this.settings().areTexturesEnabled());
			p.setTexture("normalMap", resources.normalTexture);
		}
		
		if (resources.diffuseTexture == null)
		{
			p.setUniform("useDiffuseTexture", false);
			p.setTexture("diffuseMap", null);
		}
		else
		{
			p.setUniform("useDiffuseTexture", this.settings().areTexturesEnabled());
			p.setTexture("diffuseMap", resources.diffuseTexture);
		}
		
		if (resources.specularTexture == null)
		{
			p.setUniform("useSpecularTexture", false);
			p.setTexture("specularMap", null);
		}
		else
		{
			p.setUniform("useSpecularTexture", this.settings().areTexturesEnabled());
			p.setTexture("specularMap", resources.specularTexture);
		}
		
		if (resources.roughnessTexture == null)
		{
			p.setUniform("useRoughnessTexture", false);
			p.setTexture("roughnessMap", null);
		}
		else
		{
			p.setUniform("useRoughnessTexture", this.settings().areTexturesEnabled());
			p.setTexture("roughnessMap", resources.roughnessTexture);
		}
		
		if (this.environmentMap == null || !lightModel.getEnvironmentMappingEnabled())
		{
			p.setUniform("useEnvironmentTexture", false);
			p.setTexture("environmentMap", null);
			this.environmentMapEnabled = false;
		}
		else
		{
			p.setUniform("useEnvironmentMap", true);
			p.setTexture("environmentMap", this.environmentMap);
			p.setUniform("environmentMipMapLevel", Math.max(0, Math.min(this.environmentMap.getMipmapLevelCount() - 2, 
					(int)Math.ceil(0.5 * 
						Math.log(6 * (double)this.environmentMap.getFaceSize() * 
								(double)this.environmentMap.getFaceSize() / (double)resources.viewSet.getCameraPoseCount() ) 
							/ Math.log(2.0)))));
			p.setUniform("diffuseEnvironmentMipMapLevel", this.environmentMap.getMipmapLevelCount() - 1);
			this.environmentMapEnabled = true;
		}
		
		if (resources.luminanceMap == null)
		{
			p.setUniform("useLuminanceMap", false);
			p.setTexture("luminanceMap", null);
		}
		else
		{
			p.setUniform("useLuminanceMap", true);
			p.setTexture("luminanceMap", resources.luminanceMap);
		}
		
		if (resources.inverseLuminanceMap == null)
		{
			p.setUniform("useInverseLuminanceMap", false);
			p.setTexture("inverseLuminanceMap", null);
		}
		else
		{
			p.setUniform("useInverseLuminanceMap", true);
			p.setTexture("inverseLuminanceMap", resources.inverseLuminanceMap);
		}
		
		float gamma = 2.2f;
		p.setUniform("ambientColor", lightModel.getAmbientLightColor());
    	
    	this.clearColor = new Vector3(
    			(float)Math.pow(lightModel.getAmbientLightColor().x, 1.0 / gamma),
    			(float)Math.pow(lightModel.getAmbientLightColor().y, 1.0 / gamma),
    			(float)Math.pow(lightModel.getAmbientLightColor().z, 1.0 / gamma));
    	
		p.setUniform("infiniteLightSources", resources.viewSet.areLightSourcesInfinite());
		p.setTexture("shadowMaps", shadowMaps);
		
		if (resources.shadowMatrixBuffer == null || resources.shadowTextures == null)
		{
			p.setUniform("shadowTestingEnabled", false);
		}
		else
		{
			p.setUniform("shadowTestingEnabled", true);
			p.setUniformBuffer("ShadowMatrices", resources.shadowMatrixBuffer);
			p.setTexture("shadowImages", resources.shadowTextures);
		}
	}
	
	private void updateCentroidAndRadius()
	{
		Vector4 sumPositions = new Vector4(0.0f);
    	this.boundingRadius = resources.geometry.getBoundingRadius();
    	
    	
    	this.centroid = resources.geometry.getCentroid();
    	
    	if (transformationMatrices != null)
    	{
    		for (Matrix4 m : transformationMatrices)
    		{
    			Vector4 position = m.times(new Vector4(resources.geometry.getCentroid(), 1.0f));
    			sumPositions = sumPositions.plus(position);
    		}
    		
    		this.centroid = new Vector3(sumPositions).dividedBy(sumPositions.w);
    		
    		for(Matrix4 m : transformationMatrices)
    		{
    			float distance = new Vector3(m.times(new Vector4(resources.geometry.getCentroid(), 1.0f))).distance(this.centroid);
    			this.boundingRadius = Math.max(this.boundingRadius, distance + resources.geometry.getBoundingRadius());
    		}
    	}
	}
	
	private float getScale()
	{
		 return new Vector3(resources.viewSet.getCameraPose(0)
				 .times(new Vector4(resources.geometry.getCentroid(), 1.0f))).length()
			 * this.boundingRadius / this.resources.geometry.getBoundingRadius();
	}
	
	private Matrix4 getLightMatrix(int lightIndex)
	{
		float scale = getScale();
		return Matrix4.scale(scale)
			.times(lightModel.getLightMatrix(lightIndex))
			.times(Matrix4.scale(1.0f / scale))
			.times(new Matrix4(new Matrix3(resources.viewSet.getCameraPose(0))))
			.times(Matrix4.translate(this.centroid.negated()));
	}
	
	private Matrix4 getLightProjection(int lightIndex)
	{
		Matrix4 lightMatrix = getLightMatrix(lightIndex);
		
		float lightDist = new Vector3(lightMatrix.times(new Vector4(this.centroid, 1.0f))).length();
		
		float radius = (float)
			(new Matrix3(resources.viewSet.getCameraPose(0))
				.times(new Vector3(this.boundingRadius))
				.length() / Math.sqrt(3));
		
		return Matrix4.perspective(2.0f * (float)Math.atan(radius / lightDist) /*1.5f*/, 1.0f, 
				lightDist - radius,
				lightDist + radius);
	}
	
	private void generateShadowMaps(int lightIndex)
	{
		shadowProgram.setUniform("projection", getLightProjection(lightIndex));
		
		shadowFramebuffer.setDepthAttachment(shadowMaps.getLayerAsFramebufferAttachment(lightIndex));
		shadowFramebuffer.clearDepthBuffer();
		
		for (Matrix4 m : this.transformationMatrices)
		{
			shadowProgram.setUniform("model_view", getLightMatrix(lightIndex).times(m));
			shadowDrawable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);
		}
	}
	
	private Matrix4 setupLight(int lightIndex, int modelInstance)
	{
		Matrix4 lightMatrix = 
			getLightMatrix(lightIndex).times(this.transformationMatrices.get(modelInstance));
		
		// lightMatrix can be hardcoded here (comment out previous line)
			
			// Contemporary gallery and stonewall
			//Matrix4.rotateY(16 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))
				
			// Color studio 2:
			//Matrix4.rotateY(6 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))
			
			// For the synthetic falcon example?
			//Matrix4.rotateY(5 * Math.PI / 4).times(Matrix4.rotateX(-Math.PI / 4))
				
			// Always end with this when hardcoding:
			//	.times(new Matrix4(new Matrix3(resources.viewSet.getCameraPose(0))));
		
		if (lightIndex == 0)
		{
			program.setUniform("envMapMatrix", lightMatrix);
		}
		Vector3 lightPos = new Vector3(lightMatrix.quickInverse(0.001f).times(new Vector4(0.0f, 0.0f, 0.0f, 1.0f)));
		
		program.setUniform("lightPosVirtual[" + lightIndex + "]", lightPos);
		
		Vector3 controllerLightIntensity = lightModel.getLightColor(lightIndex);
		float lightDistance = new Vector3(getLightMatrix(lightIndex).times(new Vector4(this.centroid, 1.0f))).length();

		float scale = resources.viewSet.areLightSourcesInfinite() ? 1.0f :
				new Vector3(resources.viewSet.getCameraPose(0)
						.times(new Vector4(resources.geometry.getCentroid(), 1.0f))).length();
		
		program.setUniform("lightIntensityVirtual[" + lightIndex + "]", 
				controllerLightIntensity.times(lightDistance * lightDistance * resources.viewSet.getLightIntensity(0).y / (scale * scale)));
		program.setUniform("lightMatrixVirtual[" + lightIndex + "]", getLightProjection(lightIndex).times(lightMatrix));
		program.setUniform("virtualLightCount", Math.min(4, lightModel.getLightCount()));
		
		return lightMatrix;
	}
	
	private Matrix4 setupLightForFidelity(Vector3 lightIntensity, Vector3 lightPos)
	{
		float lightDist = lightPos.distance(this.resources.geometry.getCentroid());
		
		float radius = (float)
			(new Matrix3(resources.viewSet.getCameraPose(0))
				.times(new Vector3(this.resources.geometry.getBoundingRadius()))
				.length() / Math.sqrt(3));
		
		Matrix4 lightProjection = Matrix4.perspective(2.0f * (float)Math.atan(radius / lightDist) /*1.5f*/, 1.0f, 
				lightDist - radius,
				lightDist + radius);
		
		Matrix4 lightMatrix = Matrix4.lookAt(lightPos, this.resources.geometry.getCentroid(), new Vector3(0.0f, 1.0f, 0.0f));
		
    	shadowProgram.setUniform("model_view", lightMatrix);
		shadowProgram.setUniform("projection", lightProjection);
		
		shadowFramebuffer.setDepthAttachment(shadowMaps.getLayerAsFramebufferAttachment(0));
		shadowFramebuffer.clearDepthBuffer();
		shadowDrawable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);
		
		program.setUniform("lightPosVirtual[0]", lightPos);
		
		program.setUniform("lightIntensityVirtual[0]", lightIntensity);
		program.setUniform("lightMatrixVirtual[0]", lightProjection.times(lightMatrix));
		
		program.setUniform("virtualLightCount", 1);
		
		return lightMatrix;
	}
	
	private Matrix4 getViewMatrix()
	{
    	float scale = new Vector3(resources.viewSet.getCameraPose(0).times(new Vector4(resources.geometry.getCentroid(), 1.0f))).length() 
    			* this.boundingRadius / resources.geometry.getBoundingRadius();
		
		return Matrix4.scale(scale)
    			.times(cameraModel.getLookMatrix())
    			.times(Matrix4.scale(1.0f / scale))
    			.times(new Matrix4(new Matrix3(resources.viewSet.getCameraPose(0))))
    			.times(Matrix4.translate(this.centroid.negated()));
	}
	
	private Matrix4 getModelViewMatrix(int modelInstance)
	{
		return getViewMatrix().times(transformationMatrices.get(modelInstance));
	}
	
	private Matrix4 getProjectionMatrix()
	{
    	FramebufferSize size = context.getDefaultFramebuffer().getSize();
		float scale = new Vector3(resources.viewSet.getCameraPose(0).times(new Vector4(resources.geometry.getCentroid(), 1.0f))).length()
			* this.boundingRadius / resources.geometry.getBoundingRadius();
		
		return Matrix4.perspective(
    			//(float)(1.0),
    			resources.viewSet.getCameraProjection(
    					resources.viewSet.getCameraProjectionIndex(resources.viewSet.getPrimaryViewIndex()))
					.getVerticalFieldOfView(), 
    			(float)size.width / (float)size.height, 
    			0.01f * scale, 100.0f * scale);
	}

	private void drawReferenceScene(Program<ContextType> program)
	{
    	if (referenceScene != null && refScenePositions != null && refSceneNormals != null)
    	{
			Drawable<ContextType> drawable = context.createDrawable(program);
			drawable.addVertexBuffer("position", refScenePositions);
			drawable.addVertexBuffer("normal", refSceneNormals);
			
			if (refSceneTexture != null && refSceneTexCoords != null)
			{
				drawable.addVertexBuffer("texCoord", refSceneTexCoords);
				program.setTexture("diffuseMap", refSceneTexture);
				program.setUniform("useDiffuseTexture", true);
			}
			else
			{
				program.setUniform("useDiffuseTexture", false);
			}
			
			Matrix4 view = getViewMatrix();
    		program.setUniform("model_view", view);
			program.setUniform("viewPos", new Vector3(view.quickInverse(0.01f).getColumn(3)));
        	
        	if(halfResEnabled) 
        	{
    			// Do first pass at half resolution to off-screen buffer
        		drawable.draw(PrimitiveMode.TRIANGLES, offscreenFBO);
        	} 
        	else 
        	{
        		drawable.draw(PrimitiveMode.TRIANGLES, context.getDefaultFramebuffer());  
        	}
    	}
	}
	
	@Override
	public void draw() 
	{
		try
		{
			setupForRelighting();
			
			if(multisamplingEnabled)
			{
				context.enableMultisampling();
			}
			else
			{
				context.disableMultisampling();			
			}
	    	
	    	context.enableBackFaceCulling();
	    	
	    	Framebuffer<ContextType> framebuffer = context.getDefaultFramebuffer();
	    	FramebufferSize size = framebuffer.getSize();
	    	
	    	this.setupForDraw();
	    	
	    	Matrix4 projection;
	    	
	    	mainDrawable.program().setUniform("projection", projection = getProjectionMatrix());
	    	
	    	if (environmentMap != null && environmentMapEnabled)
			{
				environmentBackgroundProgram.setUniform("useEnvironmentTexture", true);
				environmentBackgroundProgram.setTexture("env", environmentMap);
				environmentBackgroundProgram.setUniform("model_view", this.getViewMatrix());
				environmentBackgroundProgram.setUniform("projection", projection);
				environmentBackgroundProgram.setUniform("envMapMatrix", envMapMatrix == null ? Matrix4.identity() : envMapMatrix);
				environmentBackgroundProgram.setUniform("envMapIntensity", this.clearColor);

				environmentBackgroundProgram.setUniform("gamma", 
						environmentMap.isInternalFormatCompressed() || 
						environmentMap.getInternalUncompressedColorFormat().dataType != DataType.FLOATING_POINT 
						? 1.0f : 2.2f);
			}
	    	
	        this.offscreenFBO = null;
	        
	        try
	        {
		        int fboWidth, fboHeight;
				if (halfResEnabled)
				{
					fboWidth = size.width / 2;
					fboHeight = size.height / 2;
					
					offscreenFBO = context.getFramebufferObjectBuilder(fboWidth, fboHeight)
							.addColorAttachment(new ColorAttachmentSpec(ColorFormat.RGB8)
								.setLinearFilteringEnabled(true))
							.addDepthAttachment(new DepthAttachmentSpec(32, false))
							.createFramebufferObject();
					
					offscreenFBO.clearColorBuffer(0, clearColor.x, clearColor.y, clearColor.z, 1.0f);
			    	offscreenFBO.clearDepthBuffer();
		    		
		    		if (environmentMap != null && environmentMapEnabled)
		    		{
		    			context.disableDepthTest();
		    			this.environmentBackgroundDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
		    			context.enableDepthTest();
		    		}
				}
				else
				{
					fboWidth = size.width;
					fboHeight = size.height;
					
					framebuffer.clearColorBuffer(0, clearColor.x, clearColor.y, clearColor.z, 1.0f);
		    		framebuffer.clearDepthBuffer();
		    		
		    		if (environmentMap != null && environmentMapEnabled)
		    		{
		    			context.disableDepthTest();
		    			this.environmentBackgroundDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
		    			context.enableDepthTest();
		    		}
				}
				
				for (int lightIndex = 0; lightIndex < lightModel.getLightCount(); lightIndex++)
				{
					generateShadowMaps(lightIndex);
				}
	
				this.program.setUniform("imageBasedRenderingEnabled", false);
				this.drawReferenceScene(this.program);
				this.program.setUniform("imageBasedRenderingEnabled", this.settings.isIBREnabled());
				setupForRelighting(); // changed anything changed when drawing the reference scene.
				
				for (int modelInstance = 0; modelInstance < transformationMatrices.size(); modelInstance++)
				{
					this.envMapMatrix = null;
					
					for (int lightIndex = 0; lightIndex < lightModel.getLightCount(); lightIndex++)
					{
						Matrix4 matrix = setupLight(lightIndex, modelInstance);
						
						if (lightIndex == 0)
						{
							this.envMapMatrix = matrix;
						}
					}
					
					// Draw instance
					Matrix4 modelView = getModelViewMatrix(modelInstance);
					this.program.setUniform("model_view", modelView);
					this.program.setUniform("viewPos", new Vector3(modelView.quickInverse(0.01f).getColumn(3)));
			    	
			    	if(halfResEnabled) 
			    	{
						// Do first pass at half resolution to off-screen buffer
				        mainDrawable.draw(PrimitiveMode.TRIANGLES, offscreenFBO);
			    	} 
			    	else 
			    	{
				        mainDrawable.draw(PrimitiveMode.TRIANGLES, context.getDefaultFramebuffer());  
			    	}
				}
				
				// Finish drawing
				if (halfResEnabled)
				{
		    		context.flush();
			        
			        // Second pass at full resolution to default framebuffer
			    	simpleTexDrawable.program().setTexture("tex", offscreenFBO.getColorAttachmentTexture(0));    	
	
		    		framebuffer.clearDepthBuffer();
			    	simpleTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
			    	
			    	context.flush();
		    		offscreenFBO.close();
				}
				else
				{
			    	context.flush();
				}
			}
	        finally
	        {
	        	if (this.offscreenFBO != null)
	        	{
	        		this.offscreenFBO.close();
	        		this.offscreenFBO = null;
	        	}
	        }
			
			FramebufferSize windowSize = context.getDefaultFramebuffer().getSize();
			
			context.setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
			
			Matrix4 viewMatrix = this.getViewMatrix();
			
			if (this.settings().isRelightingEnabled() && this.settings().areVisibleLightsEnabled())
			{
				for (int i = 0; i < lightModel.getLightCount(); i++)
				{
					if (lightModel.isLightVisualizationEnabled(i))
					{
						this.lightProgram.setUniform("color", lightModel.getLightColor(i));
						
						Vector3 lightPosition = new Vector3(viewMatrix.times(this.getLightMatrix(i).quickInverse(0.001f)).getColumn(3));
						
						this.lightProgram.setUniform("model_view",
	
	//							modelView.times(this.getLightMatrix(i).quickInverse(0.001f)));
							Matrix4.translate(lightPosition)
								.times(Matrix4.scale((float)windowSize.height * -lightPosition.z / (16.0f * windowSize.width), -lightPosition.z / 16.0f, 1.0f)));
						this.lightProgram.setUniform("projection", this.getProjectionMatrix());
			    		this.lightProgram.setTexture("lightTexture", this.lightTexture);
						this.lightDrawable.draw(PrimitiveMode.TRIANGLE_FAN, context);
					}
				}
			}
			
			context.disableAlphaBlending();
		}
		catch(Exception e)
		{
			if (!suppressErrors)
			{
				e.printStackTrace();
				suppressErrors = true; // Prevent excessive errors
			}
		}
	}

	@Override
	public void close() 
	{
		resources.close();
    	
		if (this.refScenePositions != null)
		{
			this.refScenePositions.close();
			this.refScenePositions = null;
		}
		
		if (this.refSceneTexCoords != null)
		{
			this.refSceneTexCoords.close();
			this.refSceneTexCoords = null;
		}
		
		if (this.refSceneNormals != null)
		{
			this.refSceneNormals.close();
			this.refSceneNormals = null;
		}
		
		if (this.refSceneTexture != null)
		{
			this.refSceneTexture.close();
			this.refSceneTexture = null;
		}
		
		if (this.environmentBackgroundProgram != null)
		{
			this.environmentBackgroundProgram.close();
			this.environmentBackgroundProgram = null;
		}
		
		if (this.environmentMap != null)
		{
			this.environmentMap.close();
			this.environmentMap = null;
		}
		
		// New code
		if (resources != null)
		{
			resources.close();
			resources = null;
		}
		
		if (shadowMaps != null)
		{
			shadowMaps.close();
			shadowMaps = null;
		}
		
		if (shadowFramebuffer != null)
		{
			shadowFramebuffer.close();
			shadowFramebuffer = null;
		}
		
		if (lightProgram != null)
		{
			lightProgram.close();
		}
		
		if (lightVertices != null)
		{
			lightVertices.close();
		}
		
		if (lightTexture != null)
		{
			lightTexture.close();
		}
	}
	
	private void resample() throws IOException
	{
		ViewSet targetViewSet = ViewSet.loadFromVSETFile(resampleVSETFile);
		FramebufferObject<ContextType> framebuffer = context.getFramebufferObjectBuilder(resampleWidth, resampleHeight)
				.addColorAttachment()
				.addDepthAttachment()
				.createFramebufferObject();
    	
    	this.setupForDraw();
    	
		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		{
	    	mainDrawable.program().setUniform("model_view", targetViewSet.getCameraPose(i));
	    	mainDrawable.program().setUniform("viewPos", new Vector3(targetViewSet.getCameraPose(i).quickInverse(0.01f).getColumn(3)));
	    	mainDrawable.program().setUniform("projection", 
    			targetViewSet.getCameraProjection(targetViewSet.getCameraProjectionIndex(i))
    				.getProjectionMatrix(targetViewSet.getRecommendedNearPlane(), targetViewSet.getRecommendedFarPlane()));
	    	
	    	this.resampleSetupCallback.accept(targetViewSet.getCameraPose(i));
	    	
	    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, /*1.0f*/0.0f);
	    	framebuffer.clearDepthBuffer();
	    	
	    	if (environmentMap != null && environmentMapEnabled)
			{
				environmentBackgroundProgram.setUniform("useEnvironmentTexture", true);
				environmentBackgroundProgram.setTexture("env", environmentMap);
				environmentBackgroundProgram.setUniform("model_view", targetViewSet.getCameraPose(i));
				environmentBackgroundProgram.setUniform("projection", 
					targetViewSet.getCameraProjection(targetViewSet.getCameraProjectionIndex(i))
	    				.getProjectionMatrix(targetViewSet.getRecommendedNearPlane(), targetViewSet.getRecommendedFarPlane()));
				environmentBackgroundProgram.setUniform("envMapMatrix", envMapMatrix == null ? Matrix4.identity() : envMapMatrix);
				environmentBackgroundProgram.setUniform("envMapIntensity", this.clearColor);

				environmentBackgroundProgram.setUniform("gamma", 
						environmentMap.isInternalFormatCompressed() || 
						environmentMap.getInternalUncompressedColorFormat().dataType != DataType.FLOATING_POINT 
						? 1.0f : 2.2f);
				
				context.disableDepthTest();
				this.environmentBackgroundDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
				context.enableDepthTest();
			}
    		
    		framebuffer.clearDepthBuffer();
	    	mainDrawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
	    	
	    	File exportFile = new File(resampleExportPath, targetViewSet.getImageFileName(i));
	    	exportFile.getParentFile().mkdirs();
	        framebuffer.saveColorBufferToFile(0, "PNG", exportFile);
	        
	        if (this.callback != null)
	        {
	        	this.callback.setProgress((double) i / (double) targetViewSet.getCameraPoseCount());
	        }
		}
		
		this.resampleCompleteCallback.run();
		
		Files.copy(resampleVSETFile.toPath(), 
			new File(resampleExportPath, resampleVSETFile.getName()).toPath(),
			StandardCopyOption.REPLACE_EXISTING);
		Files.copy(resources.viewSet.getGeometryFile().toPath(), 
			new File(resampleExportPath, resources.viewSet.getGeometryFile().getName()).toPath(),
			StandardCopyOption.REPLACE_EXISTING);
	}
	
	private double calculateError(Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, NativeIntVectorList viewIndexList, int targetViewIndex, int activeViewCount)
	{
		this.setupForDraw(drawable.program());
    	
    	drawable.program().setUniform("model_view", this.resources.viewSet.getCameraPose(targetViewIndex));
    	drawable.program().setUniform("viewPos", new Vector3(this.resources.viewSet.getCameraPose(targetViewIndex).quickInverse(0.01f).getColumn(3)));
    	drawable.program().setUniform("projection", 
    			this.resources.viewSet.getCameraProjection(this.resources.viewSet.getCameraProjectionIndex(targetViewIndex))
				.getProjectionMatrix(this.resources.viewSet.getRecommendedNearPlane(), this.resources.viewSet.getRecommendedFarPlane()));

    	drawable.program().setUniform("targetViewIndex", targetViewIndex);
		
    	try (UniformBuffer<ContextType> viewIndexBuffer = context.createUniformBuffer().setData(viewIndexList))
    	{
	    	drawable.program().setUniformBuffer("ViewIndices", viewIndexBuffer);
	    	drawable.program().setUniform("viewCount", activeViewCount);
	    	
	    	framebuffer.clearColorBuffer(0, -1.0f, -1.0f, -1.0f, -1.0f);
	    	framebuffer.clearDepthBuffer();
	    	
	    	drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
    	}

//	        if (activeViewCount == assets.viewSet.getCameraPoseCount() - 1 /*&& this.assets.viewSet.getImageFileName(i).matches(".*R1[^1-9].*")*/)
//	        {
//		    	File fidelityImage = new File(new File(fidelityExportPath.getParentFile(), "debug"), this.assets.viewSet.getImageFileName(i));
//		        framebuffer.saveColorBufferToFile(0, "PNG", fidelityImage);
//	        }
        	
        double sumSqError = 0.0;
        double sumWeights = 0.0;
        double sumMask = 0.0;

    	float[] fidelityArray = framebuffer.readFloatingPointColorBufferRGBA(0);
    	for (int k = 0; 4 * k + 3 < fidelityArray.length; k++)
    	{
			if (fidelityArray[4 * k + 1] >= 0.0f)
			{
				sumSqError += fidelityArray[4 * k];
				sumWeights += fidelityArray[4 * k + 1];
				sumMask += 1.0;
			}
    	}
    	
    	return Math.sqrt(sumSqError / sumMask);
	}
	
	private void executeFidelityComputation() throws IOException
	{
		System.out.println("\nView Importance:");
		
    	Vector3[] viewDirections = new Vector3[this.resources.viewSet.getCameraPoseCount()];
    	
    	for (int i = 0; i < this.resources.viewSet.getCameraPoseCount(); i++)
    	{
    		viewDirections[i] = new Vector3(this.resources.viewSet.getCameraPoseInverse(i).getColumn(3))
    				.minus(this.resources.geometry.getCentroid()).normalized();
		}
    	
    	double[][] viewDistances = new double[this.resources.viewSet.getCameraPoseCount()][this.resources.viewSet.getCameraPoseCount()];
    	
    	for (int i = 0; i < this.resources.viewSet.getCameraPoseCount(); i++)
    	{
    		for (int j = 0; j < this.resources.viewSet.getCameraPoseCount(); j++)
    		{
    			viewDistances[i][j] = Math.acos(Math.max(-1.0, Math.min(1.0f, viewDirections[i].dot(viewDirections[j]))));
    		}
    	}
    	
    	try
    	(
			Program<ContextType> fidelityProgram = context.getShaderProgramBuilder()
				.addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
				.addShader(ShaderType.FRAGMENT, new File("shaders/relight/fidelity.frag"))
				.createProgram();
    			
			FramebufferObject<ContextType> framebuffer = context.getFramebufferObjectBuilder(256, 256/*1024, 1024*/)
				.addColorAttachment(ColorFormat.RG32F)
				.createFramebufferObject();
    			
			PrintStream out = new PrintStream(fidelityExportPath);
		)
    	{
    		context.disableBackFaceCulling();
    		
    		Drawable<ContextType> drawable = context.createDrawable(fidelityProgram);
        	drawable.addVertexBuffer("position", this.resources.positionBuffer);
        	drawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
        	drawable.addVertexBuffer("normal", this.resources.normalBuffer);
        	drawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
        	setupForDraw(fidelityProgram);
    		
    		double[] slopes = new double[this.resources.viewSet.getCameraPoseCount()];
    		double[] peaks = new double[this.resources.viewSet.getCameraPoseCount()];
    		
			this.callback.setMaximum(this.resources.viewSet.getCameraPoseCount());
			
			new File(fidelityExportPath.getParentFile(), "debug").mkdir();
    		
    		for (int i = 0; i < this.resources.viewSet.getCameraPoseCount(); i++)
			{
		    	if (this.fidelitySetupCallback != null)
	    		{
		    		this.fidelitySetupCallback.accept(i);
	    		}
    			
    			System.out.println(this.resources.viewSet.getImageFileName(i));
    			out.print(this.resources.viewSet.getImageFileName(i) + "\t");
    			
    			double lastMinDistance = 0.0;
    			double minDistance;
    			int activeViewCount;
    			double sumMask = 0.0;
    			
    			List<Double> distances = new ArrayList<Double>();
    			List<Double> errors = new ArrayList<Double>();
    			
    			distances.add(0.0);
    			errors.add(0.0);
    			
    			do 
    			{
			    	NativeIntVectorList viewIndexList = new NativeIntVectorList(1, this.resources.viewSet.getCameraPoseCount());
			    	
			    	activeViewCount = 0;
			    	minDistance = Float.MAX_VALUE;
			    	for (int j = 0; j < this.resources.viewSet.getCameraPoseCount(); j++)
			    	{
			    		if (i != j && viewDistances[i][j] > lastMinDistance)
			    		{
			    			minDistance = Math.min(minDistance, viewDistances[i][j]);
			    			viewIndexList.set(activeViewCount, 0, j);
			    			activeViewCount++;
			    		}
			    	}
			    	
			    	if (activeViewCount > 0)
			    	{
				    	if (sumMask >= 0.0)
				    	{
					        distances.add(minDistance);
					        errors.add(calculateError(drawable, framebuffer, viewIndexList, i, activeViewCount));
					    	lastMinDistance = minDistance;
				    	}
			    	}
    			}
    			while(sumMask >= 0.0 && activeViewCount > 0 && minDistance < /*0*/ Math.PI / 4);
    			
    			// Fit the error v. distance data to a quadratic with a few constraints.
    			// First, the quadratic must pass through the origin.
    			// Second, the slope at the origin must be positive.
    			// Finally, the "downward" slope of the quadratic will be clamped to the quadratic's maximum value 
    			// to ensure that the function is monotonically increasing or constant.
    			// (So only half of the quadratic will actually be used.)
    			double peak = -1.0, slope = -1.0;
    			double maxDistance = distances.get(distances.size() - 1);
    			double prevPeak, prevSlope, prevMaxDistance;
    			
    			// Every time we fit a quadratic, the data that would have been clamped on the downward slope messes up the fit.
    			// So we should keep redoing the fit without that data affecting the initial slope and only affecting the peak value.
    			// This continues until convergence (no new data points are excluded from the quadratic).
    			do
    			{
	    			double sumSquareDistances = 0.0;
	    			double sumCubeDistances = 0.0;
	    			double sumFourthDistances = 0.0;
	    			double sumErrorDistanceProducts = 0.0;
	    			double sumErrorSquareDistanceProducts = 0.0;
	    			
	    			double sumHighErrors = 0.0;
	    			int countHighErrors = 0;
	    			
	    			for (int k = 0; k < distances.size(); k++)
	    			{
	    				double distance = distances.get(k);
	    				double error = errors.get(k);
	    				
	    				if (distance < maxDistance)
	    				{
	    					double distanceSq = distance * distance;
	    				
		    				sumSquareDistances += distanceSq;
		    				sumCubeDistances += distance * distanceSq;
		    				sumFourthDistances += distanceSq * distanceSq;
		    				sumErrorDistanceProducts += error * distance;
		    				sumErrorSquareDistanceProducts += error * distanceSq;
	    				}
	    				else
	    				{
	    					sumHighErrors += error;
	    					countHighErrors++;
	    				}
	    			}
	    			
	    			prevPeak = peak;
    				prevSlope = slope;
    				
    				// Fit error vs. distance to a quadratic using least squares: a*x^2 + slope * x = error
	    			double d = (sumCubeDistances * sumCubeDistances - sumFourthDistances * sumSquareDistances);
	    			double a = (sumCubeDistances * sumErrorDistanceProducts - sumSquareDistances * sumErrorSquareDistanceProducts) / d;
	    			
	    			slope = (sumCubeDistances * sumErrorSquareDistanceProducts - sumFourthDistances * sumErrorDistanceProducts) / d;
	    			
	    			if (slope <= 0.0 || !Double.isFinite(slope) || countHighErrors > errors.size() - 5)
	    			{
	    				if (prevSlope < 0.0)
	    				{
	    					// If its the first iteration, use a linear function
		    				// peak=0 is a special case for designating a linear function
		    				peak = 0.0;
		    				slope = sumErrorDistanceProducts / sumSquareDistances;
	    				}
	    				else
	    				{
		    				// Revert to the previous peak and slope
		    				slope = prevSlope;
		    				peak = prevPeak;
	    				}
	    			}
	    			else
	    			{
		    			// Peak can be determined from a and the slope.
		    			double leastSquaresPeak = slope * slope / (-4 * a);

		    			if (Double.isFinite(leastSquaresPeak) && leastSquaresPeak > 0.0)
		    			{
		    				if (countHighErrors == 0)
		    				{
		    					peak = leastSquaresPeak;
		    				}
		    				else
		    				{
				    			// Do a weighted average between the least-squares peak and the average of all the errors that would be on the downward slope of the quadratic,
				    			// but are instead clamped to the maximum of the quadratic.
				    			// Clamp the contribution of the least-squares peak to be no greater than twice the average of the other values.
				    			peak = (Math.min(2 * sumHighErrors / countHighErrors, leastSquaresPeak) * (errors.size() - countHighErrors) + sumHighErrors) / errors.size();
		    				}
		    			}
		    			else if (prevPeak < 0.0)
	    				{
	    					// If its the first iteration, use a linear function
		    				// peak=0 is a special case for designating a linear function
		    				peak = 0.0;
		    				slope = sumErrorDistanceProducts / sumSquareDistances;
	    				}
	    				else
	    				{
		    				// Revert to the previous peak and slope
		    				slope = prevSlope;
		    				peak = prevPeak;
	    				}
	    			}
	    			
	    			// Update the max distance and previous max distance.
	    			prevMaxDistance = maxDistance;
	    			maxDistance = 2 * peak / slope;
    			}
    			while(maxDistance < prevMaxDistance && peak > 0.0);
    			
    			if (errors.size() >= 2)
    			{
    				out.println(slope + "\t" + peak + "\t" + minDistance + "\t" + errors.get(1));
    			}
    			
    			System.out.println("Slope: " + slope);
    			System.out.println("Peak: " + peak);
    			System.out.println();
    			
    			slopes[i] = slope;
    			peaks[i] = peak;
    			
    			for (Double distance : distances)
    			{
    				out.print(distance + "\t");
    			}
    			out.println();

    			for (Double error : errors)
    			{
    				out.print(error + "\t");
    			}
    			out.println();
    			
    			out.println();
		        
		        if (this.callback != null)
		        {
		        	this.callback.setProgress(i);
		        }
			}
    		
    		if (fidelityVSETFile != null && fidelityVSETFile.exists())
    		{
	    		out.println();
	    		out.println("Expected error for views in target view set:");
	    		out.println();
	    		
	    		ViewSet targetViewSet = ViewSet.loadFromVSETFile(fidelityVSETFile);
	    		
	    		Vector3[] targetDirections = new Vector3[targetViewSet.getCameraPoseCount()];
	    		
	    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
	        	{
	    			targetDirections[i] = new Vector3(targetViewSet.getCameraPoseInverse(i).getColumn(3))
	        				.minus(this.resources.geometry.getCentroid()).normalized();
	    		}
	    		
	    		// Determine a function describing the error of each quadratic view by blending the slope and peak parameters from the known views.
	    		double[] targetSlopes = new double[targetViewSet.getCameraPoseCount()];
	    		double[] targetPeaks = new double[targetViewSet.getCameraPoseCount()];
	    		
	    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
	    		{
	    			double weightedSlopeSum = 0.0;
	    			double weightSum = 0.0;
	    			double weightedPeakSum = 0.0;
	    			double peakWeightSum = 0.0;
	    			
	    			for (int k = 0; k < slopes.length; k++)
	    			{
	    				double weight = 1 / Math.max(0.000001, 1.0 - 
	    						Math.pow(Math.max(0.0, targetDirections[i].dot(viewDirections[k])), this.settings().getWeightExponent())) 
							- 1.0;
	    				
	    				if (peaks[k] > 0)
    					{
	    					weightedPeakSum += weight * peaks[k];
	    					peakWeightSum += weight;
    					}
	    				
						weightedSlopeSum += weight * slopes[k];
	    				weightSum += weight;
	    			}
	    			
	    			targetSlopes[i] = weightedSlopeSum / weightSum;
	    			targetPeaks[i] = peakWeightSum == 0.0 ? 0.0 : weightedPeakSum / peakWeightSum;
	    		}
	    		
	    		double[] targetDistances = new double[targetViewSet.getCameraPoseCount()];
	    		double[] targetErrors = new double[targetViewSet.getCameraPoseCount()];
	    		
	    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
	    		{
    				targetDistances[i] = Double.MAX_VALUE;
	    		}
	    		
	    		boolean[] originalUsed = new boolean[this.resources.viewSet.getCameraPoseCount()];
				
				NativeIntVectorList viewIndexList = new NativeIntVectorList(1, this.resources.viewSet.getCameraPoseCount());
	    		int activeViewCount = 0;
	    		
	    		// Print views that are only in the original view set and NOT in the target view set
	    		// This also initializes the distances for the target views.
	    		for (int j = 0; j < this.resources.viewSet.getCameraPoseCount(); j++)
	    		{
	    			// First determine if the view is in the target view set
	    			boolean found = false;
	    			for (int i = 0; !found && i < targetViewSet.getCameraPoseCount(); i++)
	    			{
	    				if (targetViewSet.getImageFileName(i).contains(this.resources.viewSet.getImageFileName(j).split("\\.")[0]))
	    				{
	    					found = true;
	    				}
	    			}
	    			
	    			if (!found)
	    			{
	    				// If it isn't, then print it to the file
	    				originalUsed[j] = true;
	    				out.print(this.resources.viewSet.getImageFileName(j).split("\\.")[0] + "\t" + slopes[j] + "\t" + peaks[j] + "\tn/a\t" + 
	    						calculateError(drawable, framebuffer, viewIndexList, j, activeViewCount) + "\t");

	    				viewIndexList.set(activeViewCount, 0, j);
	    				activeViewCount++;

			    		double cumError = 0.0;
			    		
			    		for (int k = 0; k < this.resources.viewSet.getCameraPoseCount(); k++)
			    		{
			    			if (!originalUsed[k])
			    			{
			    				cumError += calculateError(drawable, framebuffer, viewIndexList, k, activeViewCount);
			    			}
			    		}
			    		
			    		out.println(cumError);
	    				
	    				// Then update the distances for all of the target views
	    				for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		    			{
		    				targetDistances[i] = Math.min(targetDistances[i], Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(viewDirections[j])))));
		    			}
	    			}
	    		}
	    		
				// Now update the errors for all of the target views
				for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
    			{
    				if (Double.isFinite(targetPeaks[i]))
					{
    					double peakDistance = 2 * targetPeaks[i] / targetSlopes[i];
    					if (targetDistances[i] > peakDistance)
    					{
    						targetErrors[i] = targetPeaks[i];
    					}
    					else
    					{
    						targetErrors[i] = targetSlopes[i] * targetDistances[i] - targetSlopes[i] * targetSlopes[i] * targetDistances[i] * targetDistances[i] / (4 * targetPeaks[i]);
    					}
					}
    				else
    				{
    					targetErrors[i] = targetSlopes[i] * targetDistances[i];
    				}
    			}
	    		
	    		boolean[] targetUsed = new boolean[targetErrors.length];

	    		int unusedOriginalViews = 0;
	    		for (int j = 0; j < this.resources.viewSet.getCameraPoseCount(); j++)
	    		{
	    			if (!originalUsed[j])
	    			{
	    				unusedOriginalViews++;
	    			}
	    		}
	    		
	    		// Views that are in both the target view set and the original view set
	    		// Go through these views in order of importance so that when loaded viewset = target viewset, it generates a ground truth ranking.
	    		while(unusedOriginalViews > 0)
	    		{
	    			double maxError = -1.0;
	    			int maxErrorTargetIndex = -1;
	    			int maxErrorOriginalIndex = -1;
	    			
	    			// Determine which view to do next.  Must be in both view sets and currently have more error than any other view in both view sets.
		    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		    		{
	    	    		for (int j = 0; j < this.resources.viewSet.getCameraPoseCount(); j++) 
		    			{
		    				if (targetViewSet.getImageFileName(i).contains(this.resources.viewSet.getImageFileName(j).split("\\.")[0]))
		    				{
		    					// Can't be previously used and must have more error than any other view
				    			if (!originalUsed[j] && targetErrors[i] > maxError)
			    				{
			    					maxError = targetErrors[i];
			    					maxErrorTargetIndex = i;
			    					maxErrorOriginalIndex = j;
			    				}
		    				}
		    			}
		    		}
		    		
		    		// Print the view to the file
		    		out.print(targetViewSet.getImageFileName(maxErrorTargetIndex).split("\\.")[0] + "\t" + targetSlopes[maxErrorTargetIndex] + "\t" + targetPeaks[maxErrorTargetIndex] + "\t" + 
	    					targetDistances[maxErrorTargetIndex] + "\t" + targetErrors[maxErrorTargetIndex] + "\t");
					
		    		// Flag that its been used
					targetUsed[maxErrorTargetIndex] = true;
					originalUsed[maxErrorOriginalIndex] = true;
					
					double expectedCumError = 0.0;
	    			
					// Update all of the other target distances and errors that haven't been used yet
	    			for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++) 
	    			{
    					// Don't update previously used views
	    				if (!targetUsed[i])
	    				{
	    					// distance
	    					targetDistances[i] = Math.min(targetDistances[i], 
	    							Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(targetDirections[maxErrorTargetIndex])))));

	    					// error
	        				if (Double.isFinite(targetPeaks[i]))
	    					{
	        					double peakDistance = 2 * targetPeaks[i] / targetSlopes[i];
	        					if (targetDistances[i] > peakDistance)
	        					{
	        						targetErrors[i] = targetPeaks[i];
	        					}
	        					else
	        					{
	        						targetErrors[i] = targetSlopes[i] * targetDistances[i] - targetSlopes[i] * targetSlopes[i] * targetDistances[i] * targetDistances[i] / (4 * targetPeaks[i]);
	        					}
	    					}
	        				else
	        				{
	        					targetErrors[i] = targetSlopes[i] * targetDistances[i];
	        				}
	        				
	        				expectedCumError += targetErrors[i];
	    				}
	    			}

		    		out.println(expectedCumError);
	    			
	    			// Count how many views from the original view set haven't been used.
	    			unusedOriginalViews = 0;
		    		for (int j = 0; j < this.resources.viewSet.getCameraPoseCount(); j++)
		    		{
		    			if (!originalUsed[j])
		    			{
		    				unusedOriginalViews++;
		    			}
		    		}
	    		}

	    		// Views that are in the target view set and NOT in the original view set
    			int unused;
	    		do
	    		{
	    			unused = 0;
	    			double maxError = -1.0;
	    			int maxErrorIndex = -1;

	    			// Determine which view to do next.  Must be in both view sets and currently have more error than any other view in both view sets.
		    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		    		{
    					// Can't be previously used and must have more error than any other view
		    			if (!targetUsed[i])
		    			{
		    				// Keep track of number of unused views at the same time
		    				unused++;
		    				
		    				if (targetErrors[i] > maxError)
		    				{
		    					maxError = targetErrors[i];
		    					maxErrorIndex = i;
		    				}
		    			}
		    		}
		    		
		    		if (maxErrorIndex >= 0)
		    		{
			    		// Print the view to the file
		    			out.print(targetViewSet.getImageFileName(maxErrorIndex).split("\\.")[0] + "\t" + targetSlopes[maxErrorIndex] + "\t" + targetPeaks[maxErrorIndex] + "\t" + 
    	    					targetDistances[maxErrorIndex] + "\t" + targetErrors[maxErrorIndex] + "\t"); 
		    			
		    			// Flag that its been used
		    			targetUsed[maxErrorIndex] = true;
			    		unused--;
			    		
			    		double cumError = 0.0;
		    			
						// Update all of the other target distances and errors
			    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++) 
		    			{
	    					// Don't update previously used views
		    				if (!targetUsed[i])
		    				{
		    					// distance
		    					targetDistances[i] = Math.min(targetDistances[i], 
		    							Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(targetDirections[maxErrorIndex])))));
	
		    					// error
		        				if (Double.isFinite(targetPeaks[i]))
		    					{
		        					double peakDistance = 2 * targetPeaks[i] / targetSlopes[i];
		        					if (targetDistances[i] > peakDistance)
		        					{
		        						targetErrors[i] = targetPeaks[i];
		        					}
		        					else
		        					{
		        						targetErrors[i] = targetSlopes[i] * targetDistances[i] - targetSlopes[i] * targetSlopes[i] * targetDistances[i] * targetDistances[i] / (4 * targetPeaks[i]);
		        					}
		    					}
		        				else
		        				{
		        					targetErrors[i] = targetSlopes[i] * targetDistances[i];
		        				}
		        				
		        				cumError += targetErrors[i];
		    				}
		    			}
			    		
			    		out.println(cumError);
		    		}
	    		}
	    		while(unused > 0);
    		}
    	}
		
		if (this.fidelityCompleteCallback != null)
		{
    		this.fidelityCompleteCallback.run();
		}
	}
	
	private void exportBTF()
	{	
		try
        {
			Program<ContextType> btfProgram = context.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/relight/relight.frag"))
    				.createProgram();
			
			FramebufferObject<ContextType> framebuffer = context.getFramebufferObjectBuilder(btfWidth, btfHeight)
					.addColorAttachment()
					.createFramebufferObject();
	    	
	    	Drawable<ContextType> drawable = context.createDrawable(btfProgram);
	    	drawable.addVertexBuffer("position", this.resources.positionBuffer);
	    	drawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
	    	drawable.addVertexBuffer("normal", this.resources.normalBuffer);
	    	drawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
	    	
	    	this.setupForDraw(btfProgram);
	    	this.setupForRelighting(btfProgram);
	    	
	    	btfProgram.setUniform("useTSOverrides", true);
	    	
	    	////////////////////////////////
	    	
	    	// Backscattering
			for (int i = 1; i <= 179; i++)
			{
				double theta = i / 180.0f * Math.PI;
		    	btfProgram.setUniform("virtualLightCount", 1);
		    	btfProgram.setUniform("lightIntensityVirtual[0]", lightModel.getLightColor(0));
		    	btfProgram.setUniform("lightDirTSOverride", new Vector3((float)Math.cos(theta), 0.0f, (float)Math.sin(theta)));
		    	btfProgram.setUniform("viewDirTSOverride", new Vector3((float)Math.cos(theta), 0.0f, (float)Math.sin(theta)));
	    	
//	    	// Joey's lab
//	    	for (int i = 1; i <= 90; i++)
//			{
//				double theta = i / 180.0f * Math.PI;
//		    	btfProgram.setUniform("virtualLightCount", 1);
//		    	btfProgram.setUniform("lightIntensityVirtual[0]", lightController.getLightColor(0));
////		    	btfProgram.setUniform("lightDirTSOverride", new Vector3(-(float)(Math.sin(theta)*Math.sqrt(0.5)), -(float)(Math.sin(theta)*Math.sqrt(0.5)), (float)Math.cos(theta)));
////		    	btfProgram.setUniform("viewDirTSOverride", new Vector3((float)(Math.cos(theta)*Math.sqrt(0.5)), (float)(Math.cos(theta)*Math.sqrt(0.5)), (float)Math.sin(theta)));
//		    	btfProgram.setUniform("lightDirTSOverride", new Vector3(-(float)Math.sin(theta), 0.0f, (float)Math.cos(theta)));
//		    	btfProgram.setUniform("viewDirTSOverride", new Vector3((float)Math.cos(theta), 0.0f, (float)Math.sin(theta)));
//		    	
	    	////////////////////////////////
				
		    	context.disableBackFaceCulling();
		    	
		    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
		    	
		    	File exportFile = new File(btfExportPath, String.format("%02d.png", i));
		    	exportFile.getParentFile().mkdirs();
		        framebuffer.saveColorBufferToFile(0, "PNG", exportFile);
		        
		        if (this.callback != null)
		        {
		        	this.callback.setProgress((double) i / (double) /*90*/180);
		        }
			}
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
	}

	@Override
	public void setOnLoadCallback(IBRLoadingMonitor callback) 
	{
		this.callback = callback;
	}
	
	@Override
	public VertexGeometry getActiveProxy()
	{
		return this.resources.geometry;
	}
	
	@Override
	public ViewSet getActiveViewSet()
	{
		return this.resources.viewSet;
	}

	@Override
	public IBRSettings settings()
	{
		return this.settings;
	}

	@Override
	public boolean getHalfResolution()
	{
		return this.halfResEnabled;
	}
	
	@Override
	public void setHalfResolution(boolean halfResEnabled) 
	{
		this.halfResEnabled = halfResEnabled;
	}

	@Override
	public boolean getMultisampling() 
	{
		return this.multisamplingEnabled;
	}

	@Override
	public void setMultisampling(boolean multisamplingEnabled) 
	{
		this.multisamplingEnabled = multisamplingEnabled;
	}

	@Override
	public void requestResample(int width, int height, File targetVSETFile, File exportPath)
	{
		this.resampleRequested = true;
		this.resampleWidth = width;
		this.resampleHeight = height;
		this.resampleVSETFile = targetVSETFile;
		this.resampleExportPath = exportPath;
	}

	@Override
	public void requestFidelity(File exportPath, File targetVSETFile)
	{
		this.fidelityRequested = true;
		this.fidelityExportPath = exportPath;
		this.fidelityVSETFile = targetVSETFile;
	}

	@Override
	public void requestBTF(int width, int height, File exportPath)
	{
		this.btfRequested = true;
		this.btfWidth = width;
		this.btfHeight = height;
		this.btfExportPath = exportPath;
	}

	@Override
	public void setProgram(Program<ContextType> program) 
	{
		this.program = program;
		
		this.mainDrawable = context.createDrawable(program);
    	this.mainDrawable.addVertexBuffer("position", this.resources.positionBuffer);
    	
    	if (this.resources.normalBuffer != null)
    	{
    		this.mainDrawable.addVertexBuffer("normal", this.resources.normalBuffer);
    	}
    	
    	if (this.resources.texCoordBuffer != null)
    	{
    		this.mainDrawable.addVertexBuffer("texCoord", this.resources.texCoordBuffer);
    	}
    	
    	if (this.resources.texCoordBuffer != null)
    	{
    		this.mainDrawable.addVertexBuffer("tangent", this.resources.tangentBuffer);
    	}
    	
		suppressErrors = false;
	}
	
	@Override
	public void setEnvironment(File environmentFile)
	{
		if (environmentFile != null && environmentFile.exists())
		{
			this.newEnvironmentFile = environmentFile;
		}
	}
	
	@Override
	public String toString()
	{
		return this.id.length() > 32 
				? "..." + this.id.substring(this.id.length()-31, this.id.length()) 
				: this.id;
	}

	@Override
	public void reloadHelperShaders() 
	{
		try
        {
    		Program<ContextType> newProgram = context.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/common/texture.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/common/envbackgroundtexture.frag"))
    				.createProgram();

    		if (this.environmentBackgroundProgram != null)
    		{
    			this.environmentBackgroundProgram.close();
    		}
    		
    		this.environmentBackgroundProgram = newProgram;
	    	this.environmentBackgroundDrawable = context.createDrawable(environmentBackgroundProgram);
	    	this.environmentBackgroundDrawable.addVertexBuffer("position", context.createRectangle());

	    	
	    	newProgram = context.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/relight/light.frag"))
    				.createProgram();
	    	
			if (this.lightProgram != null)
	    	{
				this.lightProgram.close();
	    	}
    		
    		this.lightProgram = newProgram;
    		this.lightDrawable = context.createDrawable(this.lightProgram);
    		this.lightDrawable.addVertexBuffer("position", lightVertices);
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
	}

	@Override
	public void setTransformationMatrices(List<Matrix4> matrices) 
	{
		if (matrices != null)
		{
			this.transformationMatrices = matrices;
		}
	}
	
	@Override
	public void setReferenceScene(VertexGeometry scene)
	{
		this.referenceScene = scene;
		this.referenceSceneChanged = true;
	}
}
