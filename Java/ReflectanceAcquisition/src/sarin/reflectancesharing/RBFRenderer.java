package sarin.reflectancesharing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.FramebufferSize;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.builders.framebuffer.DepthAttachmentSpec;
import tetzlaff.gl.helpers.Drawable;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.ulf.ViewSet;

/**
 * An implementation of a renderer for a single unstructured light field.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the context that will be used for rendering.
 */
public class RBFRenderer<ContextType extends Context<ContextType>> implements Drawable
{
    private Program<ContextType> program;
    
    private File meshFile;
	private ContextType context;
    private Renderable<ContextType> renderable;
    private VertexMesh mesh;
    private Trackball trackball;
    
    private VertexBuffer<ContextType> rectangleVBO;

    private Vector4 backgroundColor;
    
	/**
	 * A vertex buffer containing the positions of the vertices in the proxy geometry as 3D vectors.
	 */
    private VertexBuffer<ContextType> positionBuffer;
	
	/**
	 * A vertex buffer containing the texture coordinates of the vertices in the proxy geometry as 2D vectors.
	 */
	private VertexBuffer<ContextType> texCoordBuffer;
	
	/**
	 * A vertex buffer containing the surface normals of the vertices in the proxy geometry as 3D vectors.
	 */
	private VertexBuffer<ContextType> normalBuffer;
	
	/**
	 * A vertex buffer containing the surface normals of the vertices in the proxy geometry as 3D vectors.
	 */
	private VertexBuffer<ContextType> tangentBuffer;
	
	private UniformBuffer<ContextType> uniformBuffer;
	private UniformBuffer<ContextType> uniformCoefficientBuffer;
	private UniformBuffer<ContextType> uniformLambdaBuffer;
	private UniformBuffer<ContextType> uniformUVWBuffer;
    
    private Exception initError;

    /**
     * Creates a new unstructured light field renderer for rendering a light field from Agisoft PhotoScan.
     * @param context The GL context in which to perform the rendering.
     * @param program The program to use for rendering.
     * @param xmlFile The Agisoft PhotoScan XML camera file defining the views to load.
     * @param meshFile The mesh exported from Agisoft PhotoScan to be used as proxy geometry.
     * @param loadOptions The options to use when loading the light field.
     * @param trackball The trackball controlling the movement of the virtual camera.
     */
    public RBFRenderer(ContextType context, File meshFile, Trackball trackball)
    {
    	this.context = context;
    	this.meshFile = meshFile;
    	this.trackball = trackball;
    	this.initError = null;
    	this.backgroundColor = new Vector4(0.30f, 0.30f, 0.30f, 1.0f);
    }
 
    @Override
    public void initialize() 
    {
    	// Initialize shaders
    	if (this.program == null)
    	{
	    	try
	        {
	    		this.program = context.getShaderProgramBuilder()
	    				.addShader(ShaderType.VERTEX, new File("shaders/rbf.vert"))
	    				.addShader(ShaderType.FRAGMENT, new File("shaders/rbf.frag"))
	    				.createProgram();
	        }
	        catch (IOException e)
	        {
	        	this.initError = e;
	        }
    	} 	
    	
    	try
    	{
    		mesh = new VertexMesh("OBJ", meshFile);
    		
            positionBuffer = context.createVertexBuffer().setData(mesh.getVertices());

    		texCoordBuffer = null;
            if (mesh.hasTexCoords())
            {
            	texCoordBuffer = context.createVertexBuffer().setData(mesh.getTexCoords());
            }
            
            normalBuffer = null;
            tangentBuffer = null;
            
            if (mesh.hasNormals())
            {
            	normalBuffer = context.createVertexBuffer().setData(mesh.getNormals());
            	
            	if (mesh.hasTexCoords())
                {
            		tangentBuffer = context.createVertexBuffer().setData(mesh.getOrthoTangents());
                }
            }
    	}
        catch (IOException e)
        {
        	this.initError = e;
        }
    	
    	int numberOfPoints = 20;
    	
    	float[][] resultsFromFile = new float[numberOfPoints][numberOfPoints*3*2 + 4*3]; // TODO read from file
    	FloatVertexList uniformLambdaData = new FloatVertexList(4, numberOfPoints*100); // TODO change 100 to viewSet count
    	FloatVertexList uniformCoefficientData = new FloatVertexList(4, 4); // TODO change 100 to viewSet count
    	FloatVertexList uniformUVWData = new FloatVertexList(4, numberOfPoints*100); // TODO change 100 to viewSet count
    	// read from file
    	try {
			FileReader coefficientReader = new FileReader("C:\\Users\\Sarin\\Downloads\\coefficients.txt");
			BufferedReader bufferedCoefficientReader = new BufferedReader(coefficientReader);
			String line = null;
			int i = 0;
			while((line = bufferedCoefficientReader.readLine()) != null){
				String[] coefficients = line.split(" ");
				uniformCoefficientData.set(i, 0, Float.parseFloat(coefficients[0]));
				uniformCoefficientData.set(i, 1, Float.parseFloat(coefficients[1]));
				uniformCoefficientData.set(i, 2, Float.parseFloat(coefficients[2]));
				i ++;
			}
			FileReader lambdaReader = new FileReader("C:\\Users\\Sarin\\Downloads\\lambdas.txt");
			BufferedReader bufferedLambdaReader = new BufferedReader(lambdaReader);
			i = 0;
			while((line = bufferedLambdaReader.readLine()) != null){
				String[] lambdas = line.split(" ");
				uniformLambdaData.set(i, 0, Float.parseFloat(lambdas[0]));
				uniformLambdaData.set(i, 1, Float.parseFloat(lambdas[1]));
				uniformLambdaData.set(i, 2, Float.parseFloat(lambdas[2]));
				i ++;
			}
			FileReader UVWReader = new FileReader("C:\\Users\\Sarin\\Downloads\\uvw.txt");
			BufferedReader bufferedUVWReader = new BufferedReader(UVWReader);
			i = 0;
			while((line = bufferedUVWReader.readLine()) != null){
				String[] uvw = line.split(" ");
				uniformUVWData.set(i, 0, Float.parseFloat(uvw[0]));
				uniformUVWData.set(i, 1, Float.parseFloat(uvw[1]));
				uniformUVWData.set(i, 2, Float.parseFloat(uvw[2]));
				i ++;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    	/*FloatVertexList uniformData = new FloatVertexList(3, numberOfPoints);
		uniformData.set(0, 0, 0.0f);
		uniformData.set(0, 1, 0.0f);
		uniformData.set(0, 2, 1.0f);
		uniformData.set(1, 0, 0.0f);
		uniformData.set(1, 1, 0.0f);
		uniformData.set(1, 2, 1.0f);
		uniformData.set(2, 0, 0.0f);
		uniformData.set(2, 1, 0.0f);
		uniformData.set(2, 2, 1.0f);
    	
    	uniformBuffer = context.createUniformBuffer().setData(uniformData);*/
    	uniformCoefficientBuffer = context.createUniformBuffer().setData(uniformCoefficientData);
    	uniformLambdaBuffer = context.createUniformBuffer().setData(uniformLambdaData);
    	uniformUVWBuffer = context.createUniformBuffer().setData(uniformUVWData);
    	
    	this.renderable = context.createRenderable(program);
    	this.renderable.addVertexBuffer("position", positionBuffer);
    	this.renderable.addVertexBuffer("texCoord", texCoordBuffer);
    	this.renderable.addVertexBuffer("normal", normalBuffer);
    	this.renderable.addVertexBuffer("tangent", tangentBuffer);
    	
    	//this.program.setUniformBuffer("Constants", uniformBuffer);
    	this.program.setUniformBuffer("Constants", uniformCoefficientBuffer);
    	this.program.setUniformBuffer("Lambdas", uniformLambdaBuffer);
    	this.program.setUniformBuffer("Thetas", uniformUVWBuffer);
    	
    	this.rectangleVBO = context.createRectangle();

    	Renderable<ContextType> programRenderable = context.createRenderable(program);
    	
    	/*try {
			ViewSet<ContextType> viewSet = ViewSet.loadFromVSETFile( new File("C:\\Users\\Sarin\\Downloads\\sphere2\\Random100ViewSarin.vset"), context);
	    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
	    	{
	    		programRenderable.program().setUniform("cameraPose", viewSet.getCameraPose(i));
	    		programRenderable.program().setUniform("cameraProjection", 
	        			viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
	        				.getProjectionMatrix(viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
	    		programRenderable.program().setUniform( "lightPosition", viewSet.getLightPosition(viewSet.getLightPositionIndex(i)));

	    	}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
    }

	@Override
	public Exception getInitializeError()
	{
		return initError;
	}
		
	@Override
	public boolean hasInitializeError()
	{
		return (initError != null);
	}

	@Override
	public void update()
	{
	}
    
    @Override
    public boolean draw()
    {
    	Framebuffer<ContextType> framebuffer = context.getDefaultFramebuffer();
    	FramebufferSize size = framebuffer.getSize();
    	
    	renderable.program().setUniform("model_view", 
			Matrix4.lookAt(
				new Vector3(0.0f, 0.0f, 5.0f / trackball.getScale()), 
				new Vector3(0.0f, 0.0f, 0.0f),
				new Vector3(0.0f, 1.0f, 0.0f)
			) // View
			.times(trackball.getRotationMatrix())
			.times(Matrix4.translate(mesh.getCentroid().negated())) // Model
		);
    	
    	renderable.program().setUniform("projection", Matrix4.perspective((float)Math.PI / 4, (float)size.width / (float)size.height, 0.01f, 100.0f));
    	
		framebuffer.clearColorBuffer(0, backgroundColor.x, backgroundColor.y, backgroundColor.z, backgroundColor.w);
		framebuffer.clearDepthBuffer();
        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
        
    	return true;
    }
    

    @Override
    public void cleanup()
    {
    	if (rectangleVBO != null)
    	{
    		rectangleVBO.delete();
    		rectangleVBO = null;
    	}
    }
    
	@Override
	public void saveToFile(String fileFormat, File file)
	{
    	Framebuffer<ContextType> framebuffer = context.getDefaultFramebuffer();
    	try {
			framebuffer.saveColorBufferToFile(0, fileFormat, file);
		} catch (IOException e) {
			System.err.println("Error saving to file " + file.getPath());
			e.printStackTrace(System.err);
		}
	}
}
