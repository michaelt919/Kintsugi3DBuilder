package tetzlaff.phong;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLDefaultFramebuffer;
import tetzlaff.gl.opengl.OpenGLFramebuffer;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLTexture2D;
import tetzlaff.gl.opengl.OpenGLVertexBuffer;
import tetzlaff.ulf.ViewSet;

public class PhongRenderer implements Drawable 
{
	public static final int NO_TEXTURE_MODE = 0;
	public static final int FULL_TEXTURE_MODE = 1;
	public static final int PLASTIC_TEXTURE_MODE = 2;
	public static final int METALLIC_TEXTURE_MODE = 3;
	public static final int DIFFUSE_TEXTURE_MODE = 4;
	public static final int NORMAL_TEXTURE_ONLY_MODE = 5;
	public static final int DIFFUSE_NO_SHADING_MODE = 6;
	
	private static OpenGLProgram program;
	private static OpenGLProgram shadowProgram;
	
	private int mode = FULL_TEXTURE_MODE;
	private float specularRoughnessTextureScale = 0.5f;
	private float specularRoughness = 0.125f;
	private float specularIntensity = 0.5f;
	private float metallicIntensity = 1.0f;
	
	private OpenGLContext context;
	private File objFile;
	private Trackball viewTrackball;
	private Trackball lightTrackball;
	
	private VertexMesh mesh;
	
	private OpenGLVertexBuffer positionBuffer;
	private OpenGLVertexBuffer texCoordBuffer;
	private OpenGLVertexBuffer normalBuffer;
	
	private OpenGLTexture2D diffuse;
	private OpenGLTexture2D normal;
	private OpenGLTexture2D specular;
	private OpenGLTexture2D specNormal;
	private OpenGLTexture2D roughness;
	
	private OpenGLRenderable renderable;
	private OpenGLRenderable shadowRenderable;
	
	private OpenGLFramebufferObject shadowBuffer;
	
	public PhongRenderer(OpenGLContext context, File objFile, Trackball viewTrackball, Trackball lightTrackball) 
	{
		this.context = context;
    	this.objFile = objFile;
    	this.viewTrackball = viewTrackball;
    	this.lightTrackball = lightTrackball;
	}

	public int getMode()
	{
		return this.mode;
	}
	
	public void setMode(int mode)
	{
		this.mode = mode;
	}

	@Override
	public void initialize() 
	{
		if (PhongRenderer.program == null)
    	{
	    	try
	        {
	    		PhongRenderer.program = new OpenGLProgram(new File("shaders/phong.vert"), new File("shaders/phong.frag"));
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}
		
		if (PhongRenderer.shadowProgram == null)
    	{
	    	try
	        {
	    		PhongRenderer.shadowProgram = new OpenGLProgram(new File("shaders/depth.vert"), new File("shaders/depth.frag"));
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
    	}
    	
    	try 
    	{
			this.mesh = new VertexMesh("OBJ", objFile);
			
			this.positionBuffer = new OpenGLVertexBuffer(this.mesh.getVertices());
			this.texCoordBuffer = new OpenGLVertexBuffer(this.mesh.getTexCoords());
			this.normalBuffer = new OpenGLVertexBuffer(this.mesh.getNormals());
			
			File texturePath = new File(objFile.getParentFile(), "textures");
			this.diffuse = new OpenGLTexture2D(new File(texturePath, "diffuse.png"), true, false, false);
			this.normal = new OpenGLTexture2D(new File(texturePath, "normal.png"), true, false, false);
			this.specular = new OpenGLTexture2D(new File(texturePath, "specular.png"), true, false, false);
			this.roughness = new OpenGLTexture2D(new File(texturePath, "roughness.png"), true, false, false);
			if (new File(texturePath, "snormal.png").exists())
			{
				this.specNormal = new OpenGLTexture2D(new File(texturePath, "snormal.png"), true, false, false);
			}
			
			this.renderable = new OpenGLRenderable(PhongRenderer.program);
			this.renderable.addVertexBuffer("position", this.positionBuffer);
			this.renderable.addVertexBuffer("texCoord", this.texCoordBuffer);
			this.renderable.addVertexBuffer("normal", this.normalBuffer);

			this.shadowRenderable = new OpenGLRenderable(PhongRenderer.shadowProgram);
			this.shadowRenderable.addVertexBuffer("position", this.positionBuffer);
			this.shadowRenderable.addVertexBuffer("texCoord", this.texCoordBuffer);
			this.shadowRenderable.addVertexBuffer("normal", this.normalBuffer);
			this.shadowBuffer = context.getFramebufferObjectBuilder(4096, 4096).createFramebufferObject();
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
    	Matrix4 modelView = Matrix4.lookAt(
				new Vector3(0.0f, 0.0f, 5.0f / viewTrackball.getScale()), 
				new Vector3(0.0f, 0.0f, 0.0f),
				new Vector3(0.0f, 1.0f, 0.0f)
			) // View
			.times(viewTrackball.getRotationMatrix()) // Trackball
			.times(Matrix4.translate(mesh.getCentroid().negated())); // Model
		
		Vector3 lightPosition = 
				(new Matrix3(lightTrackball.getRotationMatrix())
					.times(new Vector3(0.0f, 0.0f, 5.0f)))
					.plus(mesh.getCentroid());
		
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
		
		OpenGLFramebuffer framebuffer = OpenGLDefaultFramebuffer.fromContext(context);
    	
    	FramebufferSize size = framebuffer.getSize();
    	
    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
    	framebuffer.clearDepthBuffer();

    	this.renderable.program().setTexture("diffuse", this.diffuse);
    	this.renderable.program().setTexture("normal", this.normal);
    	this.renderable.program().setTexture("specular", this.specular);
    	if (this.specNormal == null)
    	{
    		this.renderable.program().setTexture("specNormal", this.normal);
    	}
    	else
    	{
    		this.renderable.program().setTexture("specNormal", this.specNormal);
    	}
    	this.renderable.program().setTexture("roughness", this.roughness);
    	this.renderable.program().setTexture("shadow", this.shadowBuffer.getDepthAttachmentTexture());
    	
    	this.renderable.program().setUniform("model_view", modelView);
    	this.renderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 4, (float)size.width / (float)size.height, 0.01f, 100.0f));
    	this.renderable.program().setUniform("lightMatrix", Matrix4.perspective((float)Math.PI / 2, 1.0f, 1.0f, 9.0f).times(lightMatrix));
    	
    	// TODO settings UI
    	this.renderable.program().setUniform("mode", mode);
    	this.renderable.program().setUniform("trueBlinnPhong", true);
    	this.renderable.program().setUniform("gamma", 2.2f);
    	this.renderable.program().setUniform("shadowBias", 0.001f);
    	this.renderable.program().setUniform("ambientColor", new Vector3(0.05f, 0.05f, 0.05f));
    	this.renderable.program().setUniform("lightColor", new Vector3(1.0f, 1.0f, 1.0f));
    	this.renderable.program().setUniform("lightPosition", lightPosition);
    	//this.renderable.program().setUniform("lightDirection", new Vector3(lightTrackball.getRotationMatrix().getColumn(2)));
    	if (mode == METALLIC_TEXTURE_MODE)
    	{
        	this.renderable.program().setUniform("specularScale", metallicIntensity);
    	}
    	else if (mode == FULL_TEXTURE_MODE)
    	{
        	this.renderable.program().setUniform("specularScale", 0.5f);
    	}
    	else
    	{
        	this.renderable.program().setUniform("specularScale", specularIntensity);
    	}
    	this.renderable.program().setUniform("roughnessScale", mode == FULL_TEXTURE_MODE ? specularRoughnessTextureScale : specularRoughness);
    	
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
		this.roughness.delete();
		
		this.shadowBuffer.delete();
	}
	
	public float getSpecularRoughness() {
		return this.specularRoughness;
	}

	public void setSpecularRoughness(float specularRoughness) {
		this.specularRoughness = specularRoughness;
		System.out.println("roughness = " + specularRoughness);
	}

	public float getSpecularRoughnessTextureScale() {
		return this.specularRoughnessTextureScale;
	}

	public void setSpecularRoughnessTextureScale(float specularRoughnessTextureScale) {
		this.specularRoughnessTextureScale = specularRoughnessTextureScale;
		System.out.println("roughness scale = " + specularRoughnessTextureScale);
	}

	public float getSpecularIntensity() {
		return this.specularIntensity;
	}

	public void setSpecularIntensity(float specularIntensity) {
		this.specularIntensity = specularIntensity;
	}

	public float getMetallicIntensity() {
		return this.metallicIntensity;
	}

	public void setMetallicIntensity(float metallicIntensity) {
		this.metallicIntensity = metallicIntensity;
	}

	public void resample(File targetVSETFile, File exportPath) throws IOException
	{
		ViewSet targetViewSet = ViewSet.loadFromVSETFile(targetVSETFile, false);
		
		BufferedImage img = ImageIO.read(targetViewSet.getImageFile(0));
		OpenGLFramebufferObject framebuffer = context.getFramebufferObjectBuilder(img.getWidth(), img.getHeight()).createFramebufferObject();
    	
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
