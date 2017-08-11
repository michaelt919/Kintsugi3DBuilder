package tetzlaff.ibr.rendering;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tetzlaff.gl.AlphaBlendingFunction;
import tetzlaff.gl.AlphaBlendingFunction.Weight;
import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.ColorFormat.DataType;
import tetzlaff.gl.Context;
import tetzlaff.gl.Cubemap;
import tetzlaff.gl.CubemapFace;
import tetzlaff.gl.Drawable;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.FramebufferAttachment;
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
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.util.VertexGeometry;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector2;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.gl.vecmath.Vector4;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.IBRSettings;
import tetzlaff.ibr.LoadingMonitor;
import tetzlaff.ibr.ViewSet;
import tetzlaff.ibr.rendering2.to_sort.IBRSettingsModel;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.ReadonlyLightModel;
import tetzlaff.mvc.models.ReadonlyObjectModel;
import tetzlaff.mvc.models.SceneViewportModel;
import tetzlaff.util.EnvironmentMap;
import tetzlaff.util.ShadingParameterMode;

public class IBRImplementation<ContextType extends Context<ContextType>> implements IBRRenderable<ContextType>
{
	private ContextType context;
	private Program<ContextType> program;
	private Program<ContextType> shadowProgram;
	private LoadingMonitor callback;
	private boolean suppressErrors = false;
	private IBRSettingsModel settings;

	private IBRResources.Builder<ContextType> resourceBuilder;
	private IBRResources<ContextType> resources;
	
	private Texture3D<ContextType> shadowMaps;
	private FramebufferObject<ContextType> shadowFramebuffer;
	private Drawable<ContextType> shadowDrawable;

	private Program<ContextType> lightProgram;
	private VertexBuffer<ContextType> lightVertices;
	private Texture2D<ContextType> lightTexture;
	private Texture2D<ContextType> lightTargetTexture;
	private Drawable<ContextType> lightDrawable;
	
	private Program<ContextType> widgetProgram;
	private VertexBuffer<ContextType> widgetVertices;
	private Drawable<ContextType> widgetDrawable;
    
    private String id;
    private Drawable<ContextType> mainDrawable;

    private ReadonlyObjectModel objectModel;
    private ReadonlyCameraModel cameraModel;
	private ReadonlyLightModel lightModel;

    private Vector3 clearColor;
    private boolean halfResEnabled;
    private Program<ContextType> simpleTexProgram;
    private Drawable<ContextType> simpleTexDrawable;
    
    private File newEnvironmentFile = null;
    private boolean environmentMapUnloadRequested = false;
    private boolean environmentMapEnabled;
    private Cubemap<ContextType> environmentMap;

    private Program<ContextType> environmentBackgroundProgram;
    private Drawable<ContextType> environmentBackgroundDrawable;
    
    private boolean multisamplingEnabled = false;
    
    private UniformBuffer<ContextType> weightBuffer = null;
    
    private List<Matrix4> multiTransformationModel;
    private Vector3 centroid;
    private float boundingRadius;
    
    private VertexGeometry referenceScene = null;
    private boolean referenceSceneChanged = false;
    private VertexBuffer<ContextType> refScenePositions = null;
    private VertexBuffer<ContextType> refSceneTexCoords = null;
    private VertexBuffer<ContextType> refSceneNormals = null;
    private Texture2D<ContextType> refSceneTexture = null;
    
    private List<String> sceneObjectNameList;
    private Map<String, Integer> sceneObjectIDLookup;
	private int[] pixelObjectIDs;
	private short[] pixelDepths;
    private FramebufferSize fboSize;
    private Matrix4 partialViewMatrix;
	
	IBRImplementation(String id, ContextType context, Program<ContextType> program,
			IBRResources.Builder<ContextType> resourceBuilder)
    {
    	this.id = id;
		this.context = context;
		this.program = program;
    	this.resourceBuilder = resourceBuilder;
    	
    	this.clearColor = new Vector3(0.0f);
    	this.multiTransformationModel = new ArrayList<Matrix4>();
    	this.multiTransformationModel.add(Matrix4.IDENTITY);
    	this.settings = new IBRSettings();
    	
    	this.sceneObjectNameList = new ArrayList<String>();
    	this.sceneObjectIDLookup = new HashMap<String, Integer>();
    	
    	this.sceneObjectNameList.add(null);
    	
    	int k = 1;
    	
    	this.sceneObjectNameList.add("IBRObject");
    	this.sceneObjectIDLookup.put("IBRObject", k);
    	k++;
    	
    	this.sceneObjectNameList.add("EnvironmentMap");
    	this.sceneObjectIDLookup.put("EnvironmentMap", k);
    	k++;
    	
    	this.sceneObjectNameList.add("SceneObject");
    	this.sceneObjectIDLookup.put("SceneObject", k);
    	k++;
    	
    	for (int i = 1; i <= 4; i++)
    	{
    		this.sceneObjectNameList.add("Light" + i);
        	this.sceneObjectIDLookup.put("Light" + i, k);
    		k++;

    		this.sceneObjectNameList.add("Light" + i + ".Target");
        	this.sceneObjectIDLookup.put("Light" + i + ".Target", k);
    		k++;

    		this.sceneObjectNameList.add("Light" + i + ".AzimuthUp");
        	this.sceneObjectIDLookup.put("Light" + i + ".AzimuthUp", k);
    		k++;

    		this.sceneObjectNameList.add("Light" + i + ".AzimuthDown");
        	this.sceneObjectIDLookup.put("Light" + i + ".AzimuthDown", k);
    		k++;

    		this.sceneObjectNameList.add("Light" + i + ".InclinationUp");
        	this.sceneObjectIDLookup.put("Light" + i + ".InclinationUp", k);
    		k++;

    		this.sceneObjectNameList.add("Light" + i + ".InclinationDown");
        	this.sceneObjectIDLookup.put("Light" + i + ".InclinationDown", k);
    		k++;

    		this.sceneObjectNameList.add("Light" + i + ".DistanceUp");
        	this.sceneObjectIDLookup.put("Light" + i + ".DistanceUp", k);
    		k++;

    		this.sceneObjectNameList.add("Light" + i + ".DistanceDown");
        	this.sceneObjectIDLookup.put("Light" + i + ".DistanceDown", k);
    		k++;
    	}
    }
	
	@Override
	public IBRResources<ContextType> getResources()
	{
		return this.resources;
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
		
		if (this.widgetProgram == null)
    	{
	    	try
	        {
	    		this.widgetProgram = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/common/solid.frag"))
	    				.createProgram();
	    		this.widgetVertices = context.createVertexBuffer()
	    				.setData(NativeVectorBufferFactory.getInstance()
    						.createFromFloatArray(3, 3, new float[] 
							{ 
								-1, -1, 0,
								1, -1, 0, 
								0, 1, 0
							}));
	    		
	    		this.widgetDrawable = context.createDrawable(this.widgetProgram);
	    		this.widgetDrawable.addVertexBuffer("position", widgetVertices);
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
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
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}
		
		NativeVectorBuffer lightTextureData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 4096);

		NativeVectorBuffer lightTargetTextureData = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.FLOAT, 1, 4096);
		
		int k = 0;
		for (int i = 0; i < 64; i++)
		{
			double x = i * 2.0 / 63.0 - 1.0;
			
			for (int j = 0; j < 64; j++)
			{
				double y = j * 2.0 / 63.0 - 1.0;
				
				double rSq = x*x + y*y;
				lightTextureData.set(k, 0, (float)(Math.cos(Math.min(Math.sqrt(rSq), 1.0) * Math.PI) + 1.0) * 0.5f);
				
				if (rSq <= 1.0)
				{
					lightTargetTextureData.set(k, 0, 1.0f);
				}
				
				k++;
			}
		}
	    		
		if (this.lightTexture == null)
		{
    		this.lightTexture = context.build2DColorTextureFromBuffer(64, 64, lightTextureData)
					.setInternalFormat(ColorFormat.R8)
					.setLinearFilteringEnabled(true)
					.setMipmapsEnabled(true)
					.createTexture();
		}
    		
		if (this.lightTargetTexture == null)
		{
    		this.lightTargetTexture = context.build2DColorTextureFromBuffer(64, 64, lightTargetTextureData)
					.setInternalFormat(ColorFormat.R8)
					.setLinearFilteringEnabled(true)
					.setMipmapsEnabled(true)
					.createTexture();
		}
		
		shadowDrawable.addVertexBuffer("position", resources.positionBuffer);

		shadowMaps = context.build2DDepthTextureArray(2048, 2048, lightModel.getLightCount()).createTexture();
		shadowFramebuffer = context.buildFramebufferObject(2048, 2048)
			.addDepthAttachment()
			.createFramebufferObject();
		
		this.updateCentroidAndRadius();
		
		// Make sure that everything is loaded onto the graphics card before announcing that loading is complete.
		this.draw(context.getDefaultFramebuffer());
		
		if (this.callback != null)
		{
			this.callback.loadingComplete();
		}
	}

	@Override
	public void update() 
	{
		this.updateCentroidAndRadius();
		
		if (this.environmentMapUnloadRequested == true && this.environmentMap != null)
		{
			this.environmentMap.close();
			this.environmentMap = null;
			this.environmentMapUnloadRequested = false;
		}
		
		if (this.newEnvironmentFile != null)
		{
			File environmentFile = this.newEnvironmentFile;
			this.newEnvironmentFile = null;
			
			try
			{
				System.out.println("Loading new environment texture.");
				
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
						context.buildColorCubemap(envMap.getSide())
							.setInternalFormat(ColorFormat.RGB32F)
							.loadFace(CubemapFace.POSITIVE_X, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.PX].length / 3, sides[EnvironmentMap.PX]))
							.loadFace(CubemapFace.NEGATIVE_X, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.NX].length / 3, sides[EnvironmentMap.NX]))
							.loadFace(CubemapFace.POSITIVE_Y, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.PY].length / 3, sides[EnvironmentMap.PY]))
							.loadFace(CubemapFace.NEGATIVE_Y, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.NY].length / 3, sides[EnvironmentMap.NY]))
							.loadFace(CubemapFace.POSITIVE_Z, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.PZ].length / 3, sides[EnvironmentMap.PZ]))
							.loadFace(CubemapFace.NEGATIVE_Z, NativeVectorBufferFactory.getInstance().createFromFloatArray(3, sides[EnvironmentMap.NZ].length / 3, sides[EnvironmentMap.NZ]))
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
				this.refSceneTexture = context.build2DColorTextureFromFile(
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
	}
	
	private void setupForDraw(Matrix4 view)
	{
		this.setupForDraw(this.mainDrawable.program(), view);
	}
	
	private void setupForDraw(Program<ContextType> program, Matrix4 view)
	{
		this.resources.setupShaderProgram(program, this.getSettingsModel().areTexturesEnabled());
		
		if (!this.settings.isRelightingEnabled() && this.settings.getWeightMode() == ShadingParameterMode.UNIFORM)
		{
			program.setUniform("perPixelWeightsEnabled", false);
			if (weightBuffer != null)
			{
				weightBuffer.close();
				weightBuffer = null;
			}
			weightBuffer = context.createUniformBuffer().setData(this.generateViewWeights(view));
			program.setUniformBuffer("ViewWeights", weightBuffer);
	    	program.setUniform("occlusionEnabled", false);
		}
		else
		{
			program.setUniform("perPixelWeightsEnabled", true);
			program.setUniform("weightExponent", this.settings.getWeightExponent());
	    	program.setUniform("isotropyFactor", this.settings.getIsotropyFactor());
	    	program.setUniform("occlusionEnabled", this.resources.depthTextures != null && this.settings.isOcclusionEnabled());
	    	program.setUniform("occlusionBias", this.settings.getOcclusionBias());
		}
    	
    	program.setUniform("renderGamma", this.settings.getGamma());
    	
    	program.setUniform("imageBasedRenderingEnabled", this.settings.isIBREnabled());
    	program.setUniform("relightingEnabled", this.settings.isRelightingEnabled());
    	program.setUniform("pbrGeometricAttenuationEnabled", this.settings.isPBRGeometricAttenuationEnabled());
    	program.setUniform("fresnelEnabled", this.settings.isFresnelEnabled());
    	program.setUniform("shadowsEnabled", this.settings.areShadowsEnabled());
    	
		program.setTexture("shadowMaps", shadowMaps);
		
		if (this.environmentMap == null || !lightModel.getEnvironmentMappingEnabled())
		{
			program.setUniform("useEnvironmentTexture", false);
			program.setTexture("environmentMap", null);
			this.environmentMapEnabled = false;
		}
		else
		{
			program.setUniform("useEnvironmentMap", true);
			program.setTexture("environmentMap", this.environmentMap);
			program.setUniform("environmentMipMapLevel", Math.max(0, Math.min(this.environmentMap.getMipmapLevelCount() - 2, 
					(int)Math.ceil(0.5 * 
						Math.log(6 * (double)this.environmentMap.getFaceSize() * 
								(double)this.environmentMap.getFaceSize() / (double)resources.viewSet.getCameraPoseCount() ) 
							/ Math.log(2.0)))));
			program.setUniform("diffuseEnvironmentMipMapLevel", this.environmentMap.getMipmapLevelCount() - 1);
			this.environmentMapEnabled = true;
		}
		
		program.setUniform("virtualLightCount", Math.min(4, lightModel.getLightCount()));
		
		program.setUniform("ambientColor", lightModel.getAmbientLightColor());
    	
    	this.clearColor = new Vector3(
    			(float)Math.pow(lightModel.getAmbientLightColor().x, 1.0 / this.getSettingsModel().getGamma()),
    			(float)Math.pow(lightModel.getAmbientLightColor().y, 1.0 / this.getSettingsModel().getGamma()),
    			(float)Math.pow(lightModel.getAmbientLightColor().z, 1.0 / this.getSettingsModel().getGamma()));
	}
	
	private void updateCentroidAndRadius()
	{
		Vector4 sumPositions = new Vector4(0.0f);
    	this.boundingRadius = resources.geometry.getBoundingRadius();
    	this.centroid = resources.geometry.getCentroid();
    	
    	if (multiTransformationModel != null)
    	{
    		for (Matrix4 m : multiTransformationModel)
    		{
    			Vector4 position = m.times(resources.geometry.getCentroid().asPosition());
    			sumPositions = sumPositions.plus(position);
    		}
    		
    		this.centroid = sumPositions.getXYZ().dividedBy(sumPositions.w);
    		
    		for(Matrix4 m : multiTransformationModel)
    		{
    			float distance = m.times(resources.geometry.getCentroid().asPosition()).getXYZ().distance(this.centroid);
    			this.boundingRadius = Math.max(this.boundingRadius, distance + resources.geometry.getBoundingRadius());
    		}
    	}
	}
	
	private float getScale()
	{
		 return resources.viewSet.getCameraPose(0)
				 .times(resources.geometry.getCentroid().asPosition())
			 .getXYZ().length()
			 * this.boundingRadius / this.resources.geometry.getBoundingRadius();
	}
	
	private Matrix4 getLightMatrix(int lightIndex)
	{
		float scale = getScale();
		return Matrix4.scale(scale)
			.times(lightModel.getLightMatrix(lightIndex))
			.times(Matrix4.scale(1.0f / scale))
			.times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().asMatrix4())
			.times(Matrix4.translate(this.centroid.negated()));
	}
	
	private Matrix4 getEnvironmentMapMatrix()
	{
		float scale = getScale();
		return Matrix4.scale(scale)
			.times(lightModel.getEnvironmentMapMatrix())
			.times(Matrix4.scale(1.0f / scale))
			.times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().asMatrix4())
			.times(Matrix4.translate(this.centroid.negated()));
	}
	
	private Matrix4 getLightProjection(int lightIndex)
	{
		Matrix4 lightMatrix = getLightMatrix(lightIndex);
		
		float lightDist = lightMatrix.times(this.centroid.asPosition()).getXYZ().length();
		
		float radius = (float)
			(resources.viewSet.getCameraPose(0).getUpperLeft3x3()
				.times(new Vector3(this.boundingRadius))
				.length() / Math.sqrt(3));
		
		return Matrix4.perspective(2.0f * (float)Math.atan(radius / lightDist) /*1.5f*/, 1.0f, 
				lightDist - radius,
				lightDist + radius);
	}
	
	private void generateShadowMaps(int lightIndex)
	{
		Matrix4 lightProj = getLightProjection(lightIndex);

		shadowProgram.setUniform("projection", lightProj);


		FramebufferAttachment<ContextType> attachment =shadowMaps.getLayerAsFramebufferAttachment(lightIndex);

		shadowFramebuffer.setDepthAttachment(attachment);
		shadowFramebuffer.clearDepthBuffer();
		
		for (Matrix4 m : this.multiTransformationModel)
		{
			shadowProgram.setUniform("model_view", getLightMatrix(lightIndex).times(m));
			shadowDrawable.draw(PrimitiveMode.TRIANGLES, shadowFramebuffer);
		}
	}
	
	private void setupLight(int lightIndex, int modelInstance)
	{
		Matrix4 lightMatrix = 
			getLightMatrix(lightIndex).times(this.multiTransformationModel.get(modelInstance));
		
		// lightMatrix can be hardcoded here (comment out previous line)
			
			// Contemporary gallery and stonewall
			//Matrix4.rotateY(16 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))
				
			// Color studio 2:
			//Matrix4.rotateY(6 * Math.PI / 16).times(Matrix4.rotateX(0 * Math.PI / 16))
			
			// For the synthetic falcon example?
			//Matrix4.rotateY(5 * Math.PI / 4).times(Matrix4.rotateX(-Math.PI / 4))
				
			// Always end with this when hardcoding:
			//	.times(new Matrix4(new Matrix3(resources.viewSet.getCameraPose(0))));
		
		Vector3 lightPos = lightMatrix.quickInverse(0.001f).times(Vector4.ORIGIN).getXYZ();
		
		program.setUniform("lightPosVirtual[" + lightIndex + "]", lightPos);
		
		Vector3 controllerLightIntensity = lightModel.getLightColor(lightIndex);
		float lightDistance = getLightMatrix(lightIndex).times(this.centroid.asPosition()).getXYZ().length();

		float scale = resources.viewSet.areLightSourcesInfinite() ? 1.0f :
				resources.viewSet.getCameraPose(0)
						.times(resources.geometry.getCentroid().asPosition())
					.getXYZ().length();
		
		program.setUniform("lightIntensityVirtual[" + lightIndex + "]", 
				controllerLightIntensity.times(lightDistance * lightDistance * resources.viewSet.getLightIntensity(0).y / (scale * scale)));
		program.setUniform("lightMatrixVirtual[" + lightIndex + "]", getLightProjection(lightIndex).times(lightMatrix));
	}
	
	private Matrix4 getPartialViewMatrix()
	{
		float scale = getScale();
		
		return Matrix4.scale(scale)
    			.times(cameraModel.getLookMatrix())
    			.times(Matrix4.scale(1.0f / scale));
	}
	
	private Matrix4 getPartialViewMatrix(Matrix4 viewMatrix)
	{
		return viewMatrix
    			.times(Matrix4.translate(this.centroid))
    			.times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().transpose().asMatrix4());
	}
	
	private Matrix4 getViewMatrix()
	{
    	return getPartialViewMatrix()
    			.times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().asMatrix4())
    			.times(Matrix4.translate(this.centroid.negated()));
	}
	
	private Matrix4 getModelViewMatrix(Matrix4 partialViewMatrix, int modelInstance)
	{
		float scale = getScale();
		
		return partialViewMatrix
				.times(Matrix4.scale(scale))
				.times(this.objectModel.getTransformationMatrix())
    			.times(Matrix4.scale(1.0f / scale))
    			.times(resources.viewSet.getCameraPose(0).getUpperLeft3x3().asMatrix4())
    			.times(Matrix4.translate(this.centroid.negated()))
				.times(multiTransformationModel.get(modelInstance));
	}
	
	private Matrix4 getProjectionMatrix(FramebufferSize size)
	{
		float scale = getScale();
		
		return Matrix4.perspective(
    			//(float)(1.0),
    			resources.viewSet.getCameraProjection(
    					resources.viewSet.getCameraProjectionIndex(resources.viewSet.getPrimaryViewIndex()))
					.getVerticalFieldOfView(), 
    			(float)size.width / (float)size.height, 
    			0.01f * scale, 100.0f * scale);
	}

	private void drawReferenceScene(Program<ContextType> program, Framebuffer<ContextType> framebuffer, Matrix4 view)
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
    		program.setUniform("model_view", view);
			program.setUniform("viewPos", view.quickInverse(0.01f).getColumn(3).getXYZ());
        	
			// Do first pass at half resolution to off-screen buffer
    		drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
    	}
	}
	
	private NativeVectorBuffer generateViewWeights(Matrix4 view)
	{
		float[] viewWeights = new float[this.resources.viewSet.getCameraPoseCount()];
		float viewWeightSum = 0.0f;
		
		for (int i = 0; i < viewWeights.length; i++)
		{
			Vector3 viewDir = this.resources.viewSet.getCameraPose(i).times(this.resources.geometry.getCentroid().asPosition()).getXYZ().negated().normalized();
			Vector3 targetDir = this.resources.viewSet.getCameraPose(i).times(
					view.quickInverse(0.01f).getColumn(3)
						.minus(this.resources.geometry.getCentroid().asPosition())).getXYZ().normalized();
			
			viewWeights[i] = 1.0f / (float)Math.max(0.000001, 1.0 - Math.pow(Math.max(0.0, targetDir.dot(viewDir)), this.settings.getWeightExponent())) - 1.0f;
			viewWeightSum += viewWeights[i];
		}
		
		for (int i = 0; i < viewWeights.length; i++)
		{
			viewWeights[i] /= Math.max(0.01, viewWeightSum);
		}
		
		return NativeVectorBufferFactory.getInstance().createFromFloatArray(1, viewWeights.length, viewWeights);
	}
	
	@Override
	public void draw(Framebuffer<ContextType> framebuffer, Matrix4 view, Matrix4 projection) 
	{
		boolean customViewMatrix = (view != null);
    	
		try
		{
			if(multisamplingEnabled)
			{
				context.getState().enableMultisampling();
			}
			else
			{
				context.getState().disableMultisampling();			
			}
	    	
	    	context.getState().enableBackFaceCulling();
	    	
	    	if (!customViewMatrix)
	    	{
	    		view = this.getViewMatrix();
	    		partialViewMatrix = getPartialViewMatrix();
	    	}
	    	else
	    	{
	    		partialViewMatrix = getPartialViewMatrix(view);
	    		
	    		if (lightModel instanceof CameraBasedLightModel)
				{
			    	float scale = resources.viewSet.getCameraPose(0)
			    			.times(resources.geometry.getCentroid().asPosition())
		    			.getXYZ().length();
					
					((CameraBasedLightModel)lightModel).overrideCameraPose(
							Matrix4.scale(1.0f / scale)
								.times(view)
								.times(Matrix4.translate(resources.geometry.getCentroid()))
				    			.times(resources.viewSet.getCameraPose(0).transpose().getUpperLeft3x3().asMatrix4())
								.times(Matrix4.scale(scale)));
				}
	    	}
	    	
	    	FramebufferSize size = framebuffer.getSize();
	    	
	    	if (projection == null)
	    	{
	    		projection = this.getProjectionMatrix(size);
	    	}
	    	
	    	this.setupForDraw(view);
	    	
	    	mainDrawable.program().setUniform("projection", projection);
	    	
			Matrix4 envMapMatrix = this.getEnvironmentMapMatrix();
	    	
	    	if (environmentMap != null && environmentMapEnabled)
			{
				environmentBackgroundProgram.setUniform("objectID", this.sceneObjectIDLookup.get("EnvironmentMap"));
				environmentBackgroundProgram.setUniform("useEnvironmentTexture", true);
				environmentBackgroundProgram.setTexture("env", environmentMap);
				environmentBackgroundProgram.setUniform("model_view", view);
				environmentBackgroundProgram.setUniform("projection", projection);
				environmentBackgroundProgram.setUniform("envMapMatrix", envMapMatrix);
				environmentBackgroundProgram.setUniform("envMapIntensity", this.clearColor);
				

				environmentBackgroundProgram.setUniform("gamma", 
						environmentMap.isInternalFormatCompressed() || 
						environmentMap.getInternalUncompressedColorFormat().dataType != DataType.FLOATING_POINT 
						? 1.0f : 2.2f);
			}
	    	
	    	int fboWidth = size.width;
	        int fboHeight = size.height;
	        
			if (halfResEnabled)
			{
				fboWidth /= 2;
				fboHeight /= 2;
			}
	    	
	        try
	        (
	        	FramebufferObject<ContextType> offscreenFBO = context.buildFramebufferObject(fboWidth, fboHeight)
						.addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.RGB8)
							.setLinearFilteringEnabled(true))
						.addColorAttachment(ColorAttachmentSpec.createWithInternalFormat(ColorFormat.R32I))
						.addDepthAttachment(DepthAttachmentSpec.createFixedPointWithPrecision(32))
						.createFramebufferObject()
			)
	        {
				offscreenFBO.clearColorBuffer(0, clearColor.x, clearColor.y, clearColor.z, 1.0f);
				offscreenFBO.clearIntegerColorBuffer(1, 0, 0, 0, 0);
		    	offscreenFBO.clearDepthBuffer();
	    		
	    		if (environmentMap != null && environmentMapEnabled)
	    		{
	    			context.getState().disableDepthTest();
	    			this.environmentBackgroundDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
	    			context.getState().enableDepthTest();
	    		}
				
	    		if (shadowMaps.getDepth() < lightModel.getLightCount())
	    		{
	    			shadowMaps.close();
	    			shadowMaps = null;
	    			shadowMaps = context.build2DDepthTextureArray(2048, 2048, lightModel.getLightCount()).createTexture();
	    		}
	    		
	    		for (int lightIndex = 0; lightIndex < lightModel.getLightCount(); lightIndex++)
				{
					generateShadowMaps(lightIndex);
				}
	
				this.program.setUniform("imageBasedRenderingEnabled", false);
				this.program.setUniform("objectID", this.sceneObjectIDLookup.get("SceneObject"));
				this.drawReferenceScene(this.program, offscreenFBO, view);
				
				this.program.setUniform("imageBasedRenderingEnabled", this.settings.isIBREnabled());
				setupForDraw(view); // changed anything changed when drawing the reference scene.
				this.program.setUniform("objectID", this.sceneObjectIDLookup.get("IBRObject"));
				
				this.program.setUniform("envMapMatrix", envMapMatrix);
				
				for (int modelInstance = 0; modelInstance < multiTransformationModel.size(); modelInstance++)
				{
					for (int lightIndex = 0; lightIndex < lightModel.getLightCount(); lightIndex++)
					{
						setupLight(lightIndex, modelInstance);
					}
					
					// Draw instance
					Matrix4 modelView = getModelViewMatrix(partialViewMatrix, modelInstance);
					this.program.setUniform("model_view", modelView);
					this.program.setUniform("viewPos", modelView.quickInverse(0.01f).getColumn(3).getXYZ());
			    	
			    	// Render to off-screen buffer
			        mainDrawable.draw(PrimitiveMode.TRIANGLES, offscreenFBO);
				}
				
				if (this.getSettingsModel().isRelightingEnabled() && this.getSettingsModel().areVisibleLightsEnabled())
				{
					// Draw lights
					for (int i = 0; i < lightModel.getLightCount(); i++)
					{
						context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
						context.getState().disableDepthWrite();
						
						if (lightModel.isLightWidgetEnabled(i))
						{
							this.lightProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light" + (i + 1) + ".Target"));
							
							Vector3 lightTarget = partialViewMatrix.times(this.lightModel.getLightCenter(i).times(this.getScale()).asPosition()).getXYZ();
							
							this.lightProgram.setUniform("model_view",
		//							modelView.times(this.getLightMatrix(i).quickInverse(0.001f)));
								Matrix4.translate(lightTarget)
									.times(Matrix4.scale(-lightTarget.z / 64.0f, -lightTarget.z / 64.0f, 1.0f)));
							this.lightProgram.setUniform("projection", this.getProjectionMatrix(size));
							
				    		this.lightProgram.setTexture("lightTexture", this.lightTargetTexture);
				    		
				    		this.context.getState().disableDepthTest();
							this.lightProgram.setUniform("color", new Vector3(0.5f,0,0.0f) /* dark red */);
							this.lightDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);

				    		this.context.getState().enableDepthTest();
							context.getState().disableAlphaBlending();
							this.lightProgram.setUniform("color", new Vector3(1.0f,0,1.0f) /* magenta */);
							this.lightDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
							
							this.widgetProgram.setUniform("projection", this.getProjectionMatrix(size));
							
							Matrix4 widgetTransformation = view.times(this.getLightMatrix(i).quickInverse(0.001f));

							Vector3 lightPosition = widgetTransformation.getColumn(3).getXYZ();
							
				    		this.context.getState().disableDepthTest();
							context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
							
							Vector3 lineEndpoint = lightPosition.minus(lightTarget).times(0.5f / lightPosition.getXY().distance(lightTarget.getXY())).minus(lightTarget);
							
							try
							(
								VertexBuffer<ContextType> line = 
									context.createVertexBuffer()
										.setData(NativeVectorBufferFactory.getInstance()
											.createFromFloatArray(3, 2, new float[] 
											{
												lineEndpoint.x, lineEndpoint.y, lineEndpoint.z, 
												lightTarget.x, lightTarget.y, lightTarget.z
											}));
							)
							{
								Drawable<ContextType> lineRenderable = context.createDrawable(this.widgetProgram);
								lineRenderable.addVertexBuffer("position", line);
								this.widgetProgram.setUniform("model_view", Matrix4.IDENTITY);
								this.widgetProgram.setUniform("color", new Vector4(1));
								this.widgetProgram.setUniform("objectID", 0);
								lineRenderable.draw(PrimitiveMode.LINES, offscreenFBO);
							}
							
							Vector4 arrow1DirectionY = widgetTransformation.times(new Vector4(1,0,0,0)).getXY().normalized().asDirection();
							Vector4 arrow2DirectionY = widgetTransformation.times(new Vector4(0,1,0,0)).getXY().normalized().asDirection();
							Vector4 arrow3DirectionY = widgetTransformation.times(new Vector4(0,0,1,0)).getXY().normalized().asDirection();
							
							Vector4 arrow1DirectionX = new Vector4(arrow1DirectionY.y, -arrow1DirectionY.x, 0, 0).normalized();
							Vector4 arrow2DirectionX = new Vector4(arrow2DirectionY.y, -arrow2DirectionY.x, 0, 0).normalized();
							Vector4 arrow3DirectionX = new Vector4(arrow3DirectionY.y, -arrow3DirectionY.x, 0, 0).normalized();
							
							Vector3 arrow1PositionR = lightPosition.plus(arrow1DirectionY.getXYZ().normalized().times(-lightPosition.z / 16.0f));
							Vector3 arrow1PositionL = lightPosition.minus(arrow1DirectionY.getXYZ().normalized().times(-lightPosition.z / 16.0f));
							Vector3 arrow2PositionR = lightPosition.plus(arrow2DirectionY.getXYZ().normalized().times(-lightPosition.z / 16.0f));
							Vector3 arrow2PositionL = lightPosition.minus(arrow2DirectionY.getXYZ().normalized().times(-lightPosition.z / 16.0f));
							Vector3 arrow3PositionR = lightPosition.plus(arrow3DirectionY.getXYZ().normalized().times(-lightPosition.z / 16.0f));
							Vector3 arrow3PositionL = lightPosition.minus(arrow3DirectionY.getXYZ().normalized().times(-lightPosition.z / 16.0f));
							
//							Vector3 arrow1PositionR = widgetTransformation.times(new Vector4(1,0,0,1)).getXYZ();
//							Vector3 arrow1PositionL = widgetTransformation.times(new Vector4(-1,0,0,1)).getXYZ();
//							Vector3 arrow2PositionR = widgetTransformation.times(new Vector4(0,1,0,1)).getXYZ();
//							Vector3 arrow2PositionL = widgetTransformation.times(new Vector4(0,-1,0,1)).getXYZ();
//							Vector3 arrow3PositionR = widgetTransformation.times(new Vector4(0,0,1,1)).getXYZ();
//							Vector3 arrow3PositionL = widgetTransformation.times(new Vector4(0,0,-1,1)).getXYZ();
							
							this.widgetProgram.setUniform("model_view",
								Matrix4.translate(arrow1PositionR)
									.times(Matrix4.scale(-lightPosition.z / 64.0f, -lightPosition.z / 64.0f, 1.0f))
									.times(Matrix4.fromColumns(
											arrow1DirectionX, 
											arrow1DirectionY, 
											new Vector4(0,0,1,0), 
											new Vector4(0,0,0,1))));
							this.widgetProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light" + (i + 1) + ".AzimuthUp"));
							
				    		this.context.getState().disableDepthTest();
							context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
							this.widgetProgram.setUniform("color", new Vector4(0.0f, 0.5f, 0.0f, 1) /* green */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);

				    		this.context.getState().enableDepthTest();
							context.getState().disableAlphaBlending();
							this.widgetProgram.setUniform("color", new Vector4(0.0f, 1.0f, 1.0f, 1) /* cyan */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
							
							this.widgetProgram.setUniform("model_view",
								Matrix4.translate(arrow1PositionL)
									.times(Matrix4.scale(-lightPosition.z / 64.0f, -lightPosition.z / 64.0f, 1.0f))
									.times(Matrix4.fromColumns(
											arrow1DirectionX.negated(), 
											arrow1DirectionY.negated(), 
											new Vector4(0,0,1,0), 
											new Vector4(0,0,0,1))));
							this.widgetProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light" + (i + 1) + ".AzimuthDown"));
							
				    		this.context.getState().disableDepthTest();
							context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
							this.widgetProgram.setUniform("color", new Vector4(0.0f, 0.5f, 0.0f, 1) /* green */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);

				    		this.context.getState().enableDepthTest();
							context.getState().disableAlphaBlending();
							this.widgetProgram.setUniform("color", new Vector4(0.0f, 1.0f, 1.0f, 1) /* cyan */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
							
							
							this.widgetProgram.setUniform("model_view",
									Matrix4.translate(arrow2PositionR)
										.times(Matrix4.scale(-lightPosition.z / 64.0f, -lightPosition.z / 64.0f, 1.0f))
										.times(Matrix4.fromColumns(
												arrow2DirectionX, 
												arrow2DirectionY, 
												new Vector4(0,0,1,0), 
												new Vector4(0,0,0,1))));
							this.widgetProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light" + (i + 1) + ".InclinationUp"));
								
				    		this.context.getState().disableDepthTest();
							context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
							this.widgetProgram.setUniform("color", new Vector4(0.25f, 0.25f, 0.0f, 1) /* dark yellow / gold */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);

				    		this.context.getState().enableDepthTest();
							context.getState().disableAlphaBlending();
							this.widgetProgram.setUniform("color", new Vector4(1.0f, 0.5f, 0.0f, 1) /* orange */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
							
							this.widgetProgram.setUniform("model_view",
								Matrix4.translate(arrow2PositionL)
									.times(Matrix4.scale(-lightPosition.z / 64.0f, -lightPosition.z / 64.0f, 1.0f))
									.times(Matrix4.fromColumns(
											arrow2DirectionX.negated(), 
											arrow2DirectionY.negated(), 
											new Vector4(0,0,1,0), 
											new Vector4(0,0,0,1))));
							this.widgetProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light" + (i + 1) + ".InclinationDown"));

				    		this.context.getState().disableDepthTest();
							context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
							this.widgetProgram.setUniform("color", new Vector4(0.25f, 0.25f, 0.0f, 1) /* dark yellow / gold */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);

				    		this.context.getState().enableDepthTest();
							context.getState().disableAlphaBlending();
							this.widgetProgram.setUniform("color", new Vector4(1.0f, 0.5f, 0.0f, 1) /* orange */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
							
							
							this.widgetProgram.setUniform("model_view",
								Matrix4.translate(arrow3PositionR)
									.times(Matrix4.scale(-lightPosition.z / 64.0f, -lightPosition.z / 64.0f, 1.0f))
									.times(Matrix4.fromColumns(
											arrow3DirectionX, 
											arrow3DirectionY, 
											new Vector4(0,0,1,0), 
											new Vector4(0,0,0,1))));
							this.widgetProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light" + (i + 1) + ".DistanceUp"));
							
				    		this.context.getState().disableDepthTest();
							context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
							this.widgetProgram.setUniform("color", new Vector4(0.0f, 0.0f, 0.5f, 1) /* blue */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);

				    		this.context.getState().enableDepthTest();
							context.getState().disableAlphaBlending();
							this.widgetProgram.setUniform("color", new Vector4(0.5f, 0.0f, 1.0f, 1) /* lavender(?) */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
							
							this.widgetProgram.setUniform("model_view",
								Matrix4.translate(arrow3PositionL)
									.times(Matrix4.scale(-lightPosition.z / 64.0f, -lightPosition.z / 64.0f, 1.0f))
									.times(Matrix4.fromColumns(
											arrow3DirectionX.negated(), 
											arrow3DirectionY.negated(), 
											new Vector4(0,0,1,0), 
											new Vector4(0,0,0,1))));
							this.widgetProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light" + (i + 1) + ".DistanceDown"));
							
				    		this.context.getState().disableDepthTest();
							context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
							this.widgetProgram.setUniform("color", new Vector4(0.0f, 0.0f, 0.5f, 1) /* blue */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);

				    		this.context.getState().enableDepthTest();
							context.getState().disableAlphaBlending();
							this.widgetProgram.setUniform("color", new Vector4(0.5f, 0.0f, 1.0f, 1) /* lavender(?) */);
							this.widgetDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
						}
						
						if (lightModel.isLightVisualizationEnabled(i))
						{
							this.context.getState().enableDepthTest();
							context.getState().setAlphaBlendingFunction(new AlphaBlendingFunction(Weight.ONE, Weight.ONE));
							
							this.lightProgram.setUniform("objectID", this.sceneObjectIDLookup.get("Light" + (i + 1)));
							this.lightProgram.setUniform("color", lightModel.getLightColor(i));
							
							Vector3 lightPosition = view.times(this.getLightMatrix(i).quickInverse(0.001f)).getColumn(3).getXYZ();
							
							this.lightProgram.setUniform("model_view",
		
		//							modelView.times(this.getLightMatrix(i).quickInverse(0.001f)));
								Matrix4.translate(lightPosition)
									.times(Matrix4.scale(-lightPosition.z / 16.0f, -lightPosition.z / 16.0f, 1.0f)));
							this.lightProgram.setUniform("projection", this.getProjectionMatrix(size));
				    		this.lightProgram.setTexture("lightTexture", this.lightTexture);
							this.lightDrawable.draw(PrimitiveMode.TRIANGLE_FAN, offscreenFBO);
						}
					}
					
					context.getState().disableAlphaBlending();
					context.getState().enableDepthWrite();
					this.context.getState().enableDepthTest();
				}
				
				// Finish drawing
				context.flush();
			        
		        // Second pass at full resolution to default framebuffer
		    	simpleTexDrawable.program().setTexture("tex", offscreenFBO.getColorAttachmentTexture(0));    	

	    		framebuffer.clearDepthBuffer();
		    	simpleTexDrawable.draw(PrimitiveMode.TRIANGLE_FAN, framebuffer);
		    	
		    	context.flush();

		    	pixelObjectIDs = offscreenFBO.readIntegerColorBufferRGBA(1);
		    	pixelDepths = offscreenFBO.readDepthBuffer();
		    	fboSize = offscreenFBO.getSize();
			}
		}
		catch(Exception e)
		{
			if (!suppressErrors)
			{
				e.printStackTrace();
				suppressErrors = true; // Prevent excessive errors
			}
		}
		finally
		{
			if (customViewMatrix && lightModel instanceof CameraBasedLightModel)
			{
				((CameraBasedLightModel)lightModel).removeCameraPoseOverride();
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

		if (weightBuffer != null)
		{
			weightBuffer.close();
		}
	}
	
	@Override
	public void setOnLoadCallback(LoadingMonitor callback) 
	{
		this.callback = callback;
	}
	
	@Override
	public VertexGeometry getActiveGeometry()
	{
		return this.resources.geometry;
	}
	
	@Override
	public ViewSet getActiveViewSet()
	{
		return this.resources.viewSet;
	}

	@Override
	public IBRSettingsModel getSettingsModel()
	{
		return this.settings;
	}

	@Override
	public void setSettingsModel(IBRSettingsModel ibrSettings2) {
		this.settings = ibrSettings2;
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
		if (environmentFile == null && this.environmentMap != null)
		{
			this.environmentMapUnloadRequested = true;
		}
		else if (environmentFile != null && environmentFile.exists())
		{
			this.newEnvironmentFile = environmentFile;
		}
		else {
			System.out.println("TEMP FIX IN IBRImplementation.setEnvironment");;
//			this.environmentMapUnloadRequested = true;//TODO this is a temp fix
		}
	}

	@Override
	public void setBackplate(File backplateFile) 
	{
		// TODO Auto-generated method stub
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
    		
    		newProgram = context.getShaderProgramBuilder()
    				.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
    				.addShader(ShaderType.FRAGMENT, new File("shaders/common/solid.frag"))
    				.createProgram();
    		
    		if (this.widgetProgram != null)
    		{
    			this.widgetProgram.close();
    		}
    		
    		this.widgetProgram = newProgram;
    		
    		this.widgetDrawable = context.createDrawable(this.widgetProgram);
    		this.widgetDrawable.addVertexBuffer("position", widgetVertices);
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
	}

	@Override
	public void setMultiTransformationModel(List<Matrix4> multiTransformationModel) 
	{
		if (multiTransformationModel != null)
		{
			this.multiTransformationModel = multiTransformationModel;
		}
	}
	
	@Override
	public void setReferenceScene(VertexGeometry scene)
	{
		this.referenceScene = scene;
		this.referenceSceneChanged = true;
	}

	@Override
	public SceneViewportModel getSceneViewportModel() 
	{
		return new SceneViewportModel()
		{
			@Override
			public Object getObjectAtCoordinates(double x, double y)
			{
				double xRemapped = Math.min(Math.max(x, 0), 1);
				double yRemapped = 1.0 - Math.min(Math.max(y, 0), 1);
				
				int index = 4 * (int)(Math.round(fboSize.height * yRemapped) * fboSize.width + Math.round(fboSize.width * xRemapped));
				return sceneObjectNameList.get(pixelObjectIDs[index]);
			}

			@Override
			public Vector3 get3DPositionAtCoordinates(double x, double y) 
			{
				double xRemapped = Math.min(Math.max(x, 0), 1);
				double yRemapped = 1.0 - Math.min(Math.max(y, 0), 1);
				
				int index = (int)(Math.round(fboSize.height * yRemapped) * fboSize.width + Math.round(fboSize.width * xRemapped));
				
				Matrix4 projection = getProjectionMatrix(fboSize);
				Matrix4 projectionInverse = Matrix4.fromRows(
						new Vector4(1.0f / projection.get(0, 0), 0, 0, 0),
						new Vector4(0, 1.0f / projection.get(1, 1), 0, 0),
						new Vector4(0, 0, 0, -1),
						new Vector4(0, 0, 1.0f, projection.get(2, 2))
							.dividedBy(projection.get(2, 3)));
				
				// Transform from screen space into world space
				Vector4 unscaledPosition = projectionInverse
					.times(new Vector4((float)(2 * x - 1), (float)(1 - 2 * y), 2 * (float)(0x0000FFFF & pixelDepths[index]) / (float)0xFFFF - 1, 1.0f));
				
				return getPartialViewMatrix().quickInverse(0.01f)
						.times(unscaledPosition.getXYZ().dividedBy(unscaledPosition.w).asPosition())
						.getXYZ().dividedBy(getScale());
			};
		};
	}

	@Override
	public void setObjectModel(ReadonlyObjectModel objectModel) 
	{
		this.objectModel = objectModel;
	}

	@Override
	public void setCameraModel(ReadonlyCameraModel cameraModel) 
	{
		this.cameraModel = cameraModel;
	}

	@Override
	public void setLightModel(ReadonlyLightModel lightModel) 
	{
		this.lightModel = lightModel;
	}
}
