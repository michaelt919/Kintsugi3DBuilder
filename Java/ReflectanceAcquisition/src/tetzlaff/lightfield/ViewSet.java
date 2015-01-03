package tetzlaff.lightfield;

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

import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.opengl.OpenGLTexture2D;
import tetzlaff.gl.opengl.OpenGLTextureArray;

public class ViewSet<ImageType>
{
	private List<Matrix4> cameraPoses;
	private List<Projection> cameraProjections;
	private List<Integer> cameraProjectionIndices;
	private OpenGLTextureArray textures;
	private float recommendedNearPlane;
	private float recommendedFarPlane;
	
	public ViewSet(
		List<Matrix4> cameraPoses,
		List<Projection> cameraProjections,
		List<Integer> cameraProjectionIndices,
		OpenGLTextureArray textures, 
		float recommendedNearPlane,
		float recommendedFarPlane) 
	{
		super();
		this.cameraPoses = cameraPoses;
		this.cameraProjections = cameraProjections;
		this.cameraProjectionIndices = cameraProjectionIndices;
		this.textures = textures;
		this.recommendedNearPlane = recommendedNearPlane;
		this.recommendedFarPlane = recommendedFarPlane;
	}

	public static ViewSet<OpenGLTexture2D> loadFromVSETFile(String filename) throws IOException
	{
		Date timestamp = new Date();
		
		InputStream input = new FileInputStream(filename);
		Scanner scanner = new Scanner(input);
		
		float recommendedNearPlane = 0.0f;
		float recommendedFarPlane = Float.MAX_VALUE;
		List<Matrix4> cameraPoseList = new ArrayList<Matrix4>();
		List<Matrix4> orderedCameraPoseList = new ArrayList<Matrix4>();
		List<Projection> cameraProjectionList = new ArrayList<Projection>();
		List<Integer> cameraProjectionIndexList = new ArrayList<Integer>();
		List<String> imageFilePaths = new ArrayList<String>();
		
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
				
				String[] filePathParts = filename.split("[\\\\\\/]");
				filePathParts[filePathParts.length - 1] = imgFilename;
				String imgFilePath = String.join(File.separator, filePathParts);
				
				orderedCameraPoseList.add(cameraPoseList.get(poseId));
				cameraProjectionIndexList.add(projectionId);
				imageFilePaths.add(imgFilePath);
			}
			else
			{
				// Skip unrecognized line
				scanner.nextLine();
			}
		}
		
		scanner.close();

		System.out.println("View Set file loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		
		
		if (imageFilePaths.size() > 0)
		{
			// Read a single image to get the dimensions for the texture array
			BufferedImage img = ImageIO.read(new FileInputStream(imageFilePaths.get(0)));
			OpenGLTextureArray textures = new OpenGLTextureArray(img.getWidth(), img.getHeight(), imageFilePaths.size());
			
			for (int i = 0; i < imageFilePaths.size(); i++)
			{
				String imgFilePath = imageFilePaths.get(i);
				
				String[] imgFilenameParts = imgFilePath.split("\\.");
				String format = imgFilenameParts[imgFilenameParts.length - 1].toUpperCase();
				textures.loadLayer(i, format, imgFilePath);
			}
	
			System.out.println("View Set textures loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
			
			return new ViewSet<OpenGLTexture2D>(
				orderedCameraPoseList, cameraProjectionList, cameraProjectionIndexList, textures,
				recommendedNearPlane, recommendedFarPlane);
		}
		else
		{
			return null;
		}
	}

	public Matrix4 getCameraPose(int poseIndex) 
	{
		return this.cameraPoses.get(poseIndex);
	}

	public Projection getCameraProjection(int projectionIndex) 
	{
		return this.cameraProjections.get(projectionIndex);
	}

	public Integer getCameraProjectionIndex(int poseIndex) 
	{
		return this.cameraProjectionIndices.get(poseIndex);
	}
	
	public int getCameraPoseCount()
	{
		return this.cameraPoses.size();
	}
	
	public int getCameraProjectionCount()
	{
		return this.cameraProjections.size();
	}

	public OpenGLTextureArray getTextures() 
	{
		return this.textures;
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
