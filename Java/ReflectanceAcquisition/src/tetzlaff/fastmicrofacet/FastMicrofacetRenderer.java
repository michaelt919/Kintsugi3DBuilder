package tetzlaff.fastmicrofacet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import tetzlaff.gl.Context;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.LightController;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.ulf.ViewSet;

public class FastMicrofacetRenderer<ContextType extends Context<ContextType>> implements Drawable 
{
	private Program<ContextType> program;
	private Program<ContextType> shadowProgram;
	
	private ContextType context;
	private File objFile;
	private CameraController cameraController;
	private LightController lightController;
	
	private VertexMesh mesh;
	
	private VertexBuffer<ContextType> positionBuffer;
	private VertexBuffer<ContextType> texCoordBuffer;
	private VertexBuffer<ContextType> normalBuffer;
	
	private Texture2D<ContextType> diffuse;
	private Texture2D<ContextType> normal;
	private Texture2D<ContextType> specular;
	private Texture2D<ContextType> roughness;
	
	private Renderable<ContextType> renderable;
	private Renderable<ContextType> shadowRenderable;
	
	private FramebufferObject<ContextType> shadowBuffer;
	
	public FastMicrofacetRenderer(ContextType context, File objFile, CameraController viewTrackball, LightController lightTrackball) 
	{
		this.context = context;
    	this.objFile = objFile;
    	this.cameraController = viewTrackball;
    	this.lightController = lightTrackball;
	}

	@Override
	public void initialize() 
	{
		try
        {
    		this.program = context.getShaderProgramBuilder()
					.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
					.addShader(ShaderType.FRAGMENT, new File("shaders/fastmicrofacet/fastmicrofacet.frag"))
					.createProgram();
    		
    		this.shadowProgram = context.getShaderProgramBuilder()
					.addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
					.addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
					.createProgram();

			this.mesh = new VertexMesh("OBJ", objFile);
			
			this.positionBuffer = context.createVertexBuffer().setData(this.mesh.getVertices());
			this.texCoordBuffer = context.createVertexBuffer().setData(this.mesh.getTexCoords());
			this.normalBuffer = context.createVertexBuffer().setData(this.mesh.getNormals());
			
			File texturePath = new File(objFile.getParentFile(), "textures");
			this.diffuse = context.get2DColorTextureBuilder(new File(texturePath, "diffuse.png"), true).createTexture();
			this.normal = context.get2DColorTextureBuilder(new File(texturePath, "normal.png"), true).createTexture();
			this.specular = context.get2DColorTextureBuilder(new File(texturePath, "specular.png"), true).createTexture();
			this.roughness = context.get2DColorTextureBuilder(new File(texturePath, "roughness.png"), true).createTexture();
			
			this.renderable = context.createRenderable(this.program);
			this.renderable.addVertexBuffer("position", this.positionBuffer);
			this.renderable.addVertexBuffer("texCoord", this.texCoordBuffer);
			this.renderable.addVertexBuffer("normal", this.normalBuffer);

			this.shadowRenderable = context.createRenderable(this.shadowProgram);
			this.shadowRenderable.addVertexBuffer("position", this.positionBuffer);
			this.shadowRenderable.addVertexBuffer("texCoord", this.texCoordBuffer);
			this.shadowRenderable.addVertexBuffer("normal", this.normalBuffer);
			this.shadowBuffer = context.getFramebufferObjectBuilder(4096, 4096).addDepthAttachment().createFramebufferObject();
		} 
		catch (RuntimeException e)
		{
			e.printStackTrace();
		}
    	catch (IOException e) 
    	{
			e.printStackTrace();
		}
	}

	@Override
	public void update() 
	{
	}

	@Override
	public void draw() 
	{
		try
		{
	    	Matrix4 modelView = cameraController.getViewMatrix() // Trackball
					.times(Matrix4.translate(mesh.getCentroid().negated())); // Model
			
	    	Vector3 lightPositions[] = new Vector3[lightController.getLightCount()];
	    	for (int i = 0; i < lightController.getLightCount(); i++)
			{
	    		Matrix4 lightMatrix = lightController.getLightMatrix(i)
	        			.times(Matrix4.translate(mesh.getCentroid().negated()));
	    		
	    		lightPositions[i] = new Matrix3(lightMatrix).transpose().times(new Vector3(lightMatrix.getColumn(3).negated()));
			}
			
	//		this.shadowBuffer.clearDepthBuffer();
	//    	
	//    	this.shadowRenderable.program().setUniform("model_view", lightMatrix);
	//    	this.shadowRenderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 2, 1.0f, 1.0f, 9.0f));
	//    	this.shadowRenderable.draw(PrimitiveMode.TRIANGLES, this.shadowBuffer);
			
			Framebuffer<ContextType> framebuffer = context.getDefaultFramebuffer();
	    	
	    	FramebufferSize size = framebuffer.getSize();
	    	
	    	float gamma = 2.2f;
	    	Vector3 ambientColor = new Vector3(0.01f, 0.01f, 0.01f);
	    	
	    	Vector3 clearColor = new Vector3(
	    			(float)Math.pow(ambientColor.x, 1.0 / gamma),
	    			(float)Math.pow(ambientColor.y, 1.0 / gamma),
	    			(float)Math.pow(ambientColor.z, 1.0 / gamma));
	    	
	    	framebuffer.clearColorBuffer(0, clearColor.x, clearColor.y, clearColor.z, 1.0f);
	    	framebuffer.clearDepthBuffer();
	
	    	this.renderable.program().setTexture("diffuse", this.diffuse);
	    	this.renderable.program().setTexture("normal", this.normal);
	    	this.renderable.program().setTexture("specular", this.specular);
	    	this.renderable.program().setTexture("roughness", this.roughness);
	//    	this.renderable.program().setTexture("shadow", this.shadowBuffer.getDepthAttachmentTexture());
	    	
	    	this.renderable.program().setUniform("model_view", modelView);
	    	this.renderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 4, (float)size.width / (float)size.height, 0.01f, 100.0f));
	//    	this.renderable.program().setUniform("lightMatrix", Matrix4.perspective((float)Math.PI / 2, 1.0f, 1.0f, 9.0f).times(lightMatrix));
	    	
	    	// TODO settings UI
	    	this.renderable.program().setUniform("gamma", 2.2f);
	    	this.renderable.program().setUniform("shadowBias", 0.001f);
	    	this.renderable.program().setUniform("ambientColor", ambientColor);
	    	this.renderable.program().setUniform("lightColors[0]", lightController.getLightColor(0));
	    	this.renderable.program().setUniform("lightColors[1]", lightController.getLightColor(1));
	    	this.renderable.program().setUniform("lightColors[2]", lightController.getLightColor(2));
	    	this.renderable.program().setUniform("lightColors[3]", lightController.getLightColor(3));
	    	this.renderable.program().setUniform("lightPositions[0]", lightPositions[0]);
	    	this.renderable.program().setUniform("lightPositions[1]", lightPositions[1]);
	    	this.renderable.program().setUniform("lightPositions[2]", lightPositions[2]);
	    	this.renderable.program().setUniform("lightPositions[3]", lightPositions[3]);
	    	
	    	this.renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
		}
		catch (RuntimeException e)
		{
			e.printStackTrace(System.err);
		}
		
	}

	@Override
	public void cleanup() 
	{
		this.program.delete();
		this.shadowProgram.delete();
		
		this.positionBuffer.delete();
		this.texCoordBuffer.delete();
		this.normalBuffer.delete();
		
		this.diffuse.delete();
		this.normal.delete();
		this.specular.delete();
		this.roughness.delete();
		
		this.shadowBuffer.delete();
	}
	
	public void reloadShaders()
	{
		try
        {
			if (this.program != null)
			{
				this.program.delete();
			}
			
			if (this.shadowProgram != null)
			{
				this.shadowProgram.delete();
			}
			
			this.program = null;
			this.shadowProgram = null;
			
    		this.program = context.getShaderProgramBuilder()
					.addShader(ShaderType.VERTEX, new File("shaders/common/imgspace.vert"))
					.addShader(ShaderType.FRAGMENT, new File("shaders/fastmicrofacet/fastmicrofacet.frag"))
					.createProgram();
    		
    		this.shadowProgram = context.getShaderProgramBuilder()
					.addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
					.addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
					.createProgram();
			
			this.renderable = context.createRenderable(this.program);
			this.renderable.addVertexBuffer("position", this.positionBuffer);
			this.renderable.addVertexBuffer("texCoord", this.texCoordBuffer);
			this.renderable.addVertexBuffer("normal", this.normalBuffer);

			this.shadowRenderable = context.createRenderable(this.shadowProgram);
			this.shadowRenderable.addVertexBuffer("position", this.positionBuffer);
			this.shadowRenderable.addVertexBuffer("texCoord", this.texCoordBuffer);
			this.shadowRenderable.addVertexBuffer("normal", this.normalBuffer);
		} 
    	catch (IOException e) 
    	{
			e.printStackTrace();
		}
	}

	public void resample(File targetVSETFile, File exportPath) throws IOException
	{
		ViewSet<ContextType> targetViewSet = ViewSet.loadFromVSETFile(targetVSETFile, context);
		
		BufferedImage img = ImageIO.read(targetViewSet.getImageFile(0));
		FramebufferObject<ContextType> framebuffer = context.getFramebufferObjectBuilder(img.getWidth(), img.getHeight()).createFramebufferObject();
    	
    	for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		{
	    	renderable.program().setUniform("model_view", targetViewSet.getCameraPose(i));
	    	renderable.program().setUniform("projection", 
    			targetViewSet.getCameraProjection(targetViewSet.getCameraProjectionIndex(i))
    				.getProjectionMatrix(targetViewSet.getRecommendedNearPlane(), targetViewSet.getRecommendedFarPlane()));
	    	
	    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
	    	framebuffer.clearDepthBuffer();
	    	
	    	renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
	    	
	    	File exportFile = new File(exportPath, targetViewSet.getImageFileName(i));
	    	exportFile.getParentFile().mkdirs();
	        framebuffer.saveColorBufferToFile(0, "PNG", exportFile);
	        
	        System.out.println("Wrote image " + i);
		}
	}
}
