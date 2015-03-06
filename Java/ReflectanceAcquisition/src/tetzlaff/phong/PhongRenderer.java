package tetzlaff.phong;

import java.io.File;
import java.io.IOException;

import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLDefaultFramebuffer;
import tetzlaff.gl.opengl.OpenGLFramebuffer;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLResource;
import tetzlaff.gl.opengl.OpenGLTexture2D;
import tetzlaff.ulf.ULFRenderer;
import tetzlaff.ulf.UnstructuredLightField;

public class PhongRenderer implements Drawable 
{
	private static OpenGLProgram program;
	
	private OpenGLContext context;
	private File objFile;
	private Trackball viewTrackball;
	private Trackball lightTrackball;
	
	private VertexMesh mesh;
	private OpenGLTexture2D diffuse;
	private OpenGLTexture2D normal;
	private OpenGLTexture2D specular;
	private OpenGLTexture2D roughness;
	private OpenGLRenderable renderable;
	private Iterable<OpenGLResource> vboResources;
	
	public PhongRenderer(OpenGLContext context, File objFile, Trackball viewTrackball, Trackball lightTrackball) 
	{
		this.context = context;
    	this.objFile = objFile;
    	this.viewTrackball = viewTrackball;
    	this.lightTrackball = lightTrackball;
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
    	
    	try 
    	{
			this.mesh = new VertexMesh("OBJ", objFile);
			
			File texturePath = new File(objFile.getParentFile(), "textures");
			this.diffuse = new OpenGLTexture2D(new File(texturePath, "diffuse.png"), true);
			this.normal = new OpenGLTexture2D(new File(texturePath, "normal.png"), true);
			this.specular = new OpenGLTexture2D(new File(texturePath, "specular.png"), true);
			this.roughness = new OpenGLTexture2D(new File(texturePath, "roughness.png"), true);
			
			this.renderable = new OpenGLRenderable(PhongRenderer.program);
			this.vboResources = this.renderable.addVertexMesh("position", "texCoord", null, this.mesh);
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
		OpenGLFramebuffer framebuffer = OpenGLDefaultFramebuffer.fromContext(context);
    	
    	FramebufferSize size = framebuffer.getSize();
    	
    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
    	framebuffer.clearDepthBuffer();

    	this.renderable.program().setTexture("diffuse", this.diffuse);
    	this.renderable.program().setTexture("normal", this.normal);
    	this.renderable.program().setTexture("specular", this.specular);
    	this.renderable.program().setTexture("roughness", this.roughness);
    	
    	Matrix4 modelView = Matrix4.lookAt(
				new Vector3(0.0f, 0.0f, 5.0f / viewTrackball.getScale()), 
				new Vector3(0.0f, 0.0f, 0.0f),
				new Vector3(0.0f, 1.0f, 0.0f)
			) // View
			.times(viewTrackball.getRotationMatrix()) // Trackball
			.times(Matrix4.translate(mesh.getCentroid().negated())); // Model
    	
    	this.renderable.program().setUniform("model_view", modelView);
    	this.renderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 4, (float)size.width / (float)size.height, 0.01f, 100.0f));
    	
    	// TODO settings UI
    	this.renderable.program().setUniform("gamma", 1.0f);
    	this.renderable.program().setUniform("ambientColor", new Vector3(0.0f, 0.0f, 0.0f));
    	this.renderable.program().setUniform("lightColor", new Vector3(1.0f, 1.0f, 1.0f));
    	//this.renderable.program().setUniform("lightPosition", 
    	//		new Matrix3(modelView).transpose().times(new Vector3(modelView.getColumn(3)).negated()));
    	this.renderable.program().setUniform("lightDirection", new Vector3(lightTrackball.getRotationMatrix().getColumn(2)));
    	
    	this.renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
	}

	@Override
	public void cleanup() 
	{
		this.diffuse.delete();
		this.normal.delete();
		this.specular.delete();
		this.roughness.delete();
		
		for (OpenGLResource r : vboResources)
		{
			r.delete();
		}
	}
}
