package tetzlaff.lightfield;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.opengl.OpenGLTexture2D;

public class ViewSet<ImageType>
{
	public final Iterable<View<ImageType>> views;
	public final float recommendedNearPlane;
	public final float recommendedFarPlane;
	
	public ViewSet(Iterable<View<ImageType>> views, float recommendedNearPlane, float recommendedFarPlane) 
	{
		this.views = views;
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
		List<Matrix4> cameraPoses = new ArrayList<Matrix4>();
		List<Projection> cameraProjections = new ArrayList<Projection>();
		List<View<OpenGLTexture2D>> views = new ArrayList<View<OpenGLTexture2D>>();
		
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
				cameraPoses.add(Matrix4.fromQuaternion(i, j, k, qr)
					.times(Matrix4.translate(-x, -y, -z)));
				
				scanner.nextLine();
			}
			else if (id.equals("d") || id.equals("D"))
			{
				float cx = scanner.nextFloat();
				float cy = scanner.nextFloat();
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
					sensorWidth = 32.0f;
					k1 = scanner.nextFloat();
					k2 = k3 = 0.0f;
				}
				
				float sensorHeight = sensorWidth / aspect;
				
				if (cx == 0.0) 
				{
					cx = sensorWidth / 2;
				}
				
				if (cy == 0.0) 
				{
					cy = sensorHeight / 2;
				}
				
				cameraProjections.add(new DistortionProjection(
					sensorWidth, sensorHeight, 
					focalLength, focalLength,
					cx, cy, k1, k2, k3
				));
				
				scanner.nextLine();
			}
			else if (id.equals("f"))
			{
				// Skip meaningless "center/offset" parameters
				scanner.next();
				scanner.next();
				
				float aspect = scanner.nextFloat();
				float fovy = scanner.nextFloat();
				
				cameraProjections.add(new SimpleProjection(aspect, fovy));
				
				scanner.nextLine();
			}
			else if (id.equals("v"))
			{
				int poseId = scanner.nextInt();
				int projectionId = scanner.nextInt();
				
				// Ignore next field (unused light index)
				scanner.next();
				
				String imgFilename = scanner.nextLine().trim();
				
				String[] imgFilenameParts = imgFilename.split("\\.");
				String format = imgFilenameParts[imgFilenameParts.length - 1].toUpperCase();
				
				String[] filePathParts = filename.split("[\\\\\\/]");
				filePathParts[filePathParts.length - 1] = imgFilename;
				String imgFilePath = String.join(File.separator, filePathParts);
				
				views.add(new View<OpenGLTexture2D>(cameraPoses.get(poseId), cameraProjections.get(projectionId), new OpenGLTexture2D(format, imgFilePath)));
			}
			else
			{
				// Skip unrecognized line
				scanner.nextLine();
			}
		}

		System.out.println("View Set and textures loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		
		return new ViewSet<OpenGLTexture2D>(views, recommendedNearPlane, recommendedFarPlane);
	}
	
}
