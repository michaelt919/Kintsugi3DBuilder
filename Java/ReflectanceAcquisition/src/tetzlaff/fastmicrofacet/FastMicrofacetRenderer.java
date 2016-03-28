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
	private CameraController viewTrackball;
	private CameraController lightTrackball;
	
	private VertexMesh mesh;
	
	private VertexBuffer<ContextType> positionBuffer;
	private VertexBuffer<ContextType> texCoordBuffer;
	private VertexBuffer<ContextType> normalBuffer;
	
	private Texture2D<ContextType> diffuse;
	private Texture2D<ContextType> normal;
	private Texture2D<ContextType> specular;
	private Texture3D<ContextType> microfacetDistribution;
	
	private Renderable<ContextType> renderable;
	private Renderable<ContextType> shadowRenderable;
	
	private FramebufferObject<ContextType> shadowBuffer;
	
	public FastMicrofacetRenderer(ContextType context, File objFile, CameraController viewTrackball, CameraController lightTrackball) 
	{
		this.context = context;
    	this.objFile = objFile;
    	this.viewTrackball = viewTrackball;
    	this.lightTrackball = lightTrackball;
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
			//this.microfacetDistribution = context.get2DColorTextureArrayBuilder().createTexture();
			
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
    	Matrix4 modelView = viewTrackball.getViewMatrix() // Trackball
				.times(Matrix4.translate(mesh.getCentroid().negated())); // Model
		
		Vector3 lightPosition = new Matrix3(lightTrackball.getViewMatrix()).times(mesh.getCentroid());
		
		this.shadowBuffer.clearDepthBuffer();
		Matrix4 lightMatrix = 
			Matrix4.lookAt(
				lightPosition, 
				mesh.getCentroid(),
				new Vector3(0.0f, 1.0f, 0.0f)
			);
    	
    	this.shadowRenderable.program().setUniform("model_view", lightMatrix);
    	this.shadowRenderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 2, 1.0f, 1.0f, 9.0f));
    	this.shadowRenderable.draw(PrimitiveMode.TRIANGLES, this.shadowBuffer);
		
		Framebuffer<ContextType> framebuffer = context.getDefaultFramebuffer();
    	
    	FramebufferSize size = framebuffer.getSize();
    	
    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
    	framebuffer.clearDepthBuffer();

    	this.renderable.program().setTexture("diffuse", this.diffuse);
    	this.renderable.program().setTexture("normal", this.normal);
    	this.renderable.program().setTexture("specular", this.specular);
    	this.renderable.program().setTexture("microfacetDistribution", this.microfacetDistribution);
    	this.renderable.program().setTexture("shadow", this.shadowBuffer.getDepthAttachmentTexture());
    	
    	this.renderable.program().setUniform("model_view", modelView);
    	this.renderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 4, (float)size.width / (float)size.height, 0.01f, 100.0f));
    	this.renderable.program().setUniform("lightMatrix", Matrix4.perspective((float)Math.PI / 2, 1.0f, 1.0f, 9.0f).times(lightMatrix));
    	
    	// TODO settings UI
    	this.renderable.program().setUniform("gamma", 2.2f);
    	this.renderable.program().setUniform("shadowBias", 0.001f);
    	this.renderable.program().setUniform("ambientColor", new Vector3(0.05f, 0.05f, 0.05f));
    	this.renderable.program().setUniform("lightColor", new Vector3(1.0f, 1.0f, 1.0f));
    	this.renderable.program().setUniform("lightPosition", lightPosition);
    	
    	this.renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
	}

	@Override
	public void cleanup() 
	{
		this.positionBuffer.delete();
		this.texCoordBuffer.delete();
		this.normalBuffer.delete();
		
		this.diffuse.delete();
		this.normal.delete();
		this.specular.delete();
		this.microfacetDistribution.delete();
		
		this.shadowBuffer.delete();
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
