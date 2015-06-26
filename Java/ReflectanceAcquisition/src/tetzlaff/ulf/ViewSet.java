package tetzlaff.ulf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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

import jdk.nashorn.internal.runtime.FindProperty;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.IntVertexList;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.opengl.OpenGLTextureArray;
import tetzlaff.gl.opengl.OpenGLUniformBuffer;
import tetzlaff.helpers.ZipWrapper;

public class ViewSet
{
	private List<Matrix4> cameraPoseList;
	private List<Projection> cameraProjectionList;
	private List<Integer> cameraProjectionIndexList;
	private List<Vector3> lightPositionList;
	private List<Integer> lightIndexList;
	private List<String> imageFileNames;
	private File filePath;
	
	private OpenGLUniformBuffer cameraPoseBuffer;
	private OpenGLUniformBuffer cameraProjectionBuffer;
	private OpenGLUniformBuffer cameraProjectionIndexBuffer;
	private OpenGLUniformBuffer lightPositionBuffer;
	private OpenGLUniformBuffer lightIndexBuffer;
	private OpenGLTextureArray textureArray;
	private float recommendedNearPlane;
	private float recommendedFarPlane;
	
	public ViewSet(
		List<Matrix4> cameraPoseList,
		List<Projection> cameraProjectionList,
		List<Integer> cameraProjectionIndexList,
		List<Vector3> lightPositionList,
		List<Integer> lightIndexList,
		List<String> imageFileNames, 
		File imageFilePath,
		boolean loadImages,
		float recommendedNearPlane,
		float recommendedFarPlane) throws IOException
	{
		this.cameraPoseList = cameraPoseList;
		this.cameraProjectionList = cameraProjectionList;
		this.cameraProjectionIndexList = cameraProjectionIndexList;
		this.lightPositionList = lightPositionList;
		this.lightIndexList = lightIndexList;
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
			lightPositionBuffer = new OpenGLUniformBuffer(lightPositions);
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
			lightIndexBuffer = new OpenGLUniformBuffer(indexVertexList);
		}
		
		// Read the images from a file
		if (loadImages && imageFilePath != null && imageFileNames != null && imageFileNames.size() > 0)
		{
			Date timestamp = new Date();
			
			// Read a single image to get the dimensions for the texture array
			ZipWrapper myZip = new ZipWrapper(new File(imageFilePath, imageFileNames.get(0)));
			InputStream input = myZip.getInputStream();
			
			BufferedImage img = ImageIO.read(input);
			this.textureArray = new OpenGLTextureArray(img.getWidth(), img.getHeight(), imageFileNames.size(), false, true, true);
			
			for (int i = 0; i < imageFileNames.size(); i++)
			{
				myZip.retrieveFile(new File(imageFilePath, imageFileNames.get(i)));
				this.textureArray.loadLayer(i, myZip, true);
			}

			System.out.println("View Set textures loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		}
	}
	
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
		
		if (lightIndexBuffer != null)
		{
			lightIndexBuffer.delete();
		}
		
		if (textureArray != null)
		{
			textureArray.delete();
		}
	}

	public static ViewSet loadFromVSETFile(File file, boolean loadImages) throws IOException
	{
		Date timestamp = new Date();

		ZipWrapper myZip = new ZipWrapper(file);		
		InputStream input = myZip.getInputStream();
		if(input == null)
		{
			myZip.close();
			throw new IOException();
		}
		
		Scanner scanner = new Scanner(input);
		
		float recommendedNearPlane = 0.0f;
		float recommendedFarPlane = Float.MAX_VALUE;
		List<Matrix4> cameraPoseList = new ArrayList<Matrix4>();
		List<Matrix4> orderedCameraPoseList = new ArrayList<Matrix4>();
		List<Projection> cameraProjectionList = new ArrayList<Projection>();
		List<Vector3> lightList = new ArrayList<Vector3>();
		List<Integer> cameraProjectionIndexList = new ArrayList<Integer>();
		List<Integer> lightIndexList = new ArrayList<Integer>();
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
			else if (id.equals("l"))
			{
				float x = scanner.nextFloat();
				float y = scanner.nextFloat();
				float z = scanner.nextFloat();
				lightList.add(new Vector3(x, y, z));

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
				cameraProjectionIndexList.add(projectionId);
				lightIndexList.add(lightId);
				imageFileNames.add(imgFilename);
			}
			else
			{
				// Skip unrecognized line
				scanner.nextLine();
			}
		}
		
		scanner.close();
		myZip.close();

		System.out.println("View Set file loaded in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		
		return new ViewSet(
			orderedCameraPoseList, cameraProjectionList, cameraProjectionIndexList, lightList, lightIndexList, 
			imageFileNames, file.getParentFile(), loadImages, recommendedNearPlane, recommendedFarPlane);
	}
	
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
	    
	    Sensor(String id)
	    {
	        this.id = id;
	    }
	}
	
	private static class Camera
	{
	    String id;
	    String filename;
	    Matrix4 transform;
	    Sensor sensor;
	    
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

	public static ViewSet loadFromAgisoftXMLFile(File file, boolean loadImages) throws IOException
	{
        Map<String, Sensor> sensorSet = new Hashtable<String, Sensor>();
        HashSet<Camera> cameraSet = new HashSet<Camera>();
        
        Sensor sensor = null;
        Camera camera = null;
        Matrix4 globalTransform = null;
        
        String version = "", chunkLabel = "";
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
                            System.out.printf("PhotoScan XML version %s\n", version);
                            String[] verComponents = version.split(".");
                            for(int i=0; i<verComponents.length; i++)
                            {
                                intVersion *= 10;
                                intVersion += Integer.parseInt(verComponents[i]);
                            }
                            break;
                        case "chunk":
                            chunkLabel = reader.getAttributeValue(null, "label");
                            System.out.printf("Reading chunk '%s'\n", chunkLabel);
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
                               sensorID = reader.getAttributeValue(null, "sensor_id");
                               imageFile = reader.getAttributeValue(null, "label");
                               System.out.printf("\tAdding camera %s, with sensor %s and image %s\n",
                                                   cameraID, sensorID, imageFile);
                               camera = new Camera(cameraID, sensorSet.get(sensorID));
                               camera.filename = imageFile;
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
                            
                        case "transform":
                            if(camera == null && version.equals("1.1.0")) break;
                            
                        case "rotation":    
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
                                    Matrix4 trans;
                                    if(expectedSize == 9)
                                    {
                                        trans = new Matrix4(new Matrix3(
                                           m[0], -m[1], -m[2],
                                           m[3], -m[4], -m[5],
                                           m[6], -m[7], -m[8]));
                                    }
                                    else
                                    {
                                        trans = new Matrix4(
                                            m[0], -m[1], -m[2],  m[3],
                                            m[4], -m[5], -m[6],  m[7],
                                            m[8], -m[9], -m[10], m[11],
                                            m[12], m[13], m[14], m[15]);
                                    }
                                    
                                    camera.transform = trans;
                                }
                                else
                                {
                                    System.out.println("\tSetting global transformation.");
                                    if(expectedSize == 9)
                                    {
                                        globalTransform = new Matrix4(new Matrix3(
                                           m[0], m[1], m[2],
                                           m[3], m[4], m[5],
                                           m[6], m[7], m[8]));
                                    }
                                    else
                                    {
                                        globalTransform = new Matrix4(
                                            m[0],  m[1],  m[2],  m[3],
                                            m[4],  m[5],  m[6],  m[7],
                                            m[8],  m[9],  m[10], m[11],
                                            m[12], m[13], m[14], m[15]);
                                    }
                                }
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
                    switch (reader.getLocalName())
                    {
                    case "chunk":
                        System.out.printf("Finished chunk '%s'\n", chunkLabel);
                        chunkLabel = "";
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
                           camera = null;
                        }
                        break;                        
                    }
                    break;
                }
            }
        }
        catch (XMLStreamException e)
        {
            e.printStackTrace();
        }
        
        List<Matrix4> cameraPoseList = new ArrayList<Matrix4>();
		List<Projection> cameraProjectionList = new ArrayList<Projection>();
		List<Vector3> lightList = new ArrayList<Vector3>();
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
        		sensors[i].k3
    		));
        }
        
        Camera[] cameras = cameraSet.toArray(new Camera[0]);
        
        // Fill out the camera pose, projection index, and light index lists
        for (int i = 0; i < cameras.length; i++)
        {
        	// Apply the global transform to each camera (if there is one)
            if(globalTransform != null)
            {
            	Matrix4 m1 = cameras[i].transform;
                cameras[i].transform = globalTransform.times(m1);
            }
        	
            cameraPoseList.add(cameras[i].transform);
            cameraProjectionIndexList.add(cameras[i].sensor.index);
            lightIndexList.add(0);
            imageFileNames.add(cameras[i].filename);
        }
        
        lightList.add(new Vector3(0.0f, 0.0f, 0.0f)); // Just assume the light is co-located with the camera (for now)

        float farPlane = findFarPlane(cameraPoseList);
        return new ViewSet(cameraPoseList, cameraProjectionList, cameraProjectionIndexList, lightList, lightIndexList,
        		imageFileNames, file.getParentFile(), loadImages, farPlane / 16.0f, farPlane);
    }
	
	private static float findFarPlane(List<Matrix4> cameraPoseList)
	{
		float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
		
		for (Matrix4 pose : cameraPoseList)
		{
			Vector3 position = new Matrix3(pose).transpose().times(new Vector3(pose.getColumn(3)).negated());
			minX = Math.min(minX, position.x);
			minY = Math.min(minY, position.y);
			minZ = Math.min(minZ, position.z);
			maxX = Math.max(maxX, position.x);
			maxY = Math.max(maxY, position.y);
			maxZ = Math.max(maxZ, position.z);
		}
		
		return Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
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
	
	public Vector3 getLightPosition(int projectionIndex) 
	{
		return this.lightPositionList.get(projectionIndex);
	}

	public Integer getLightPositionIndex(int poseIndex) 
	{
		return this.lightIndexList.get(poseIndex);
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
	
	public OpenGLUniformBuffer getLightPositionBuffer()
	{
		return this.lightPositionBuffer;
	}
	
	public OpenGLUniformBuffer getLightIndexBuffer()
	{
		return this.lightIndexBuffer;
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
