package tetzlaff.ulf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.opengl.OpenGLTextureArray;
import tetzlaff.gl.opengl.OpenGLUniformBuffer;

public class ViewSet
{
	private List<Matrix4> cameraPoseList;
	private List<Projection> cameraProjectionList;
	private List<Integer> cameraProjectionIndexList;
	private List<String> imageFileNames;
	private File filePath;
	
	private OpenGLUniformBuffer cameraPoseBuffer;
	private OpenGLUniformBuffer cameraProjectionBuffer;
	private OpenGLUniformBuffer cameraProjectionIndexBuffer;
	private OpenGLTextureArray textureArray;
	private float recommendedNearPlane;
	private float recommendedFarPlane;
	
	public ViewSet(
		List<Matrix4> cameraPoseList,
		List<Projection> cameraProjectionList,
		List<Integer> cameraProjectionIndexList,
		List<String> imageFileNames, 
		File imageFilePath,
		boolean loadImages,
		float recommendedNearPlane,
		float recommendedFarPlane) throws IOException
	{
		this.cameraPoseList = cameraPoseList;
		this.cameraProjectionList = cameraProjectionList;
		this.cameraProjectionIndexList = cameraProjectionIndexList;
		this.imageFileNames = imageFileNames;
		
		this.recommendedNearPlane = recommendedNearPlane;
		this.recommendedFarPlane = recommendedFarPlane;
		
		this.filePath = imageFilePath;
		
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
			cameraPoseBuffer = new OpenGLUniformBuffer(flattenedPoseMatrices);
		}
		
		// Store the camera projections in a uniform buffer
		if (cameraPoseList != null && cameraPoseList.size() > 0)
		{
			// Flatten the camera projection matrices into 16-component vectors and store them in the vertex list data structure.
			FloatVertexList flattenedProjectionMatrices = new FloatVertexList(16, cameraPoseList.size());
			
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
			cameraProjectionBuffer = new OpenGLUniformBuffer(flattenedProjectionMatrices);
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
			cameraProjectionIndexBuffer = new OpenGLUniformBuffer(indexVertexList);
		}
		
		// Read the images from a file
		if (loadImages && imageFilePath != null && imageFileNames != null && imageFileNames.size() > 0)
		{
			Date timestamp = new Date();
			
			// Read a single image to get the dimensions for the texture array
			BufferedImage img = ImageIO.read(new FileInputStream(new File(imageFilePath, imageFileNames.get(0))));
			this.textureArray = new OpenGLTextureArray(img.getWidth(), img.getHeight(), imageFileNames.size(), false, true, true);
			
			for (int i = 0; i < imageFileNames.size(); i++)
			{
				this.textureArray.loadLayer(i, new File(imageFilePath, imageFileNames.get(i)), true);
			}

			System.out.println("View Set textures loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		}
	}
	
	public void deleteOpenGLResources()
	{
		cameraPoseBuffer.delete();
		cameraProjectionBuffer.delete();
		cameraProjectionIndexBuffer.delete();
		textureArray.delete();
	}

	public static ViewSet loadFromVSETFile(File file, boolean loadImages) throws IOException
	{
		Date timestamp = new Date();
		
		InputStream input = new FileInputStream(file);
		Scanner scanner = new Scanner(input);
		
		float recommendedNearPlane = 0.0f;
		float recommendedFarPlane = Float.MAX_VALUE;
		List<Matrix4> cameraPoseList = new ArrayList<Matrix4>();
		List<Matrix4> orderedCameraPoseList = new ArrayList<Matrix4>();
		List<Projection> cameraProjectionList = new ArrayList<Projection>();
		List<Integer> cameraProjectionIndexList = new ArrayList<Integer>();
		List<String> imageFileNames = new ArrayList<String>();
		
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
				
				scanner.nextLine();
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
			else if (id.equals("v"))
			{
				int poseId = scanner.nextInt();
				int projectionId = scanner.nextInt();
				
				// Ignore next field (unused light index)
				scanner.next();
				
				String imgFilename = scanner.nextLine().trim();
				
				orderedCameraPoseList.add(cameraPoseList.get(poseId));
				cameraProjectionIndexList.add(projectionId);
				imageFileNames.add(imgFilename);
			}
			else
			{
				// Skip unrecognized line
				scanner.nextLine();
			}
		}
		
		scanner.close();

		System.out.println("View Set file loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		
		return new ViewSet(
			orderedCameraPoseList, cameraProjectionList, cameraProjectionIndexList, imageFileNames, file.getParentFile(),
			loadImages, recommendedNearPlane, recommendedFarPlane);
	}

	public Matrix4 getCameraPose(int poseIndex) 
	{
		return this.cameraPoseList.get(poseIndex);
	}
	
	public String getGeometryFileName()
	{
		return "manifold.obj"; // TODO
	}
	
	public File getGeometryFile()
	{
		return new File(this.filePath, "manifold.obj"); // TODO
	}
	
	public String getImageFileName(int poseIndex)
	{
		return this.imageFileNames.get(poseIndex);
	}

	public File getImageFile(int poseIndex) 
	{
		return new File(this.filePath, this.imageFileNames.get(poseIndex));
	}

	public Projection getCameraProjection(int projectionIndex) 
	{
		return this.cameraProjectionList.get(projectionIndex);
	}

	public Integer getCameraProjectionIndex(int poseIndex) 
	{
		return this.cameraProjectionIndexList.get(poseIndex);
	}
	
	public int getCameraPoseCount()
	{
		return this.cameraPoseList.size();
	}
	
	public int getCameraProjectionCount()
	{
		return this.cameraProjectionList.size();
	}
	
	public OpenGLUniformBuffer getCameraPoseBuffer()
	{
		return this.cameraPoseBuffer;
	}
	
	public OpenGLUniformBuffer getCameraProjectionBuffer()
	{
		return this.cameraProjectionBuffer;
	}
	
	public OpenGLUniformBuffer getCameraProjectionIndexBuffer()
	{
		return this.cameraProjectionIndexBuffer;
	}

	public OpenGLTextureArray getTextures() 
	{
		return this.textureArray;
	}

	public float getRecommendedNearPlane() 
	{
		return this.recommendedNearPlane;
	}

	public float getRecommendedFarPlane() 
	{
		return this.recommendedFarPlane;
	}
}
