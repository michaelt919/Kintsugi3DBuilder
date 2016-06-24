package tetzlaff.ulf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.CompressionFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Texture1D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.builders.ColorTextureBuilder;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.helpers.ZipWrapper;

/**
 * A class for organizing the GL resources that are necessary for view-dependent rendering.
 * @author Michael Tetzlaff
 *
 * @param <ContextType> The type of the context that the GL resources for this view set are to be used with.
 */
public class ViewSet<ContextType extends Context<ContextType>>
{
	/**
	 * A list of camera poses defining the transformation from object space to camera space for each view.
	 * These are necessary to perform projective texture mapping.
	 */
	private List<Matrix4> cameraPoseList;
	
	/**
	 * A list of inverted camera poses defining the transformation from camera space to object space for each view.
	 * (Useful for visualizing the cameras on screen).
	 */
	private List<Matrix4> cameraPoseInvList;

	/**
	 * A list of projection transformations defining the intrinsic properties of each camera.
	 * This list can be much smaller than the number of views if the same intrinsic properties apply for multiple views.
	 */
	private List<Projection> cameraProjectionList;
	
	/**
	 * A list containing an entry for every view which designates the index of the projection transformation that should be used for each view.
	 */
	private List<Integer> cameraProjectionIndexList;
	
	/**
	 * A list of light source positions, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 * Assumed by convention to be in camera space.
	 * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
	 */
	private List<Vector3> lightPositionList;
	
	/**
	 * A list of light source intensities, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
	 */
	private List<Vector3> lightIntensityList;
	
	/**
	 * A list containing an entry for every view which designates the index of the light source position and intensity that should be used for each view.
	 */
	private List<Integer> lightIndexList;
	
	/**
	 * A list containing the relative name of the image file corresponding to each view.
	 */
	private List<String> imageFileNames;
	
	private List<Vector3> absoluteLightPositionList;
	private List<Vector3> absoluteLightIntensityList;
	private List<Matrix4> secondaryCameraPoseList;
	private List<Matrix4> secondaryCameraPoseInvList;
	private List<Integer> secondaryCameraProjectionIndexList;
	private List<Integer> secondaryLightIndexList;
	private List<String> secondaryImageFileNames;
	
	/**
	 * The absolute file path to be used for loading images.
	 */
	private File filePath;
	
	/**
	 * The reference linear luminance values used for decoding pixel colors.
	 */
	private double[] linearLuminanceValues;
	
	/**
	 * The reference encoded luminance values used for decoding pixel colors.
	 */
	private byte[] encodedLuminanceValues;
	
	/**
	 * Used to decode pixel colors according to a gamma curve if reference values are unavailable, otherwise, affects the absolute brightness of the decoded colors.
	 */
	private float gamma;
	
	/**
	 * A GPU buffer containing the camera poses defining the transformation from object space to camera space for each view.
	 * These are necessary to perform projective texture mapping.
	 */
	private UniformBuffer<ContextType> cameraPoseBuffer;
	
	/**
	 * A GPU buffer containing projection transformations defining the intrinsic properties of each camera.
	 */
	private UniformBuffer<ContextType> cameraProjectionBuffer;
	
	/**
	 * A GPU buffer containing for every view an index designating the projection transformation that should be used for each view.
	 */
	private UniformBuffer<ContextType> cameraProjectionIndexBuffer;
	
	/**
	 * A GPU buffer containing light source positions, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 * Assumed by convention to be in camera space.
	 */
	private UniformBuffer<ContextType> lightPositionBuffer;
	
	/**
	 * A GPU buffer containing light source intensities, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 */
	private UniformBuffer<ContextType> lightIntensityBuffer;
	
	/**
	 * A GPU buffer containing for every view an index designating the light source position and intensity that should be used for each view.
	 */
	private UniformBuffer<ContextType> lightIndexBuffer;
	
	/**
	 * A texture array instantiated on the GPU containing the image corresponding to each view in this dataset.
	 */
	private Texture3D<ContextType> textureArray;
	
	/**
	 * A 1D texture defining how encoded RGB values should be converted to linear luminance.
	 */
	private Texture1D<ContextType> luminanceMap;
	
	/**
	 * A 1D texture defining how encoded RGB values should be converted to linear luminance.
	 */
	private Texture1D<ContextType> inverseLuminanceMap;
	
	/**
	 * The recommended near plane to use when rendering this view set.
	 */
	private float recommendedNearPlane;
	
	/**
	 * The recommended far plane to use when rendering this view set.
	 */
	private float recommendedFarPlane;
	
	/**
	 * Creates a new view set object, allocating and initializing GPU resources as appropriate.
	 * @param cameraPoseList A list of camera poses defining the transformation from object space to camera space for each view.
	 * These are necessary to perform projective texture mapping.
	 * @param cameraPoseInvList A list of inverted camera poses defining the transformation from camera space to object space for each view.
	 * (Useful for visualizing the cameras on screen).
	 * @param cameraProjectionList A list of projection transformations defining the intrinsic properties of each camera.
	 * This list can be much smaller than the number of views if the same intrinsic properties apply for multiple views.
	 * @param cameraProjectionIndexList  A list containing an entry for every view which designates the index of the projection transformation that should be used for each view.
	 * @param lightPositionList A list of light source positions, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 * Assumed by convention to be in camera space.
	 * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
	 * @param lightIntensityList A list of light source intensities, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 * This list can be much smaller than the number of views if the same illumination conditions apply for multiple views.
	 * @param lightIndexList A list containing an entry for every view which designates the index of the light source position and intensity that should be used for each view.
	 * @param imageFileNames A list containing the relative name of the image file corresponding to each view.
	 * @param imageOptions The requested options for loading the images in this dataset. 
	 * @param recommendedNearPlane A recommendation for the near plane to use when rendering this view set.
	 * @param recommendedFarPlane A recommendation for the far plane to use when rendering this view set.
	 * @param context The GL context in which to instantiate GPU resources.
	 * @param loadingCallback A callback for monitoring loading progress, particularly for images.
	 * @throws IOException Thrown if any File I/O errors occur while loading images.
	 */
	public ViewSet(
		List<Matrix4> cameraPoseList,
		List<Matrix4> cameraPoseInvList,
		List<Projection> cameraProjectionList,
		List<Integer> cameraProjectionIndexList,
		List<Vector3> lightPositionList,
		List<Vector3> lightIntensityList,
		List<Integer> lightIndexList,
		List<String> imageFileNames, 
		List<Vector3> absoluteLightPositionList,
		List<Vector3> absoluteLightIntensityList,
		List<Matrix4> secondaryCameraPoseList,
		List<Matrix4> secondaryCameraPoseInvList,
		List<Integer> secondaryCameraProjectionIndexList,
		List<Integer> secondaryLightIndexList,
		List<String> secondaryImageFileNames, 
		ViewSetImageOptions imageOptions,
		float gamma,
		double[] linearLuminanceValues,
		byte[] encodedLuminanceValues,
		float recommendedNearPlane,
		float recommendedFarPlane,
		ContextType context,
		ULFLoadingMonitor loadingCallback) throws IOException
	{
		this.cameraPoseList = cameraPoseList;
		this.cameraPoseInvList = cameraPoseInvList;
		this.cameraProjectionList = cameraProjectionList;
		this.cameraProjectionIndexList = cameraProjectionIndexList;
		this.lightPositionList = lightPositionList;
		this.lightIntensityList = lightIntensityList;
		this.lightIndexList = lightIndexList;
		this.absoluteLightPositionList = absoluteLightPositionList;
		this.absoluteLightIntensityList = absoluteLightIntensityList;
		this.secondaryCameraPoseList = secondaryCameraPoseList;
		this.secondaryCameraPoseInvList = secondaryCameraPoseInvList;
		this.secondaryCameraProjectionIndexList = secondaryCameraProjectionIndexList;
		this.secondaryLightIndexList = secondaryLightIndexList;
		this.secondaryImageFileNames = secondaryImageFileNames;
		this.imageFileNames = imageFileNames;
		this.recommendedNearPlane = recommendedNearPlane;
		this.recommendedFarPlane = recommendedFarPlane;
		
		if (imageOptions != null)
		{
			this.filePath = imageOptions.getFilePath();
		}
		
		// Store the poses in a uniform buffer
		if (cameraPoseList != null && cameraPoseList.size() > 0)
		{
			// Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
			FloatVertexList flattenedPoseMatrices = new FloatVertexList(16, cameraPoseList.size());
			
			for (int k = 0; k < cameraPoseList.size(); k++)
			{
				int d = 0;
				for (int col = 0; col < 4; col++) // column
				{
					for (int row = 0; row < 4; row++) // row
					{
						flattenedPoseMatrices.set(k, d, cameraPoseList.get(k).get(row, col));
						d++;
					}
				}
			}
			
			// Create the uniform buffer
			cameraPoseBuffer = context.createUniformBuffer().setData(flattenedPoseMatrices);
		}
		
		// Store the camera projections in a uniform buffer
		if (cameraProjectionList != null && cameraProjectionList.size() > 0)
		{
			// Flatten the camera projection matrices into 16-component vectors and store them in the vertex list data structure.
			FloatVertexList flattenedProjectionMatrices = new FloatVertexList(16, cameraProjectionList.size());
			
			for (int k = 0; k < cameraProjectionList.size(); k++)
			{
				int d = 0;
				for (int col = 0; col < 4; col++) // column
				{
					for (int row = 0; row < 4; row++) // row
					{
						Matrix4 projection = cameraProjectionList.get(k).getProjectionMatrix(recommendedNearPlane, recommendedFarPlane);
						flattenedProjectionMatrices.set(k, d, projection.get(row, col));
						d++;
					}
				}
			}
			
			// Create the uniform buffer
			cameraProjectionBuffer = context.createUniformBuffer().setData(flattenedProjectionMatrices);
		}
		
		// Store the camera projection indices in a uniform buffer
		if (cameraProjectionIndexList != null && cameraProjectionIndexList.size() > 0)
		{
			int[] indexArray = new int[cameraProjectionIndexList.size()];
			for (int i = 0; i < indexArray.length; i++)
			{
				indexArray[i] = cameraProjectionIndexList.get(i);
			}
			IntVertexList indexVertexList = new IntVertexList(1, cameraProjectionIndexList.size(), indexArray);
			cameraProjectionIndexBuffer = context.createUniformBuffer().setData(indexVertexList);
		}
		
		// Store the light positions in a uniform buffer
		if (lightPositionList != null && lightPositionList.size() > 0)
		{
			FloatVertexList lightPositions = new FloatVertexList(4, lightPositionList.size());
			for (int k = 0; k < lightPositionList.size(); k++)
			{
				lightPositions.set(k, 0, lightPositionList.get(k).x);
				lightPositions.set(k, 1, lightPositionList.get(k).y);
				lightPositions.set(k, 2, lightPositionList.get(k).z);
				lightPositions.set(k, 3, 1.0f);
			}
			
			// Create the uniform buffer
			lightPositionBuffer = context.createUniformBuffer().setData(lightPositions);
		}
		
		// Store the light positions in a uniform buffer
		if (lightIntensityList != null && lightIntensityList.size() > 0)
		{
			FloatVertexList lightIntensities = new FloatVertexList(4, lightIntensityList.size());
			for (int k = 0; k < lightPositionList.size(); k++)
			{
				lightIntensities.set(k, 0, lightIntensityList.get(k).x);
				lightIntensities.set(k, 1, lightIntensityList.get(k).y);
				lightIntensities.set(k, 2, lightIntensityList.get(k).z);
				lightIntensities.set(k, 3, 1.0f);
			}
			
			// Create the uniform buffer
			lightIntensityBuffer = context.createUniformBuffer().setData(lightIntensities);
		}
		
		// Store the light indices indices in a uniform buffer
		if (lightIndexList != null && lightIndexList.size() > 0)
		{
			int[] indexArray = new int[lightIndexList.size()];
			for (int i = 0; i < indexArray.length; i++)
			{
				indexArray[i] = lightIndexList.get(i);
			}
			IntVertexList indexVertexList = new IntVertexList(1, lightIndexList.size(), indexArray);
			lightIndexBuffer = context.createUniformBuffer().setData(indexVertexList);
		}
		
		// Luminance map texture
		if (linearLuminanceValues != null && encodedLuminanceValues != null && linearLuminanceValues.length > 0 && encodedLuminanceValues.length > 0)
		{
			this.linearLuminanceValues = linearLuminanceValues;
			this.encodedLuminanceValues = encodedLuminanceValues;
			this.gamma = gamma;
			luminanceMap = new SampledLuminanceEncoding(linearLuminanceValues, encodedLuminanceValues).createLuminanceMap(context);
			inverseLuminanceMap = new SampledLuminanceEncoding(linearLuminanceValues, encodedLuminanceValues).createInverseLuminanceMap(context);
		}
		
		// Read the images from a file
		if (imageOptions != null && imageOptions.isLoadingRequested() && imageOptions.getFilePath() != null && imageFileNames != null && imageFileNames.size() > 0)
		{
			Date timestamp = new Date();
			File imageFile = new File(imageOptions.getFilePath(), imageFileNames.get(0));
			//ZipWrapper myZip = new ZipWrapper(imageFile);

			if (!imageFile.exists())
			//if (!myZip.exists(imageFile))
			{
				// Try some alternate file formats/extensions
				String[] altFormats = { "png", "PNG", "jpg", "JPG", "jpeg", "JPEG" };
				for(final String extension : altFormats)
				{
					String[] filenameParts = imageFileNames.get(0).split("\\.");
			    	filenameParts[filenameParts.length - 1] = extension;
			    	String altFileName = String.join(".", filenameParts);
			    	File imageFileGuess = new File(imageOptions.getFilePath(), altFileName);					
			    	
			    	System.out.printf("Trying '%s'\n", imageFileGuess.getAbsolutePath());
			    	if (imageFileGuess.exists())
			    	//if(myZip.exists(imageFileGuess))
			    	{
				    	System.out.printf("Found!!\n");
			    		imageFile = imageFileGuess;
			    		break;
			    	}
				}

				// Is it still not there?
				if (!imageFile.exists())
		    	//if(!myZip.exists(imageFile))
		    	{
		    		throw new FileNotFoundException(
		    				String.format("'%s' not found.", imageFileNames.get(0)));
		    	}
			}
			
			// Read a single image to get the dimensions for the texture array
			InputStream input = new FileInputStream(imageFile); // myZip.retrieveFile(imageFile);
			BufferedImage img = ImageIO.read(input);
			if(img == null)
			{
				throw new IOException(String.format("Error: Unsupported image format '%s'.",
						imageFileNames.get(0)));				
			}
			//myZip.getInputStream().close();
			input.close();

			ColorTextureBuilder<ContextType, ? extends Texture3D<ContextType>> textureArrayBuilder = 
					context.get2DColorTextureArrayBuilder(img.getWidth(), img.getHeight(), imageFileNames.size());
			
			if (imageOptions.isCompressionRequested())
			{
				textureArrayBuilder.setInternalFormat(CompressionFormat.RGB_PUNCHTHROUGH_ALPHA1_4BPP);
			}
			else
			{
				textureArrayBuilder.setInternalFormat(ColorFormat.RGBA8);
			}
			
			if (imageOptions.areMipmapsRequested())
			{
				textureArrayBuilder.setMipmapsEnabled(true);
			}
			else
			{
				textureArrayBuilder.setMipmapsEnabled(false);
			}
			
			textureArrayBuilder.setLinearFilteringEnabled(true);
			textureArrayBuilder.setMaxAnisotropy(16.0f);
			textureArray = textureArrayBuilder.createTexture();
			
			if(loadingCallback != null) {
				loadingCallback.setMaximum(imageFileNames.size());
			}

//			boolean needsRotation = false;
			for (int i = 0; i < imageFileNames.size(); i++)
			{
				imageFile = new File(imageOptions.getFilePath(), imageFileNames.get(i));
				if (!imageFile.exists())
				//if (!myZip.exists(imageFile))
				{
					// Try some alternate file formats/extensions
					String[] altFormats = { "png", "PNG", "jpg", "JPG", "jpeg", "JPEG" };
					for(final String extension : altFormats)
					{
						String[] filenameParts = imageFileNames.get(i).split("\\.");
				    	filenameParts[filenameParts.length - 1] = extension;
				    	String altFileName = String.join(".", filenameParts);
				    	File imageFileGuess = new File(imageOptions.getFilePath(), altFileName);

				    	if (imageFileGuess.exists())
						//if (myZip.exists(imageFileGuess))
				    	{
				    		imageFile = imageFileGuess;
				    		break;
				    	}
					}

					// Is it still not there?
					if (!imageFile.exists())
					//if (!myZip.exists(imageFile))
			    	{
			    		throw new FileNotFoundException(
			    				String.format("'%s' not found.", imageFileNames.get(i)));
			    	}
				}
				
//				myZip.retrieveFile(imageFile);
				

//				// Examine image
//				img = ImageIO.read(myZip.retrieveFile(imageFile));
//				if(img == null)
//				{
//					throw new IOException(String.format("Error: Unsupported image format '%s'.",
//							imageFileNames.get(i)));				
//				}
//				myZip.getInputStream().close();

				// TODO: This is a beginning try at supporting rotated images, needs work
//				needsRotation = false;
//				(img.getWidth() == textureArray.getHeight() && img.getHeight() == textureArray.getWidth());
//				if(!needsRotation &&
//						(img.getWidth() != textureArray.getWidth() || img.getHeight() != textureArray.getHeight()))
//				{
//					// Image resolution does not match
//					// TODO: resample image to match so we can proceed
//				}
				
				this.textureArray.loadLayer(i, imageFile, true);

				if(loadingCallback != null) {
					loadingCallback.setProgress(i+1);
				}
			}

//			myZip.close();
			System.out.println("View Set textures loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		}
	}
	
	/**
	 * Deletes all the GL resources associated with this view set.
	 * Attempting to use these resources after calling this method will have undefined results.
	 */
	public void deleteOpenGLResources()
	{
		if (cameraPoseBuffer != null)
		{
			cameraPoseBuffer.delete();
		}
		
		if (cameraProjectionBuffer != null)
		{
			cameraProjectionBuffer.delete();
		}
		
		if (cameraProjectionIndexBuffer != null)
		{
			cameraProjectionIndexBuffer.delete();
		}
		
		if (lightPositionBuffer != null)
		{
			lightPositionBuffer.delete();
		}
		
		if (lightIntensityBuffer != null)
		{
			lightIntensityBuffer.delete();
		}
		
		if (lightIndexBuffer != null)
		{
			lightIndexBuffer.delete();
		}
		
		if (textureArray != null)
		{
			textureArray.delete();
		}
	}
	
	/**
	 * Loads a VSET file and creates and initializes a corresponding ViewSet object with all associated GPU resources.
	 * @param vsetFile The VSET file to load.
	 * @param context The GL context in which to create the resources.
	 * @return The newly created ViewSet object.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	public static <ContextType extends Context<ContextType>>  ViewSet<ContextType> loadFromVSETFile(
			File vsetFile, ContextType context) throws IOException
	{
		return ViewSet.loadFromVSETFile(vsetFile, context, null);
	}

	/**
	 * Loads a VSET file and creates and initializes a corresponding ViewSet object with all associated GPU resources.
	 * @param vsetFile The VSET file to load.
	 * @param context The GL context in which to create the resources.
	 * @param loadingCallback A callback for monitoring loading progress, particularly for images.
	 * @return The newly created ViewSet object.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	public static <ContextType extends Context<ContextType>>  ViewSet<ContextType> loadFromVSETFile(
			File vsetFile, ContextType context, ULFLoadingMonitor loadingCallback) throws IOException
	{
		return ViewSet.loadFromVSETFile(vsetFile, new ViewSetImageOptions(null, false, false, false), context, loadingCallback);
	}
	
	/**
	 * Loads a VSET file and creates and initializes a corresponding ViewSet object with all associated GPU resources.
	 * @param vsetFile The VSET file to load.
	 * @param imageOptions The requested options for loading the images in this dataset. 
	 * @param context The GL context in which to create the resources.
	 * @return The newly created ViewSet object.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	public static <ContextType extends Context<ContextType>> ViewSet<ContextType> loadFromVSETFile(
			File vsetFile, ViewSetImageOptions imageOptions, ContextType context) throws IOException
	{
		return ViewSet.loadFromVSETFile(vsetFile, imageOptions, context, null);
	}

	/**
	 * Loads a VSET file and creates and initializes a corresponding ViewSet object with all associated GPU resources.
	 * @param vsetFile The VSET file to load.
	 * @param imageOptions The requested options for loading the images in this dataset. 
	 * @param context The GL context in which to create the resources.
	 * @param loadingCallback A callback for monitoring loading progress, particularly for images.
	 * @return The newly created ViewSet object.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	public static <ContextType extends Context<ContextType>> ViewSet<ContextType> loadFromVSETFile(
			File vsetFile, ViewSetImageOptions imageOptions, ContextType context, ULFLoadingMonitor loadingCallback) throws IOException
	{
		Date timestamp = new Date();

		ZipWrapper myZip = new ZipWrapper(vsetFile);		
		InputStream input = myZip.getInputStream();
		if(input == null)
		{
			myZip.close();
			throw new IOException();
		}
		
		Scanner scanner = new Scanner(input);
		
		float recommendedGamma = 2.2f;
		float recommendedNearPlane = 0.0f;
		float recommendedFarPlane = Float.MAX_VALUE;
		List<Matrix4> cameraPoseList = new ArrayList<Matrix4>();
		List<Matrix4> cameraPoseInvList = new ArrayList<Matrix4>();
		List<Matrix4> orderedCameraPoseList = new ArrayList<Matrix4>();
		List<Matrix4> orderedCameraPoseInvList = new ArrayList<Matrix4>();
		List<Projection> cameraProjectionList = new ArrayList<Projection>();
		List<Vector3> lightPositionList = new ArrayList<Vector3>();
		List<Vector3> lightIntensityList = new ArrayList<Vector3>();
		List<Integer> cameraProjectionIndexList = new ArrayList<Integer>();
		List<Integer> lightIndexList = new ArrayList<Integer>();
		List<String> imageFileNames = new ArrayList<String>();
		List<Double> linearLuminanceList = new ArrayList<Double>();
		List<Byte> encodedLuminanceList = new ArrayList<Byte>();

		List<Vector3> absoluteLightPositionList = new ArrayList<Vector3>();
		List<Vector3> absoluteLightIntensityList = new ArrayList<Vector3>();
		List<Matrix4> secondaryCameraPoseList = new ArrayList<Matrix4>();
		List<Matrix4> secondaryCameraPoseInvList = new ArrayList<Matrix4>();
		List<Integer> secondaryCameraProjectionIndexList = new ArrayList<Integer>();
		List<Integer> secondaryLightIndexList = new ArrayList<Integer>();
		List<String> secondaryImageFileNames = new ArrayList<String>();
		
		while (scanner.hasNext())
		{
			String id = scanner.next();
			if (id.equals("c"))
			{
				recommendedNearPlane = scanner.nextFloat();
				recommendedFarPlane = scanner.nextFloat();
				scanner.nextLine();
			}
			else if (id.equals("p"))
			{
				// Pose from quaternion				
				float x = scanner.nextFloat();
				float y = scanner.nextFloat();
				float z = scanner.nextFloat();
				float i = scanner.nextFloat();
				float j = scanner.nextFloat();
				float k = scanner.nextFloat();
				float qr = scanner.nextFloat();
				
				cameraPoseList.add(Matrix4.fromQuaternion(i, j, k, qr)
					.times(Matrix4.translate(-x, -y, -z)));
				
				cameraPoseInvList.add(Matrix4.translate(x, y, z)
					.times(new Matrix4(Matrix3.fromQuaternion(i, j, k, qr).transpose())));
				
				scanner.nextLine();
			}
			else if (id.equals("P"))
			{
				// Pose from matrix
				Matrix4 newPose = new Matrix4(
					scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(),
					scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(),
					scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(), 
					scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
				
				cameraPoseList.add(newPose);
				cameraPoseInvList.add(newPose.quickInverse(0.001f));
			}
			else if (id.equals("d") || id.equals("D"))
			{
				// Skip "center/offset" parameters which are not consistent across all VSET files
				scanner.nextFloat();
				scanner.nextFloat();
				
				float aspect = scanner.nextFloat();
				float focalLength = scanner.nextFloat();
				
				float sensorWidth, k1;
				float k2, k3;
				if (id.equals("D"))
				{
					sensorWidth = scanner.nextFloat();
					k1 = scanner.nextFloat();
					k2 = scanner.nextFloat();
					k3 = scanner.nextFloat();
				}
				else
				{
					sensorWidth = 32.0f; // Default sensor width
					k1 = scanner.nextFloat();
					k2 = k3 = 0.0f;
				}
				
				float sensorHeight = sensorWidth / aspect;
				
				cameraProjectionList.add(new DistortionProjection(
					sensorWidth, sensorHeight, 
					focalLength, focalLength,
					sensorWidth / 2, sensorHeight / 2, k1, k2, k3
				));
				
				scanner.nextLine();
			}
			else if (id.equals("e"))
			{
				// Non-linear encoding
				linearLuminanceList.add(scanner.nextDouble());
				encodedLuminanceList.add((Byte)((byte)scanner.nextShort()));
				scanner.nextLine();
			}
			else if (id.equals("g"))
			{
				// Gamma
				recommendedGamma = scanner.nextFloat();
				scanner.nextLine();
			}
			else if (id.equals("f"))
			{
				// Skip "center/offset" parameters which are not consistent across all VSET files
				scanner.next();
				scanner.next();
				
				float aspect = scanner.nextFloat();
				float fovy = scanner.nextFloat();
				
				cameraProjectionList.add(new SimpleProjection(aspect, fovy));
				
				scanner.nextLine();
			}
			else if (id.equals("l"))
			{
				float x = scanner.nextFloat();
				float y = scanner.nextFloat();
				float z = scanner.nextFloat();
				lightPositionList.add(new Vector3(x, y, z));
				
				float r = scanner.nextFloat();
				float g = scanner.nextFloat();
				float b = scanner.nextFloat();
				lightIntensityList.add(new Vector3(r, g, b));

				// Skip the rest of the line
				scanner.nextLine();
			}
			else if (id.equals("L"))
			{
				float x = scanner.nextFloat();
				float y = scanner.nextFloat();
				float z = scanner.nextFloat();
				absoluteLightPositionList.add(new Vector3(x, y, z));
				
				float r = scanner.nextFloat();
				float g = scanner.nextFloat();
				float b = scanner.nextFloat();
				absoluteLightIntensityList.add(new Vector3(r, g, b));

				// Skip the rest of the line
				scanner.nextLine();
			}
			else if (id.equals("v"))
			{
				int poseId = scanner.nextInt();
				int projectionId = scanner.nextInt();
				int lightId = scanner.nextInt();
				
				String imgFilename = scanner.nextLine().trim();
				
				orderedCameraPoseList.add(cameraPoseList.get(poseId));
				orderedCameraPoseInvList.add(cameraPoseInvList.get(poseId));
				cameraProjectionIndexList.add(projectionId);
				lightIndexList.add(lightId);
				imageFileNames.add(imgFilename);
			}
			else if (id.equals("V"))
			{
				int poseId = scanner.nextInt();
				int projectionId = scanner.nextInt();
				int lightId = scanner.nextInt();
				
				String imgFilename = scanner.nextLine().trim();
				
				secondaryCameraPoseList.add(cameraPoseList.get(poseId));
				secondaryCameraPoseInvList.add(cameraPoseInvList.get(poseId));
				secondaryCameraProjectionIndexList.add(projectionId);
				secondaryLightIndexList.add(lightId);
				secondaryImageFileNames.add(imgFilename);
			}
			else
			{
				// Skip unrecognized line
				scanner.nextLine();
			}
		}
		
		scanner.close();
		myZip.close();
		
		if (imageOptions.getFilePath() == null)
		{
			imageOptions.setFilePath(vsetFile.getParentFile());
		}
		
		double[] linearLuminance = new double[linearLuminanceList.size()];
		for (int i = 0; i < linearLuminance.length; i++)
		{
			linearLuminance[i] = linearLuminanceList.get(i);
		}
		
		byte[] encodedLuminance = new byte[encodedLuminanceList.size()];
		for (int i = 0; i < encodedLuminance.length; i++)
		{
			encodedLuminance[i] = encodedLuminanceList.get(i);
		}

		System.out.println("View Set file loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		
		return new ViewSet<ContextType>(
			orderedCameraPoseList, orderedCameraPoseInvList, cameraProjectionList, cameraProjectionIndexList, lightPositionList, lightIntensityList, lightIndexList, imageFileNames, 
			absoluteLightPositionList, absoluteLightIntensityList, secondaryCameraPoseList, secondaryCameraPoseInvList, secondaryCameraProjectionIndexList, secondaryLightIndexList, secondaryImageFileNames,
			imageOptions, recommendedGamma, linearLuminance, encodedLuminance, recommendedNearPlane, recommendedFarPlane, context, loadingCallback);
	}
	
	/**
	 * A private class for representing a "sensor" in an Agisoft PhotoScan XML file.
	 * @author Michael Tetzlaff
	 *
	 */
	private static class Sensor
	{
		int index;
	    String id;
	    float width;
	    float height;
	    float fx;
	    float fy;
	    float cx;
	    float cy;
	    float k1;
	    float k2;
	    float k3;
	    float k4;
		float p1;
		float p2;
	    // TODO: Incorporate this value into the distortion object
	    @SuppressWarnings("unused")
	    float skew;
	    
	    Sensor(String id)
	    {
	        this.id = id;
	    }
	}
	
	/**
	 * A private class for representing a "camera" in an Agisoft PhotoScan XML file.
	 * @author Michael Tetzlaff
	 *
	 */
	private static class Camera
	{
	    String id;
	    String filename;
	    Matrix4 transform;
	    Sensor sensor;
	    
	    @SuppressWarnings("unused")
		int orientation;
	    
	    Camera(String id)
	    {
	        this.id = id;
	    }
	    
	    Camera(String id, Sensor sensor)
	    {
	        this.id = id;
	        this.sensor = sensor;
	    }
	    
	    @Override
	    public boolean equals(Object other)
	    {
	      Camera otherCam = (Camera)other;
	      return (this.id.equals(otherCam.id));
	    }
	}

	/**
	 * Loads a camera definition file exported in XML format from Agisoft PhotoScan and initializes a corresponding ViewSet object with all associated GPU resources.
	 * @param file The Agisoft PhotoScan XML camera file to load.
	 * @param imageOptions The requested options for loading the images in this dataset. 
	 * @param context The GL context in which to create the resources.
	 * @return The newly created ViewSet object.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	public static <ContextType extends Context<ContextType>> ViewSet<ContextType> loadFromAgisoftXMLFile(
			File file, ViewSetImageOptions imageOptions, ContextType context) throws IOException
	{
		return loadFromAgisoftXMLFile(file, imageOptions, context, null);
	}

	/**
	 * Loads a camera definition file exported in XML format from Agisoft PhotoScan and initializes a corresponding ViewSet object with all associated GPU resources.
	 * @param file The Agisoft PhotoScan XML camera file to load.
	 * @param imageOptions The requested options for loading the images in this dataset. 
	 * @param context The GL context in which to create the resources.
	 * @param loadingCallback A callback for monitoring loading progress, particularly for images.
	 * @return The newly created ViewSet object.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	public static <ContextType extends Context<ContextType>> ViewSet<ContextType> loadFromAgisoftXMLFile(
			File file, ViewSetImageOptions imageOptions, ContextType context, ULFLoadingMonitor loadingCallback) throws IOException
	{
		return loadFromAgisoftXMLFile(file, imageOptions, new Vector3(0.0f, 0.0f, 0.0f), new Vector3(1.0f, 1.0f, 1.0f), context, loadingCallback);
	}
	
	/**
	 * Loads a camera definition file exported in XML format from Agisoft PhotoScan and initializes a corresponding ViewSet object with all associated GPU resources.
	 * @param file The Agisoft PhotoScan XML camera file to load.
	 * @param imageOptions The requested options for loading the images in this dataset. 
	 * @param lightOffset The default light position relative to the camera to use for every view..
	 * @param lightIntensity The default light intensity to use for every view.
	 * @param context The GL context in which to create the resources.
	 * @param loadingCallback A callback for monitoring loading progress, particularly for images.
	 * @return The newly created ViewSet object.
	 * @throws IOException Thrown due to a File I/O error occurring.
	 */
	public static <ContextType extends Context<ContextType>> ViewSet<ContextType> loadFromAgisoftXMLFile(
		File file, ViewSetImageOptions imageOptions, Vector3 lightOffset, Vector3 lightIntensity, ContextType context, ULFLoadingMonitor loadingCallback) throws IOException
	{
        Map<String, Sensor> sensorSet = new Hashtable<String, Sensor>();
        HashSet<Camera> cameraSet = new HashSet<Camera>();
        
        Sensor sensor = null;
        Camera camera = null;

        float globalScale = 1.0f;
        Matrix4 globalRotation = new Matrix4();
        Vector3 globalTranslate = new Vector3(0.0f, 0.0f, 0.0f);
        
        String version = "", chunkLabel = "", groupLabel = "";
        String sensorID = "", cameraID = "", imageFile = "";
        int intVersion = 0;
        
        XMLInputFactory factory = XMLInputFactory.newInstance();
        try
        {
            InputStream xmlStream = new FileInputStream(file);
        	XMLStreamReader reader = factory.createXMLStreamReader(xmlStream);
            while (reader.hasNext())
            {
                int event = reader.next();
                switch(event)
                {
	                case XMLStreamConstants.START_ELEMENT:
	                    switch (reader.getLocalName())
	                    {
	                        case "document":
	                            version = reader.getAttributeValue(null, "version");
	                            String[] verComponents = version.split("\\.");
	                            for(int i=0; i<verComponents.length; i++)
	                            {
	                                intVersion *= 10;
	                                intVersion += Integer.parseInt(verComponents[i]);
	                            }
	                            System.out.printf("PhotoScan XML version %s (%d)\n", version, intVersion);
	                            break;
	                        case "chunk":
	                            chunkLabel = reader.getAttributeValue(null, "label");
	                            if(chunkLabel == null) { chunkLabel = "unnamed"; }
	                            System.out.printf("Reading chunk '%s'\n", chunkLabel);
	                            break;
	                        case "group":
	                            groupLabel = reader.getAttributeValue(null, "label");
	                            System.out.printf("Reading group '%s'\n", groupLabel);
	                            break;
	                        case "sensor":
	                            sensorID = reader.getAttributeValue(null, "id");
	                            System.out.printf("\tAdding sensor '%s'\n", sensorID);
	                            sensor = new Sensor(sensorID);
	                            break;
	                        case "camera":
	                            cameraID = reader.getAttributeValue(null, "id");
	                            if(cameraID == null || cameraSet.contains(new Camera(cameraID)))
	                            {
	                               camera = null;
	                            }
	                            else
	                            {
	                            	if (reader.getAttributeValue(null, "enabled").equals("true"))
	                            	{
		                                sensorID = reader.getAttributeValue(null, "sensor_id");
		                                imageFile = reader.getAttributeValue(null, "label");
		                                camera = new Camera(cameraID, sensorSet.get(sensorID));
		                                camera.filename = imageFile;
	                            	}
	                            	else
	                            	{
	                            		camera = null;
	                            	}
	                            }
	                            break;
	                        case "orientation":
	                        	if(camera != null)
	                        	{
	                        		camera.orientation = Integer.parseInt(reader.getElementText());
	                        	}
	                        	break;
	                        case "image":
	                            if (camera != null)
	                            {
	                                camera.filename = reader.getAttributeValue(null, "path");
	                            }
	                            break;
	                        case "resolution":
	                            if (sensor != null)
	                            {
	                                sensor.width = Float.parseFloat(reader.getAttributeValue(null, "width"));
	                                sensor.height = Float.parseFloat(reader.getAttributeValue(null, "height"));
	                            }
	                            break;
	                        case "fx":
	                            if (sensor != null) 
	                            {
	                                sensor.fx = Float.parseFloat(reader.getElementText());
	                            }
	                            break;
	                        case "fy":
	                            if (sensor != null) 
	                            {
	                                sensor.fy = Float.parseFloat(reader.getElementText());
	                            }
	                            break;
	                        case "cx":
	                            if (sensor != null) 
	                            {
	                                sensor.cx = Float.parseFloat(reader.getElementText());
	                            }
	                            break;
	                        case "cy":
	                            if (sensor != null) 
	                            {
	                                sensor.cy = Float.parseFloat(reader.getElementText());
	                            }
	                            break;
	                        case "p1":
	                            if (sensor != null) 
	                            {
	                                sensor.p1 = Float.parseFloat(reader.getElementText());
	                            }
	                            break;
	                        case "p2":
	                            if (sensor != null) 
	                            {
	                                sensor.p2 = Float.parseFloat(reader.getElementText());
	                            }
	                            break;
	                        case "k1":
	                            if (sensor != null) 
	                            {
	                                sensor.k1 = Float.parseFloat(reader.getElementText());
	                            }
	                            break;
	                        case "k2":
	                            if (sensor != null) 
	                            {
	                                sensor.k2 = Float.parseFloat(reader.getElementText());
	                            }
	                            break;
	                        case "k3":
	                            if (sensor != null) 
	                            {
	                                sensor.k3 = Float.parseFloat(reader.getElementText());
	                            }
	                            break;
	                        case "k4":
	                            if (sensor != null) 
	                            {
	                                sensor.k4 = Float.parseFloat(reader.getElementText());
	                            }
	                            break;
	                        case "skew":
	                            if (sensor != null) 
	                            {
	                                sensor.skew = Float.parseFloat(reader.getElementText());
	                            }
	                            break;                        	
	
	                        case "transform":
	                            if(camera == null && intVersion >= 110)
	                            {
	                            	break;
	                            }
	                                                        
	                        case "rotation":
		                        {
		                            String[] components = reader.getElementText().split("\\s");
		                            if ((reader.getLocalName().equals("transform") && components.length < 16) ||
		                                (reader.getLocalName().equals("rotation") && components.length < 9))
		                            {
		                                System.err.println("Error: Not enough components in the transform/rotation matrix");
		                            }
		                            else
		                            {
		                                int expectedSize = 16;
		                                if(reader.getLocalName().equals("rotation")) expectedSize = 9;
		                                
		                                if(components.length > expectedSize)
		                                {
		                                    System.err.println("Warning: Too many components in the transform/rotation matrix, ignoring extras.");                                    
		                                }
		                                
		                                float[] m = new float[expectedSize];
		                                for (int i = 0; i < expectedSize; i++)
		                                {
		                                    m[i] = Float.parseFloat(components[i]);
		                                }
		                                                                
		                                if (camera != null)
		                                {
		                                    // Negate 2nd and 3rd column to rotate 180 degrees around x-axis
		                                	// Invert matrix by transposing rotation and negating translation
		                                    Matrix4 trans;
		                                    if(expectedSize == 9)
		                                    {
		                                        trans = new Matrix4(new Matrix3(
		                                            m[0],  m[3],  m[6],
		                                           -m[1], -m[4], -m[7],
		                                           -m[2], -m[5], -m[8]));
		                                    }
		                                    else
		                                    {
		                                        trans = new Matrix4(new Matrix3(
			                                             m[0], 	m[4],  m[8],
			                                            -m[1], -m[5], -m[9],
			                                            -m[2], -m[6], -m[10]))
			                                    	.times(Matrix4.translate(-m[3], -m[7], -m[11]));
		                                    }
		                                    
		                                    camera.transform = trans;
		                                }
		                                else
		                                {
		                                    if(expectedSize == 9)
		                                    {
		                                        System.out.println("\tSetting global rotation.");
		                                    	globalRotation = new Matrix4(new Matrix3(
		                                            m[0], m[3], m[6],
		                                            m[1], m[4], m[7],
		                                            m[2], m[5], m[8]));
		                                    }
		                                    else
		                                    {
		                                        System.out.println("\tSetting global transformation.");
		                                    	globalRotation = new Matrix4(new Matrix3(
			                                             m[0], 	m[4],  m[8],
			                                             m[1],  m[5],  m[9],
			                                             m[2],  m[6],  m[10]))
		                                        	.times(Matrix4.translate(m[3], m[7], m[11]));
		                                    }
		                                }
		                            }
		                        }
	                            break;
	                            
	                        case "translation":
	                        	if (camera == null)
	                        	{
	                                System.out.println("\tSetting global translate.");
	                                String[] components = reader.getElementText().split("\\s");
	                                globalTranslate = new Vector3(
	                                		-Float.parseFloat(components[0]),
	                                		-Float.parseFloat(components[1]),
	                                		-Float.parseFloat(components[2]));
	                        	}
	                        	break;
	                        	
	                        case "scale":
	                        	if (camera == null)
	                        	{
	                                System.out.println("\tSetting global scale.");
	                    			globalScale = 1.0f/Float.parseFloat(reader.getElementText());
	                        	}
	                        	break;
	                            
	                        case "property": case "projections": case "depth":
	                        case "frames": case "frame": case "meta": case "R":
	                        case "size": case "center": case "region": case "settings":
	                        case "ground_control": case "mesh": case "texture":
	                        case "model": case "calibration": case "thumbnail":
	                        case "point_cloud": case "points": case "sensors":
	                        case "cameras":
	                           // These can all be safely ignored if version is >= 0.9.1
	                        break;
	                        
	                        case "photo": case "tracks": case "depth_maps":
	                        case "depth_map": case "dense_cloud":
	                           if(intVersion < 110) 
	                           {
	                               System.out.printf("Unexpected tag '%s' for psz version %s\n",
	                                                   reader.getLocalName(), version);
	                           }
	                        break;
	                        
	                        default:
	                           System.out.printf("Unexpected tag '%s'\n", reader.getLocalName());                           
	                           break;
	                    }
	                    break;
	                    
	                case XMLStreamConstants.END_ELEMENT:
	                {
	                    switch (reader.getLocalName())
	                    {
		                    case "chunk":
		                        System.out.printf("Finished chunk '%s'\n", chunkLabel);
		                        chunkLabel = "";
		                        break;
		                    case "group":
		                        System.out.printf("Finished group '%s'\n", groupLabel);
		                        groupLabel = "";
		                        break;
		                    case "sensor":
		                        if(sensor != null)
		                        {
		                            sensorSet.put(sensor.id, sensor);
		                            sensor = null;
		                        }
		                        break;
		                    case "camera":
		                        if(camera != null && camera.transform != null)
		                        {
		                           cameraSet.add(camera);
		                           System.out.printf("\tAdding camera %s, with sensor %s and image %s\n",
		                                   cameraID, sensorID, imageFile);
		                           camera = null;
		                        }
		                        break;                        
	                    }
	                }
	                break;
                }
            }
        }
        catch (XMLStreamException e)
        {
            e.printStackTrace();
        }

        // Initialize internal lists
        List<Matrix4> cameraPoseList = new ArrayList<Matrix4>();
        List<Matrix4> cameraPoseInvList = new ArrayList<Matrix4>();
		List<Projection> cameraProjectionList = new ArrayList<Projection>();
		List<Vector3> lightPositionList = new ArrayList<Vector3>();
		List<Vector3> lightIntensityList = new ArrayList<Vector3>();
		List<Integer> cameraProjectionIndexList = new ArrayList<Integer>();
		List<Integer> lightIndexList = new ArrayList<Integer>();
		List<String> imageFileNames = new ArrayList<String>();
        
        Sensor[] sensors = sensorSet.values().toArray(new Sensor[0]);
        
        // Reassign the ID for each sensor to correspond with the sensor's index
        // and add the corresponding projection to the list.
        for (int i = 0; i < sensors.length; i++)
        {
            sensors[i].index = i;
            cameraProjectionList.add(new DistortionProjection(
        		sensors[i].width,
        		sensors[i].height,
        		sensors[i].fx,
        		sensors[i].fy,
        		sensors[i].cx,
        		sensors[i].cy,
        		sensors[i].k1,
        		sensors[i].k2,
        		sensors[i].k3,
        		sensors[i].k4,
        		sensors[i].p1,
        		sensors[i].p2
    		));
        }
                
        Camera[] cameras = cameraSet.toArray(new Camera[0]);
        
        // Fill out the camera pose, projection index, and light index lists
        for (int i = 0; i < cameras.length; i++)
        {
        	// Apply the global transform to each camera
        	Matrix4 m1 = cameras[i].transform;
        	
        	// TODO: Figure out the right way to integrate the global transforms
            cameras[i].transform = m1.times(globalRotation)
            						 .times(Matrix4.translate(globalTranslate))
            						 .times(Matrix4.scale(globalScale));
        	            
            cameraPoseList.add(cameras[i].transform);
            
            // Compute inverse by just reversing steps to build transformation
            Matrix4 cameraPoseInv = Matrix4.scale(1.0f / globalScale)
            							   .times(Matrix4.translate(globalTranslate.negated()))
						        		   .times(globalRotation.transpose())
						        		   .times(new Matrix4(new Matrix3(m1).transpose()))
						            	   .times(Matrix4.translate(new Vector3(m1.getColumn(3).negated())));
            cameraPoseInvList.add(cameraPoseInv);
            
            Matrix4 expectedIdentity = cameraPoseInv.times(cameras[i].transform);
            boolean error = false;
        	for (int r = 0; r < 4; r++)
            {
            	for (int c = 0; c < 4; c++)
            	{
            		float expectedValue;
            		if (r == c)
            		{
            			expectedValue = 1.0f;
            		}
            		else
            		{
            			expectedValue = 0.0f;
            		}
            		
            		if(Math.abs(expectedIdentity.get(r, c) - expectedValue) > 0.001f)
            		{
            			error = true;
            			break;
            		}
            	}
            	if (error)
            	{
            		break;
            	}
            }
            
            if (error)
            {
	            System.err.println("Warning: matrix inverse could not be computed correctly - transformation is not affine.");
				for (int r = 0; r < 4; r++)
				{
					for (int c = 0; c < 4; c++)
					{
						System.err.print("\t" + String.format("%.3f", expectedIdentity.get(r, c)));
					}
					System.err.println();
				}
            }
            
            
            cameraProjectionIndexList.add(cameras[i].sensor.index);
            lightIndexList.add(0);
            imageFileNames.add(cameras[i].filename);
        }
        
        lightPositionList.add(lightOffset);
        lightIntensityList.add(lightIntensity);

        float farPlane = findFarPlane(cameraPoseInvList) * globalScale;
        System.out.println("Near and far planes: " + (farPlane/32.0f) + ", " + (farPlane));
        
        // Not used
		List<Vector3> absoluteLightPositionList = new ArrayList<Vector3>();
		List<Vector3> absoluteLightIntensityList = new ArrayList<Vector3>();
		List<Matrix4> secondaryCameraPoseList = new ArrayList<Matrix4>();
		List<Matrix4> secondaryCameraPoseInvList = new ArrayList<Matrix4>();
		List<Integer> secondaryCameraProjectionIndexList = new ArrayList<Integer>();
		List<Integer> secondaryLightIndexList = new ArrayList<Integer>();
		List<String> secondaryImageFileNames = new ArrayList<String>();
        
        return new ViewSet<ContextType>(cameraPoseList, cameraPoseInvList, cameraProjectionList, cameraProjectionIndexList, lightPositionList, lightIntensityList, lightIndexList, imageFileNames, 
        		absoluteLightPositionList, absoluteLightIntensityList, secondaryCameraPoseList, secondaryCameraPoseInvList, secondaryCameraProjectionIndexList, secondaryLightIndexList, secondaryImageFileNames,
        		imageOptions, 2.2f, null, null, farPlane / 32.0f, farPlane, context, loadingCallback);
    }
	
	/**
	 * A subroutine for guessing an appropriate far plane from an Agisoft PhotoScan XML file.
	 * Assumes that the object must lie between all of the cameras in the file.
	 * @param cameraPoseList The list of camera poses.
	 * @return A far plane estimate.
	 */
	private static float findFarPlane(List<Matrix4> cameraPoseInvList)
	{
		float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
		
		for (int i = 0; i < cameraPoseInvList.size(); i++)
		{
			Vector4 position = cameraPoseInvList.get(i).getColumn(3);
			minX = Math.min(minX, position.x);
			minY = Math.min(minY, position.y);
			minZ = Math.min(minZ, position.z);
			maxX = Math.max(maxX, position.x);
			maxY = Math.max(maxY, position.y);
			maxZ = Math.max(maxZ, position.z);
		}
	
		// Corner-to-corner
		float dX = (maxX-minX), dY = (maxY-minY), dZ = (maxZ-minZ);
		return (float)Math.sqrt(dX*dX + dY*dY + dZ*dZ);
		
		// Longest Side approach
//		return Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
	}
	
    public void writeVSETFileToStream(OutputStream outputStream)
    {
	    PrintStream out = new PrintStream(outputStream);
	    out.println("# Created by ULF Renderer from PhotoScan XML file");

	    out.println("# Estimated near and far planes");
	    out.printf("c\t%.8f\t%.8f\n", recommendedNearPlane, recommendedFarPlane);
	    
	    out.println("# " + cameraProjectionList.size() + (cameraProjectionList.size()==1?" Sensor":" Sensors"));
	    for (Projection proj : cameraProjectionList)
	    {
	        out.println(proj.toVSETString());
	    }
	
	    out.println("\n# " + cameraPoseList.size() + (cameraPoseList.size()==1?" Camera":" Cameras"));
	    for (Matrix4 pose : cameraPoseList)
	    {
	    	// TODO validate quaternion computation
//	    	Matrix3 rot = new Matrix3(pose);
//	    	if (rot.determinant() == 1.0f)
//	    	{
//	    		// No scale - use quaternion
//	    		Vector4 quat = rot.toQuaternion();
//		    	Vector4 loc = pose.getColumn(3);
//		    	out.printf("p\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\n",
//		    				loc.x, loc.y, loc.z, quat.x, quat.y, quat.z, quat.w);
//	    	}
//	    	else
	    	{
	    		// Write a general 4x4 matrix
	    		out.printf("P\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\n",
	    				pose.get(0, 0), pose.get(0, 1), pose.get(0, 2), pose.get(0, 3),
	    				pose.get(1, 0), pose.get(1, 1), pose.get(1, 2), pose.get(1, 3),
	    				pose.get(2, 0), pose.get(2, 1), pose.get(2, 2), pose.get(2, 3),
	    				pose.get(3, 0), pose.get(3, 1), pose.get(3, 2), pose.get(3, 3));
	    	}
	    }

	    if(!lightPositionList.isEmpty())
	    {
		    out.println("\n# " + lightPositionList.size() + (lightPositionList.size()==1?" Light":" Lights"));
		    for (int ID=0; ID < lightPositionList.size(); ID++)		    	
		    {
		    	Vector3 pos = lightPositionList.get(ID);
		    	Vector3 intensity = lightIntensityList.get(ID);
		    	out.printf("l\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\t%.8f\n", pos.x, pos.y, pos.z, intensity.x, intensity.y, intensity.z);
		    }	    
	    }
	    	    
	    out.println("\n# " + cameraPoseList.size() + (cameraPoseList.size()==1?" View":" Views"));
	    for (int ID=0; ID<cameraPoseList.size(); ID++)
	    {
	        out.printf("v\t%d\t%d\t%d\t%s\n", ID,  cameraProjectionIndexList.get(ID), lightIndexList.get(ID), imageFileNames.get(ID));
	    }
	    
	    out.close();
    }

	/**
	 * Gets the camera pose defining the transformation from object space to camera space for a particular view.
	 * @param poseIndex The index of the camera pose to retrieve.
	 * @return The camera pose as a 4x4 affine transformation matrix.
	 */
	public Matrix4 getCameraPose(int poseIndex) 
	{
		return this.cameraPoseList.get(poseIndex);
	}

	/**
	 * Gets the inverse of the camera pose, defining the transformation from camera space to object space for a particular view.
	 * @param poseIndex The index of the camera pose to retrieve.
	 * @return The inverse camera pose as a 4x4 affine transformation matrix.
	 */
	public Matrix4 getCameraPoseInverse(int poseIndex) 
	{
		return this.cameraPoseInvList.get(poseIndex);
	}
	
	/**
	 * Gets the name of the geometry file associated with this view set.
	 * @return The name of the geometry file.
	 */
	public String getGeometryFileName()
	{
		return "manifold.obj"; // TODO
	}
	
	/**
	 * Gets the geometry file associated with this view set.
	 * @return The geometry file.
	 */
	public File getGeometryFile()
	{
		return new File(this.filePath, "manifold.obj"); // TODO
	}
	
	/**
	 * Gets the relative name of the image file corresponding to a particular view.
	 * @param poseIndex The index of the image file to retrieve.
	 * @return The image file's relative name.
	 */
	public String getImageFileName(int poseIndex)
	{
		return this.imageFileNames.get(poseIndex);
	}

	/**
	 * Gets the image file corresponding to a particular view.
	 * @param poseIndex The index of the image file to retrieve.
	 * @return The image file.
	 */
	public File getImageFile(int poseIndex) 
	{
		return new File(this.filePath, this.imageFileNames.get(poseIndex));
	}

	/**
	 * Gets the projection transformation defining the intrinsic properties of a particular camera.
	 * @param projectionIndex The index of the camera whose projection transformation is to be retrieved.
	 * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
	 * @return The projection transformation.
	 */
	public Projection getCameraProjection(int projectionIndex) 
	{
		return this.cameraProjectionList.get(projectionIndex);
	}

	/**
	 * Gets the index of the projection transformation to be used for a particular view, 
	 * which can subsequently be used with getCameraProjection() to obtain the corresponding projection transformation itself.
	 * @param poseIndex The index of the view.
	 * @return The index of the projection transformation.
	 */
	public Integer getCameraProjectionIndex(int poseIndex) 
	{
		return this.cameraProjectionIndexList.get(poseIndex);
	}
	
	/**
	 * Gets the position of a particular light source.
	 * Used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 * Assumed by convention to be in camera space.
	 * @param lightIndex The index of the light source.
	 * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
	 * @return The position of the light source.
	 */
	public Vector3 getLightPosition(int lightIndex) 
	{
		return this.lightPositionList.get(lightIndex);
	}
	
	/**
	 * Gets the intensity of a particular light source.
	 * Used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 * Assumed by convention to be in camera space.
	 * @param lightIndex The index of the light source.
	 * IMPORTANT: this is NOT usually the same as the index of the view to be retrieved.
	 * @return The position of the light source.
	 */
	public Vector3 getLightIntensity(int lightIndex) 
	{
		return this.lightIntensityList.get(lightIndex);
	}
	
	public void setLightPosition(int lightIndex, Vector3 lightPosition)
	{
		this.lightPositionList.set(lightIndex, lightPosition); // TODO decide if this should result in a uniform buffer refresh
	}
	
	public void setLightIntensity(int lightIndex, Vector3 lightIntensity)
	{
		this.lightIntensityList.set(lightIndex, lightIntensity); // TODO decide if this should result in a uniform buffer refresh
	}
	
	/**
	 * Gets the index of the light source to be used for a particular view,
	 * which can subsequently be used with getLightPosition() and getLightIntensity() to obtain the actual position and intensity of the light source.
	 * @param poseIndex The index of the view.
	 * @return The index of the light source.
	 */
	public Integer getLightIndex(int poseIndex) 
	{
		return this.lightIndexList.get(poseIndex);
	}
	
	/**
	 * Gets the number of camera poses defined in this view set.
	 * @return The number of camera poses defined in this view set.
	 */
	public int getCameraPoseCount()
	{
		return this.cameraPoseList.size();
	}
	
	/**
	 * Gets the number of projection transformations defined in this view set.
	 * @return The number of projection transformations defined in this view set.
	 */
	public int getCameraProjectionCount()
	{
		return this.cameraProjectionList.size();
	}
	
	/**
	 * Gets a GPU buffer containing the camera poses defining the transformation from object space to camera space for each view.
	 * These are necessary to perform projective texture mapping.
	 * @return The requested uniform buffer.
	 */
	public UniformBuffer<ContextType> getCameraPoseBuffer()
	{
		return this.cameraPoseBuffer;
	}
	
	/**
	 * Gets a GPU buffer containing projection transformations defining the intrinsic properties of each camera.
	 * @return The requested uniform buffer.
	 */
	public UniformBuffer<ContextType> getCameraProjectionBuffer()
	{
		return this.cameraProjectionBuffer;
	}
	
	/**
	 * Gets a GPU buffer containing for every view an index designating the projection transformation that should be used for each view.
	 * @return The requested uniform buffer.
	 */
	public UniformBuffer<ContextType> getCameraProjectionIndexBuffer()
	{
		return this.cameraProjectionIndexBuffer;
	}
	
	/**
	 * Gets a GPU buffer containing light source positions, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 * Assumed by convention to be in camera space.
	 * @return The requested uniform buffer.
	 */
	public UniformBuffer<ContextType> getLightPositionBuffer()
	{
		return this.lightPositionBuffer;
	}
	
	/**
	 * Gets a GPU buffer containing light source intensities, used only for reflectance fields and illumination-dependent rendering (ignored for light fields).
	 * @return The requested uniform buffer.
	 */
	public UniformBuffer<ContextType> getLightIntensityBuffer()
	{
		return this.lightIntensityBuffer;
	}
	
	/**
	 * Gets a GPU buffer containing for every view an index designating the light source position and intensity that should be used for each view.
	 * @return The requested uniform buffer.
	 */
	public UniformBuffer<ContextType> getLightIndexBuffer()
	{
		return this.lightIndexBuffer;
	}

	/**
	 * Gets a texture array instantiated on the GPU containing the image corresponding to each view in this dataset.
	 * @return The requested texture array.
	 */
	public Texture3D<ContextType> getTextures() 
	{
		return this.textureArray;
	}
	
	/**
	 * Gets a 1D texture instantiated on the GPU containing the luminance map for this dataset.
	 * @return The requested luminance texture.
	 */
	public Texture1D<ContextType> getLuminanceMap()
	{
		return this.luminanceMap;
	}
	
	/**
	 * Gets a 1D texture instantiated on the GPU containing the inverse luminance map for this dataset.
	 * @return The requested inverse luminance texture.
	 */
	public Texture1D<ContextType> getInverseLuminanceMap()
	{
		return this.inverseLuminanceMap;
	}

	/**
	 * Gets the recommended near plane to use when rendering this view set.
	 * @return The near plane value.
	 */
	public float getRecommendedNearPlane() 
	{
		return this.recommendedNearPlane;
	}

	/**
	 * Gets the recommended far plane to use when rendering this view set.
	 * @return The far plane value.
	 */
	public float getRecommendedFarPlane() 
	{
		return this.recommendedFarPlane;
	}
	
	public Vector3 getAbsoluteLightPosition(int absoluteLightIndex) 
	{
		return this.absoluteLightPositionList.get(absoluteLightIndex);
	}
	
	public Vector3 getAbsoluteLightIntensity(int absoluteLightIndex) 
	{
		return this.absoluteLightIntensityList.get(absoluteLightIndex);
	}
	
	public int getSecondaryCameraPoseCount()
	{
		return this.secondaryCameraPoseList.size();
	}
	
	public Matrix4 getSecondaryCameraPose(int poseIndex) 
	{
		return this.secondaryCameraPoseList.get(poseIndex);
	}
	
	public Matrix4 getSecondaryCameraPoseInverse(int poseIndex) 
	{
		return this.secondaryCameraPoseInvList.get(poseIndex);
	}
	
	public Integer getSecondaryCameraProjectionIndex(int poseIndex) 
	{
		return this.secondaryCameraProjectionIndexList.get(poseIndex);
	}
	
	public Integer getSecondaryLightIndex(int poseIndex) 
	{
		return this.secondaryLightIndexList.get(poseIndex);
	}
	
	public String getSecondaryImageFileName(int poseIndex)
	{
		return this.secondaryImageFileNames.get(poseIndex);
	}

	public File getSecondaryImageFile(int poseIndex) 
	{
		return new File(this.filePath, this.secondaryImageFileNames.get(poseIndex));
	}
}
