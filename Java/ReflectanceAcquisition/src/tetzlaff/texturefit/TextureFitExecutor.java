package tetzlaff.texturefit;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.ops.MatrixFeatures;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.Renderable;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.Texture;
import tetzlaff.gl.Texture2D;
import tetzlaff.gl.Texture3D;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.VertexBuffer;
import tetzlaff.gl.builders.framebuffer.ColorAttachmentSpec;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector2;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.gl.helpers.VertexMesh;
import tetzlaff.ulf.ViewSet;

public class TextureFitExecutor<ContextType extends Context<ContextType>>
{
	// Debug parameters
	private static final boolean DEBUG = true;
	
	private final int SHADOW_MAP_FAR_PLANE_CUSHION = 2; // TODO decide where this should be defined
	
	private final int ROUGHNESS_TEXTURE_SIZE = 128; // TODO decide where this should be defined

	private ContextType context;
	private File vsetFile;
	private File objFile;
	private File imageDir;
	private File maskDir;
	private File rescaleDir;
	private File outputDir;
	private File tmpDir;
	private Vector3 lightPosition;
	private Vector3 lightIntensity;
	private TextureFitParameters param;
	
	private ViewSet<ContextType> viewSet;
	
	private Program<ContextType> depthRenderingProgram;
	private Program<ContextType> projTexProgram;
	private Program<ContextType> lightFitProgram;
	private Program<ContextType> diffuseFitProgram;
	private Program<ContextType> specularFitProgram;
	private Program<ContextType> specularFit2Program;
	private Program<ContextType> specularResidProgram;
	private Program<ContextType> diffuseDebugProgram;
	private Program<ContextType> specularDebugProgram;
	private Program<ContextType> textureRectProgram;
	private Program<ContextType> holeFillProgram;
	
	private VertexBuffer<ContextType> positionBuffer;
	private VertexBuffer<ContextType> texCoordBuffer;
	private VertexBuffer<ContextType> normalBuffer;
	private VertexBuffer<ContextType> tangentBuffer;
	private Vector3 center;
	
	UniformBuffer<ContextType> lightPositionBuffer;
	UniformBuffer<ContextType> lightIntensityBuffer;
	UniformBuffer<ContextType> shadowMatrixBuffer;
	
	public TextureFitExecutor(ContextType context, File vsetFile, File objFile, File imageDir, File maskDir, File rescaleDir, File outputDir,
			Vector3 lightOffset, Vector3 lightIntensity, TextureFitParameters param) 
	{
		this.context = context;
		this.vsetFile = vsetFile;
		this.objFile = objFile;
		this.imageDir = imageDir;
		this.maskDir = maskDir;
		this.rescaleDir = rescaleDir;
		this.outputDir = outputDir;
		this.lightPosition = lightOffset;
		this.lightIntensity = lightIntensity;
		this.param = param;
	}
	
	private interface TextureSpaceCallback<ContextType extends Context<ContextType>>
	{
		void execute(Framebuffer<ContextType> framebuffer, int subdivRow, int subdivCol);
	}
	
	private void projectIntoTextureSpace(ViewSet<ContextType> viewSet, Program<ContextType> program, int viewIndex, int textureSize, int textureSubdiv, TextureSpaceCallback<ContextType> callback) throws IOException
	{
		FramebufferObject<ContextType> mainFBO = 
			context.getFramebufferObjectBuilder(textureSize / textureSubdiv, textureSize / textureSubdiv)
				.addColorAttachments(ColorFormat.RGBA32F, 2)
				.createFramebufferObject();
    	Renderable<ContextType> renderable = context.createRenderable(program);
    	
    	renderable.addVertexBuffer("position", positionBuffer);
    	renderable.addVertexBuffer("texCoord", texCoordBuffer);
    	renderable.addVertexBuffer("normal", normalBuffer);
    	renderable.addVertexBuffer("tangent", tangentBuffer);
    	
    	renderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
    	renderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
    	
    	renderable.program().setUniform("cameraPose", viewSet.getCameraPose(viewIndex));
    	renderable.program().setUniform("cameraProjection", 
    			viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
    				.getProjectionMatrix(viewSet.getRecommendedNearPlane(), viewSet.getRecommendedFarPlane()));
    	
    	renderable.program().setUniform("gamma", param.getGamma());
    	
    	if (viewSet.getLuminanceMap() == null)
        {
    		renderable.program().setUniform("useLuminanceMap", false);
        }
        else
        {
        	renderable.program().setUniform("useLuminanceMap", true);
        	renderable.program().setTexture("luminanceMap", viewSet.getLuminanceMap());
        }
    	
    	if (lightIntensity != null)
    	{
    		renderable.program().setUniform("lightIntensity", lightIntensity);
    	}
    	else
    	{
    		renderable.program().setUniform("lightIntensity", viewSet.getLightIntensity(viewSet.getLightIndex(viewIndex)));
    	}
    	
    	boolean enableShadowTest = param.isCameraVisibilityTestEnabled();
    	
    	if (lightPosition != null)
    	{
    		renderable.program().setUniform("lightPosition", lightPosition);
    		enableShadowTest = enableShadowTest && !lightPosition.equals(new Vector3(0.0f, 0.0f, 0.0f));
    	}
    	else
    	{
    		renderable.program().setUniform("lightPosition", viewSet.getLightPosition(viewSet.getLightIndex(viewIndex)));
    	}
    	
    	File imageFile = new File(imageDir, viewSet.getImageFileName(viewIndex));
		if (!imageFile.exists())
		{
			String[] filenameParts = viewSet.getImageFileName(viewIndex).split("\\.");
	    	filenameParts[filenameParts.length - 1] = "png";
	    	String pngFileName = String.join(".", filenameParts);
	    	imageFile = new File(imageDir, pngFileName);
		}
    	
    	Texture2D<ContextType> viewTexture;
    	if (maskDir == null)
    	{
    		viewTexture = context.get2DColorTextureBuilder(imageFile, true)
    						.setLinearFilteringEnabled(true)
    						.setMipmapsEnabled(true)
    						.createTexture();
    	}
    	else
    	{
    		File maskFile = new File(maskDir, viewSet.getImageFileName(viewIndex));
			if (!maskFile.exists())
			{
				String[] filenameParts = viewSet.getImageFileName(viewIndex).split("\\.");
		    	filenameParts[filenameParts.length - 1] = "png";
		    	String pngFileName = String.join(".", filenameParts);
		    	maskFile = new File(maskDir, pngFileName);
			}
			
    		viewTexture = context.get2DColorTextureBuilder(imageFile, maskFile, true)
    						.setLinearFilteringEnabled(true)
    						.setMipmapsEnabled(true)
    						.createTexture();
    	}
    	
    	renderable.program().setTexture("viewImage", viewTexture);
    	
    	FramebufferObject<ContextType> depthFBO = 
			context.getFramebufferObjectBuilder(viewTexture.getWidth(), viewTexture.getHeight())
				.addDepthAttachment()
				.createFramebufferObject();
    	
    	Renderable<ContextType> depthRenderable = context.createRenderable(depthRenderingProgram);
    	depthRenderable.addVertexBuffer("position", positionBuffer);
    	
    	depthRenderingProgram.setUniform("model_view", viewSet.getCameraPose(viewIndex));
		depthRenderingProgram.setUniform("projection", 
			viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
				.getProjectionMatrix(
					viewSet.getRecommendedNearPlane(), 
					viewSet.getRecommendedFarPlane()
				)
		);
    	
		depthFBO.clearDepthBuffer();
    	depthRenderable.draw(PrimitiveMode.TRIANGLES, depthFBO);
    	
    	renderable.program().setTexture("depthImage", depthFBO.getDepthAttachmentTexture());
    	
    	FramebufferObject<ContextType> shadowFBO = null;
    	
    	if (enableShadowTest)
    	{
        	Matrix4 shadowModelView = Matrix4.lookAt(new Vector3(viewSet.getCameraPoseInverse(viewIndex).times(new Vector4(lightPosition, 1.0f))), center, new Vector3(0, 1, 0));
        	
    		Matrix4 shadowProjection = viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(viewIndex))
    				.getProjectionMatrix(
    					viewSet.getRecommendedNearPlane(), 
    					viewSet.getRecommendedFarPlane() * SHADOW_MAP_FAR_PLANE_CUSHION // double it for good measure
    				);
    		
    		shadowFBO = context.getFramebufferObjectBuilder(viewTexture.getWidth(), viewTexture.getHeight())
	    					.addDepthAttachment()
	    					.createFramebufferObject();
    		
    		Renderable<ContextType> shadowRenderable = context.createRenderable(depthRenderingProgram);
    		shadowRenderable.addVertexBuffer("position", positionBuffer);
        	
        	depthRenderingProgram.setUniform("model_view", shadowModelView);
    		depthRenderingProgram.setUniform("projection", shadowProjection);
        	
    		shadowFBO.clearDepthBuffer();
    		shadowRenderable.draw(PrimitiveMode.TRIANGLES, shadowFBO);
    		
    		renderable.program().setUniform("shadowTestEnabled", true);
    		renderable.program().setUniform("shadowMatrix", shadowProjection.times(shadowModelView));
        	renderable.program().setTexture("shadowImage", shadowFBO.getDepthAttachmentTexture());
    	}
    	else
    	{
    		renderable.program().setUniform("shadowTestEnabled", false);
    	}
	
    	for (int row = 0; row < textureSubdiv; row++)
    	{
	    	for (int col = 0; col < textureSubdiv; col++)
    		{
	    		renderable.program().setUniform("minTexCoord", 
	    				new Vector2((float)col / (float)textureSubdiv, (float)row / (float)textureSubdiv));
	    		
	    		renderable.program().setUniform("maxTexCoord", 
	    				new Vector2((float)(col+1) / (float)textureSubdiv, (float)(row+1) / (float)textureSubdiv));
	    		
	    		mainFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
	    		mainFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
	    		mainFBO.clearDepthBuffer();
	    		renderable.draw(PrimitiveMode.TRIANGLES, mainFBO);
	    		
	    		callback.execute(mainFBO, row, col);
	    	}
		}
    	
    	mainFBO.delete();
    	viewTexture.delete();
    	depthFBO.delete();
    	
    	if (shadowFBO != null)
		{
    		shadowFBO.delete();
		}
	}
	
	private DenseMatrix64F createRegularizationMatrix(int directionalNeighborhood, int spatialNeighborhood, float directionalWeight, float spatialWeight, boolean isotropic)
	{
		int dim = (isotropic ? 1 : directionalNeighborhood) * directionalNeighborhood * spatialNeighborhood * spatialNeighborhood;
		DenseMatrix64F matrix = new DenseMatrix64F(dim, dim);
		
		int index = 0;
		
		float quarterDirectionalWeight = directionalWeight / 4;
		float quarterSpatialWeight = spatialWeight / 4;
		
		for (int s = 0; s < (isotropic ? 1 : directionalNeighborhood); s++)
		{
			int indexIncrS = index + (directionalNeighborhood * spatialNeighborhood * spatialNeighborhood);
			
			for (int t = 0; t < directionalNeighborhood; t++)
			{
				int indexIncrT = index + (spatialNeighborhood * spatialNeighborhood);
				
				for (int u = 0; u < spatialNeighborhood; u++)
				{
					int indexIncrU = index + spatialNeighborhood;
					
					for (int v = 0; v < spatialNeighborhood; v++)
					{
						int indexIncrV = index + 1;
						
						if (!isotropic && s + 1 < directionalNeighborhood)
						{
							matrix.set(index, index, matrix.get(index, index) + quarterDirectionalWeight);
							matrix.set(indexIncrS, indexIncrS, matrix.get(indexIncrS, indexIncrS) + quarterDirectionalWeight);
							matrix.set(index, indexIncrS, matrix.get(index, indexIncrS) - quarterDirectionalWeight);
							matrix.set(indexIncrS, index, matrix.get(indexIncrS, index) - quarterDirectionalWeight);
						}

						if (t + 1 < directionalNeighborhood)
						{
							matrix.set(index, index, matrix.get(index, index) + quarterDirectionalWeight);
							matrix.set(indexIncrT, indexIncrT, matrix.get(indexIncrT, indexIncrT) + quarterDirectionalWeight);
							matrix.set(index, indexIncrT, matrix.get(index, indexIncrT) - quarterDirectionalWeight);
							matrix.set(indexIncrT, index, matrix.get(indexIncrT, index) - quarterDirectionalWeight);
						}

						if (u + 1 < spatialNeighborhood)
						{
							matrix.set(index, index, matrix.get(index, index) + quarterSpatialWeight);
							matrix.set(indexIncrU, indexIncrU, matrix.get(indexIncrU, indexIncrU) + quarterSpatialWeight);
							matrix.set(index, indexIncrU, matrix.get(index, indexIncrU) - quarterSpatialWeight);
							matrix.set(indexIncrU, index, matrix.get(indexIncrU, index) - quarterSpatialWeight);
						}

						if (v + 1 < spatialNeighborhood)
						{
							matrix.set(index, index, matrix.get(index, index) + quarterSpatialWeight);
							matrix.set(indexIncrV, indexIncrV, matrix.get(indexIncrV, indexIncrV) + quarterSpatialWeight);
							matrix.set(index, indexIncrV, matrix.get(index, indexIncrV) - quarterSpatialWeight);
							matrix.set(indexIncrV, index, matrix.get(indexIncrV, index) - quarterSpatialWeight);
						}
						
						index = indexIncrV;
						indexIncrU++;
						indexIncrT++;
						indexIncrS++;
					}
				}
			}
		}
		
		return matrix;
	}
	
	private void accumResampleSystem(float[][][][][] lhsData, float[][][][][] rhsData, int directionalRes, int spatialRes,
			float hDotTMax, boolean isotropic, int dataRes, int dataStartX, int dataStartY, int dataWidth, int dataHeight, float[] colorDataRGBA, float[] halfAngleDataTBNA)
	{
		int[] coordinatesMax = isotropic ?
			new int[] { directionalRes, spatialRes, spatialRes } :
			new int[] { directionalRes, directionalRes, spatialRes, spatialRes }; // exclusive
		
		for (int y = 0; y < dataHeight; y++)
		{
			for (int x = 0; x < dataWidth; x++)
			{
				float alpha = colorDataRGBA[((y*dataWidth) + x) * 4 + 3];
				
				if (alpha > 0.0f)
				{
					//float red = colorDataRGBA[((y*dataWidth) + x) * 4];
					//float green = colorDataRGBA[((y*dataWidth) + x) * 4 + 1];
					//float blue = colorDataRGBA[((y*dataWidth) + x) * 4 + 2];
					
					float luminance = colorDataRGBA[((y*dataWidth) + x) * 4];
					float hDotN =  colorDataRGBA[((y*dataWidth) + x) * 4 + 1];
		
					// Mapped coordinates for the specified half-vector and surface position
					double[] coordinates;
					
					if (isotropic)
					{
						coordinates = new double[]
						{
							(directionalRes-1) * (2 * hDotN - 1 + Math.sqrt(2) * (hDotN - 1)), //(directionalRes-1) * 2 * (hDotN - 0.5),
							(float) (x + dataStartX) * (float) (spatialRes-1) / (float) (dataRes-1),
							(float) (y + dataStartY) * (float) (spatialRes-1) / (float) (dataRes-1)
						};
					}
					else
					{
						// the tangent and bitangent components of the half-vector
						float hDotT = halfAngleDataTBNA[((y*dataWidth) + x) * 4];
						float hDotB = halfAngleDataTBNA[((y*dataWidth) + x) * 4 + 1]; 
						
						double projHOntoTB = Math.sqrt(hDotT*hDotT+hDotB*hDotB);
						double directionalScale = projHOntoTB / hDotTMax / Math.max(Math.abs(hDotT), Math.abs(hDotB));
						
						coordinates = new double[]
						{
							0.5 * (directionalRes-1) * (1 + directionalScale * hDotT),
							0.5 * (directionalRes-1) * (1 + directionalScale * hDotB),
							(float) (x + dataStartX) * (float) (spatialRes-1) / (float) (dataRes-1),
							(float) (y + dataStartY) * (float) (spatialRes-1) / (float) (dataRes-1)
						};
					};

					boolean outOfBounds = false;
					int[][] roundedCoordinatePairs = new int[coordinates.length][2];
					double[][] interpolationCoefficients = new double[coordinates.length][2];
					
					for (int i = 0; i < coordinates.length && !outOfBounds; i++)
					{
						roundedCoordinatePairs[i][0] = (int)Math.floor(coordinates[i]);
						roundedCoordinatePairs[i][1] = (int)Math.ceil(coordinates[i]);

						outOfBounds = outOfBounds || roundedCoordinatePairs[i][0] < 0 || roundedCoordinatePairs[i][1] >= coordinatesMax[i];
						if (!outOfBounds)
						{
							if (roundedCoordinatePairs[i][1] == roundedCoordinatePairs[i][0])
							{
								interpolationCoefficients[i][0] = 0.0;
							}
							else
							{
								interpolationCoefficients[i][0] = (coordinates[i] - roundedCoordinatePairs[i][0]) / (roundedCoordinatePairs[i][1] - roundedCoordinatePairs[i][0]);
							}
							
							interpolationCoefficients[i][1] = 1.0 - interpolationCoefficients[i][0];
						}
					}
					
					if (!outOfBounds)
					{
						int[][] indices = new int[1 << coordinates.length][coordinates.length];
						double[] weights = new double[indices.length];
						
						for (int i = 0; i < indices.length; i++)
						{
							weights[i] = 1.0;
							
							for (int j = 0; j < indices[i].length; j++)
							{
								int selector = (i >> j) & 0x1;
								indices[i][j] = roundedCoordinatePairs[j][selector];
								weights[i] *= interpolationCoefficients[j][selector];
							}
						}
						
						// Accumulate in the least squares system
						for (int p = 0; p < indices.length; p++)
						{
							float[] lhsRow = isotropic ?
								lhsData[0][indices[p][0]][indices[p][1]][indices[p][2]] :
								lhsData[indices[p][0]][indices[p][1]][indices[p][2]][indices[p][3]];
							float[] rhsRow = isotropic ? 
								rhsData[0][indices[p][0]][indices[p][1]][indices[p][2]] :
								rhsData[indices[p][0]][indices[p][1]][indices[p][2]][indices[p][3]];
							
							for (int q = 0; q < indices.length; q++)
							{
								if (isotropic)
								{
									lhsRow[(indices[q][0] - indices[p][0] + 1) + (indices[q][1] - indices[p][1] + 1) * 3 + (indices[q][2] - indices[p][2] + 1) * 9]
							 			+= (float)(weights[p] * weights[q] * alpha);
								}
								else
								{
									lhsRow[(indices[q][0] - indices[p][0] + 1) + (indices[q][1] - indices[p][1] + 1) * 3
										 	+ (indices[q][2] - indices[p][2] + 1) * 9 + (indices[q][3] - indices[p][3] + 1) * 27]
							 			+= (float)(weights[p] * weights[q] * alpha);
								}
							}
							
							rhsRow[0] += (float)weights[p] * luminance * alpha;
							
//							rhsRow[0] += (float)weights[p] * red * alpha;
//							rhsRow[1] += (float)weights[p] * green * alpha;
//							rhsRow[2] += (float)weights[p] * blue * alpha;
						}
					}
					
//					if (SIN_PI_OVER_8 < hDotN && hDotN < ONE_OVER_SQRT2)
//					{
//						// Evaluate 2 - 2( 2( 2( 2x^2 - 1 )^2 - 1 )^2 - 1 )^2
//						double weight = 2.0 * hDotN  * hDotN  - 1.0;   // First square
//						       weight = 2.0 * weight * weight - 1.0;   // Second square
//						       weight = 2.0 * weight * weight - 1.0;   // Third square
//						       weight = 2.0 * (1.0 - weight * weight); // Fourth square
//				        
//				        diffuseLowResData[?][?][0] += weight * red;
//				        diffuseLowResData[?][?][1] += weight * green;
//				        diffuseLowResData[?][?][2] += weight * blue;
//				        
//				        diffuseHighResData[?][?][0] += weight * red;
//				        diffuseHighResData[?][?][1] += weight * green;
//				        diffuseHighResData[?][?][2] += weight * blue;
//					}
				}
			}
		}
	}
	
	private boolean buildNeighborhoodSystem(float[][][][][] lhsData, float[][][][][] rhsData, int centerS, int centerT, int centerU, int centerV, 
			int directionalRes, int spatialRes, int directionalNeighborhood, int spatialNeighborhood, boolean isotropic, DenseMatrix64F lhsDest, DenseMatrix64F rhsDest)
	{
		boolean entryFound = false;
		
		int directionalRadius = (directionalNeighborhood - 1) / 2;
		int spatialRadius = (spatialNeighborhood - 1) / 2;
		
		// minimum coordinates
		int minS = isotropic ? 0 : centerS - directionalRadius;
		int minT = centerT - directionalRadius;
		int minU = centerU - spatialRadius;
		int minV = centerV - spatialRadius;
		
		// maximum coordinates
		int maxS = isotropic ? 0 : centerS + directionalRadius;
		int maxT = centerT + directionalRadius;
		int maxU = centerU + spatialRadius;
		int maxV = centerV + spatialRadius;
		
		for (int sRow = Math.max(0, minS); sRow <= maxS && sRow < directionalRes; sRow++)
		{
			for (int tRow = Math.max(0, minT); tRow <= maxT && tRow < directionalRes; tRow++)
			{
				for (int uRow = Math.max(0, minU); uRow <= maxU && uRow < spatialRes; uRow++)
				{
					int rowIndex = spatialNeighborhood * (spatialNeighborhood * (directionalNeighborhood * (sRow - minS) + (tRow - minT)) + (uRow - minU));
					
					for (int vRow = Math.max(0, minV); vRow <= maxV && vRow < spatialRes; vRow++)
					{
						float[] lhsRow = lhsData[sRow][tRow][uRow][vRow];
						
						for (int i = 0; i < lhsRow.length; i++)
						{
							int q = i;
							
							int sCol;
							if (isotropic)
							{
								sCol = 0;
							}
							else
							{
								sCol = (q % 3) + sRow - 1;
								q /= 3;
							}
							int tCol = (q % 3) + tRow - 1;
							q /= 3;
							int uCol = (q % 3) + uRow - 1;
							q /= 3;
							int vCol = (q % 3) + vRow - 1;
							
							if (minS <= sCol && sCol <= maxS && minT <= tCol && tCol <= maxT && 
								minU <= uCol && uCol <= maxU && minV <= vCol && vCol <= maxV)
							{
								int columnIndex = spatialNeighborhood * (spatialNeighborhood * (directionalNeighborhood * (sCol - minS) + (tCol - minT)) + (uCol - minU)) + (vCol - minV);

								entryFound = entryFound || lhsRow[i] != 0.0;
								
								lhsDest.set(rowIndex, columnIndex, lhsDest.get(rowIndex, columnIndex) + lhsRow[i]);
							}
							else if (	(minS > sCol || sCol > maxS || sCol == sRow) &&
										(minT > tCol || tCol > maxT || tCol == tRow) &&
										(minU > uCol || uCol > maxU || uCol == uRow) &&
										(minV > vCol || vCol > maxV || vCol == vRow))
								// Selecting column nodes that are outside the neighborhood boundary 
								// but aligned with the row node in every dimension in which they are inside the neighborhood boundary.
								// For each dimension in which the column node is outside the neighborhood boundary, 
								// we want to add its weight to the adjacent row node's diagonal to account for the column node not being in the system to be solved.
								// The case where s|t|u|vCol == s|t|u|vRow for each s|t|u|v will never fall here since it will be in the original "if" case.
							{
								entryFound = entryFound || lhsRow[i] != 0.0;
								lhsDest.set(rowIndex, rowIndex, lhsDest.get(rowIndex, rowIndex) + lhsRow[i]);
							}
						}
						
						float[] rhsRow = rhsData[sRow][tRow][uRow][vRow];
						
						rhsDest.set(rowIndex, 0, rhsDest.get(rowIndex, 0) + rhsRow[0]);
						rhsDest.set(rowIndex, 1, rhsDest.get(rowIndex, 1) + rhsRow[1]);
						rhsDest.set(rowIndex, 2, rhsDest.get(rowIndex, 2) + rhsRow[2]);
						
						rowIndex++;
					}
				}
			}
		}
		
		return entryFound;
	}
	
	private double[] resample(ViewSet<ContextType> viewSet, Texture<ContextType> diffuseFitTexture, Texture<ContextType> normalFitTexture) throws IOException
	{
		int directionalRes = 128;
		int spatialRes = ROUGHNESS_TEXTURE_SIZE;
		int directionalNeighborhood = 3;
		int spatialNeighborhood = 3;
		float directionalRegularization = 0.125f;
		float spatialRegularization = 0.875f;
		boolean isotropic = true;
		
		if (directionalNeighborhood % 2 == 0 || spatialNeighborhood % 2 == 0)
		{
			throw new IllegalArgumentException("Directional and spatial neighborhoods must be odd.");
		}
		
		// The greatest projection the half vector can have on the tangent plane before specularity is assumed to be negligible.
		float hDotTMax = 0.5f * (float)Math.sqrt(3); //(float)Math.min(0.5*Math.sqrt(3), Math.sqrt(Math.sqrt(2) * maxRmsSlope));
		
		float[][][][][] lhsData = isotropic ? 
			new float[1][directionalRes][spatialRes][spatialRes][27] :
			new float[directionalRes][directionalRes][spatialRes][spatialRes][81];
		float[][][][][] rhsData = isotropic ?
			new float[1][directionalRes][spatialRes][spatialRes][3] :
			new float[directionalRes][directionalRes][spatialRes][spatialRes][3];
		DenseMatrix64F regularizationMatrix = createRegularizationMatrix(directionalNeighborhood, spatialNeighborhood, directionalRegularization, spatialRegularization, isotropic);
		
		if (MatrixFeatures.nullity(regularizationMatrix) > 1)
		{
			System.out.println("Something's wrong...");
		}
		
		System.out.println("Sampling views...");
		
		specularResidProgram.setTexture("diffuseEstimate", diffuseFitTexture);
		specularResidProgram.setTexture("normalEstimate", normalFitTexture);
		
		for (int k = 0; k < viewSet.getCameraPoseCount(); k++)
		{
			final int K = k;
			
			projectIntoTextureSpace(viewSet, specularResidProgram, k, param.getTextureSize(), param.getTextureSubdivision(),
				(framebuffer, row, col) -> 
				{
					if (DEBUG)
			    	{
						try
	    				{
	    					framebuffer.saveColorBufferToFile(0, "PNG", new File(new File(outputDir, "debug"), String.format("colors%04d.png", K)));
	    					framebuffer.saveColorBufferToFile(1, "PNG", new File(new File(outputDir, "debug"), String.format("halfangle%04d.png", K)));
	    				}
	    				catch (IOException e)
	    				{
	    					e.printStackTrace();
	    				}
			    	}
					
					int partitionSize = param.getTextureSize() / param.getTextureSubdivision();

					accumResampleSystem(lhsData, rhsData, directionalRes, spatialRes, hDotTMax, isotropic, param.getTextureSize(), 
						col * partitionSize, row * partitionSize, partitionSize, partitionSize, 
						framebuffer.readFloatingPointColorBufferRGBA(0), 
						framebuffer.readFloatingPointColorBufferRGBA(1));
				});
			
	    	System.out.println("Completed " + (k+1) + "/" + viewSet.getCameraPoseCount() + " views...");
		}
		
		System.out.println("Solving system...");
		
		int partitionDim = isotropic ? 
			directionalNeighborhood * spatialNeighborhood * spatialNeighborhood :
			directionalNeighborhood * directionalNeighborhood * spatialNeighborhood * spatialNeighborhood;
		int solutionRow = (partitionDim - 1) / 2;
		
		// Solve using Cholesky decomposition
		LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.chol(partitionDim);
		
		File directionalLayerDir = new File(outputDir, "directional-layers");
		directionalLayerDir.mkdir();

		File spatialLayerDir = new File(outputDir, "spatial-layers");
		spatialLayerDir.mkdir();
		
		DenseMatrix64F lhsNeighborhood = regularizationMatrix.copy(); // new DenseMatrix64F(partitionDim, partitionDim);
		DenseMatrix64F rhsNeighborhood = new DenseMatrix64F(partitionDim, 3);
		
		// Test
		lhsNeighborhood.set(solutionRow, solutionRow, lhsNeighborhood.get(solutionRow, solutionRow) + 1.0);
		if (MatrixFeatures.nullity(lhsNeighborhood) > 0)
		{
			System.out.println("Something's wrong...");
		}
		
		float[][][][][] solution = isotropic ?
			new float[1][directionalRes][spatialRes][spatialRes][4] :
			new float[directionalRes][directionalRes][spatialRes][spatialRes][4];
		
		for (int i = 0; i < (isotropic ? 1 : directionalRes); i++)
		{
			for (int j = 0; j < directionalRes; j++)
			{
				int[] imageData = new int[spatialRes*spatialRes];
				
				if (spatialNeighborhood == spatialRes)
				{
					lhsNeighborhood.set(regularizationMatrix);
					rhsNeighborhood.zero();
					
					if (buildNeighborhoodSystem(lhsData, rhsData, i, j, (spatialRes-1)/2, (spatialRes-1)/2, directionalRes, spatialRes, directionalNeighborhood, spatialRes, isotropic, 
							lhsNeighborhood, rhsNeighborhood))
					{
						DenseMatrix64F neighborhoodSolution = new DenseMatrix64F(partitionDim, 3);
						solver.setA(lhsNeighborhood);
						solver.solve(rhsNeighborhood, neighborhoodSolution);
						
						for (int k = 0; k < spatialRes; k++)
						{
							for (int l = 0; l < spatialRes; l++)
							{
								float[] solutionEntry = solution[i][j][k][l];
								
								int currentRow = l + spatialRes * (k + spatialRes * ((isotropic ? 1 : directionalNeighborhood) * directionalNeighborhood - 1) / 2);
		
								solutionEntry[0] = (float)neighborhoodSolution.get(currentRow, 0);
								solutionEntry[1] = (float)neighborhoodSolution.get(currentRow, 1);
								solutionEntry[2] = (float)neighborhoodSolution.get(currentRow, 2);
								solutionEntry[3] = 1.0f;
								
								imageData[k + (spatialRes - l - 1) * spatialRes] = 0xFF000000 |
										(Math.max(0, Math.min(255, (int)(solutionEntry[0] * 255.0))) << 16) |
										(Math.max(0, Math.min(255, (int)(solutionEntry[1] * 255.0))) << 8) |
										Math.max(0, Math.min(255, (int)(solutionEntry[2] * 255.0)));
							}
						}
					}
				}
				else
				{
					for (int k = 0; k < spatialRes; k++)
					{
						for (int l = 0; l < spatialRes; l++)
						{
							lhsNeighborhood.set(regularizationMatrix);
							rhsNeighborhood.zero();
							
							if (buildNeighborhoodSystem(lhsData, rhsData, i, j, k, l, directionalRes, spatialRes, directionalNeighborhood, spatialNeighborhood, isotropic, 
									lhsNeighborhood, rhsNeighborhood))
							{
								DenseMatrix64F neighborhoodSolution = new DenseMatrix64F(partitionDim, 3);
								solver.setA(lhsNeighborhood);
								solver.solve(rhsNeighborhood, neighborhoodSolution);
								
								float[] solutionEntry = solution[i][j][k][l];
		
								solutionEntry[0] = (float)neighborhoodSolution.get(solutionRow, 0);
								solutionEntry[1] = (float)neighborhoodSolution.get(solutionRow, 1);
								solutionEntry[2] = (float)neighborhoodSolution.get(solutionRow, 2);
								solutionEntry[3] = 1.0f;
								
								imageData[k + (spatialRes - l - 1) * spatialRes] = 0xFF000000 |
										(Math.max(0, Math.min(255, (int)(solutionEntry[0] * 255.0))) << 16) |
										(Math.max(0, Math.min(255, (int)(solutionEntry[1] * 255.0))) << 8) |
										Math.max(0, Math.min(255, (int)(solutionEntry[2] * 255.0)));
							}
						}
					}
				}
				
				
				BufferedImage outImg = new BufferedImage(spatialRes, spatialRes, BufferedImage.TYPE_INT_ARGB);
		        outImg.setRGB(0, 0, spatialRes, spatialRes, imageData, 0, spatialRes);
		        ImageIO.write(outImg, "PNG", new File(directionalLayerDir, (isotropic ? "" : i + "_") + j + ".png"));

				System.out.println("Finished layer " + (1 + j + (isotropic ? 0 : i * directionalRes)) + "/" + ((isotropic ? 1 : directionalRes) * directionalRes) + " ...");
			}
		}
		
		if (isotropic)
		{
			double[] roughnessValues = new double[spatialRes*spatialRes];
			double[] reflectivityValues = new double[spatialRes*spatialRes];
			
			int[] roughnessData = new int[spatialRes*spatialRes];
			int[] reflectivityData = new int[spatialRes*spatialRes];
			
			for (int k = 0; k < spatialRes; k++)
			{
				for (int l = 0; l < spatialRes; l++)
				{
					// Integral of Specular reflectivity
					// 		times microfacet probability
					//		times microfacet slope squared
					// 		times d [n dot h]
					// as [n dot h] goes from [n dot h]_min to 1
					double specularWeightedSlopeSum = 0.0;
					
					// Integral to find Specular reflectivity (low res)
					double specularReflectivitySum = 0.0;
					
					double lastAlpha;
					
					// In case the first step(s) are missing
					int j = 0;
					do
					{
						lastAlpha = solution[0][j][k][l][3];
						j++;
					}
					while (j < directionalRes && lastAlpha == 0.0);
					
					float[] solutionEntry = solution[0][j-1][k][l];
					double lastWeight = (solutionEntry[0] + solutionEntry[1] + solutionEntry[2]) / 3;
					
					double coord = (double)(j - 1) / (double)(directionalRes - 1);
					double lastNDotH = coord + (1 - coord) * Math.sqrt(0.5);
					
					specularWeightedSlopeSum += lastWeight * (1 / lastNDotH - lastNDotH) * (lastNDotH - Math.sqrt(0.5));
					specularReflectivitySum += lastWeight * lastNDotH * (lastNDotH - Math.sqrt(0.5));
					
					int intervalCount = 0;
					
					for ( ; j < directionalRes; j++)
					{
						solutionEntry = solution[0][j][k][l];
						
						double alpha = solutionEntry[3];
						
						if (alpha > 0.0)
						{
							coord = (double)j / (double)(directionalRes - 1);
							double nDotH = coord + (1 - coord) * Math.sqrt(0.5);
							double weight = (solutionEntry[0] + solutionEntry[1] + solutionEntry[2]) / 3;
							
							// Trapezoidal rule for integration
							specularWeightedSlopeSum += (weight * (1 / nDotH - nDotH) + lastWeight * (1 / lastNDotH - lastNDotH)) / 2 * (nDotH - lastNDotH);
							specularReflectivitySum += (weight * nDotH + lastWeight * lastNDotH) / 2 * (nDotH - lastNDotH);
							
							intervalCount++;
							
							lastWeight = weight;
							lastNDotH = nDotH;
							lastAlpha = alpha;
						}
					}
					
					// In case the final step(s) were missing
					specularWeightedSlopeSum += lastWeight * (1 / lastNDotH - lastNDotH) * (1.0 - lastNDotH);
					specularReflectivitySum += lastWeight * lastNDotH * (1.0 - lastNDotH);
					
					if (intervalCount > 0 && specularReflectivitySum > 0.0 && specularWeightedSlopeSum > 0.0)
					{
						double roughnessSq = specularWeightedSlopeSum / specularReflectivitySum;
						double roughness = Math.sqrt(roughnessSq);
						
						// Assuming scaling by 1 / (pi * roughness^2)
						double reflectivity = specularReflectivitySum * 2 / roughnessSq;
						
						double alpha = (double)intervalCount / (double)(directionalRes - 1);
						
						roughnessValues[k + l * spatialRes] = roughness;
						reflectivityValues[k + l * spatialRes] = reflectivity;
						
						int roughnessPixelValue = Math.max(0, Math.min(255, (int)(roughness * 255.0)));
						int reflectivityPixelValue = Math.max(0, Math.min(255, (int)(reflectivity * 255.0)));
						int alphaPixelValue = Math.max(0, Math.min(255, (int)Math.ceil(alpha * 255.0)));
						
						roughnessData[k + (spatialRes - l - 1) * spatialRes] = alphaPixelValue << 24 | roughnessPixelValue << 16 | roughnessPixelValue << 8 | roughnessPixelValue;
						reflectivityData[k + (spatialRes - l - 1) * spatialRes] = alphaPixelValue << 24 | reflectivityPixelValue << 16 | reflectivityPixelValue << 8 | reflectivityPixelValue;
					}
				}
			}
			
			BufferedImage roughnessImg = new BufferedImage(spatialRes, spatialRes, BufferedImage.TYPE_INT_ARGB);
	        roughnessImg.setRGB(0, 0, spatialRes, spatialRes, roughnessData, 0, spatialRes);
	        ImageIO.write(roughnessImg, "PNG", new File(new File(outputDir, "textures"), "roughness.png"));
			
			BufferedImage reflectivityImg = new BufferedImage(spatialRes, spatialRes, BufferedImage.TYPE_INT_ARGB);
	        reflectivityImg.setRGB(0, 0, spatialRes, spatialRes, reflectivityData, 0, spatialRes);
	        ImageIO.write(reflectivityImg, "PNG", new File(new File(outputDir, "textures"), "debug_reflectivity_beckmann.png"));
			
			System.out.println("Wrote specular roughness image...");
			
			System.out.println("Writing debug layers...");
			for (int j = 0; j < directionalRes; j++)
			{
				int[] imageData = new int[spatialRes*spatialRes];
				
				for (int k = 0; k < spatialRes; k++)
				{
					for (int l = 0; l < spatialRes; l++)
					{
						double roughness = roughnessValues[k + l * spatialRes];
						double reflectivity = reflectivityValues[k + l * spatialRes];
						
						if (Double.isFinite(roughness) && roughness > 0.0)
						{
							double coord = (double)j / (double)(directionalRes - 1);
							double nDotH = coord + (1 - coord) * Math.sqrt(0.5);
							
							double roughnessSq = roughness * roughness;
							double nDotHSq = nDotH * nDotH;
							
							double reflectance = reflectivity * Math.exp((nDotHSq - 1) / (nDotHSq * roughnessSq)) / (nDotHSq * nDotHSq);
							
							imageData[k + (spatialRes - l - 1) * spatialRes] = 0xFF000000 |
									(Math.max(0, Math.min(255, (int)(reflectance * 255.0))) << 16) |
									(Math.max(0, Math.min(255, (int)(reflectance * 255.0))) << 8) |
									Math.max(0, Math.min(255, (int)(reflectance * 255.0)));
						}
					}
				}
				
				BufferedImage outImg = new BufferedImage(spatialRes, spatialRes, BufferedImage.TYPE_INT_ARGB);
		        outImg.setRGB(0, 0, spatialRes, spatialRes, imageData, 0, spatialRes);
		        ImageIO.write(outImg, "PNG", new File(directionalLayerDir, "debug_" + j + ".png"));

				System.out.println("Finished layer " + (1 + j) + "/" + directionalRes + " ...");
			}
			
			return roughnessValues;
		}
		else
		{
			for (int k = 0; k < spatialRes; k++)
			{
				for (int l = 0; l < spatialRes; l++)
				{
					int[] imageData = new int[directionalRes*directionalRes];
					
					for (int i = 0; i < directionalRes; i++)
					{
						for (int j = 0; j < directionalRes; j++)
						{
							float[] solutionEntry = solution[i][j][k][l];
							
							if (solutionEntry[3] > 0.0)
							{
								imageData[j + (directionalRes - i - 1) * directionalRes] = 0xFF000000 |
									(Math.max(0, Math.min(255, (int)(solutionEntry[0] * 255.0))) << 16) |
									(Math.max(0, Math.min(255, (int)(solutionEntry[1] * 255.0))) << 8) |
									Math.max(0, Math.min(255, (int)(solutionEntry[2] * 255.0)));
							}
						}
					}
					
					BufferedImage outImg = new BufferedImage(directionalRes, directionalRes, BufferedImage.TYPE_INT_ARGB);
			        outImg.setRGB(0, 0, directionalRes, directionalRes, imageData, 0, directionalRes);
			        ImageIO.write(outImg, "PNG", new File(spatialLayerDir, k + "_" + l + ".png"));
					
					System.out.println("Wrote debug image " + (1 + l + k * spatialRes) + "/" + (spatialRes * spatialRes) + " ...");
				}
			}
			
			return null; // TODO
		}
	}
	
	private void loadMesh() throws IOException
	{
		VertexMesh mesh = new VertexMesh("OBJ", objFile);
    	positionBuffer = context.createVertexBuffer().setData(mesh.getVertices());
    	texCoordBuffer = context.createVertexBuffer().setData(mesh.getTexCoords());
    	normalBuffer = context.createVertexBuffer().setData(mesh.getNormals());
    	tangentBuffer = context.createVertexBuffer().setData(mesh.getTangents());
    	center = mesh.getCentroid();
    	mesh = null;
    	System.gc(); // Garbage collect the mesh object (hopefully)
	}
	
	private Renderable<ContextType> getLightFitRenderable()
	{
		Renderable<ContextType> renderable = context.createRenderable(lightFitProgram);
    	
        renderable.addVertexBuffer("position", positionBuffer);
        renderable.addVertexBuffer("texCoord", texCoordBuffer);
        renderable.addVertexBuffer("normal", normalBuffer);
    	
        renderable.program().setUniform("viewCount", viewSet.getCameraPoseCount());
        renderable.program().setUniform("gamma", param.getGamma());
        if (viewSet.getLuminanceMap() == null)
        {
        	renderable.program().setUniform("useLuminanceMap", false);
        }
        else
        {
        	renderable.program().setUniform("useLuminanceMap", true);
        	renderable.program().setTexture("luminanceMap", viewSet.getLuminanceMap());
        }
        renderable.program().setUniform("shadowTestEnabled", false);
        renderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
        renderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
        renderable.program().setUniform("infiniteLightSources", param.areLightSourcesInfinite());
    	
        renderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
    	
    	if (!param.isImagePreprojectionUseEnabled())
    	{
    		renderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
    		renderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
    	}
    	
    	renderable.program().setUniform("delta", param.getDiffuseDelta());
    	renderable.program().setUniform("iterations", param.getDiffuseIterations());
    	
    	return renderable;
	}
	
	private static abstract class LightFit<ContextType extends Context<ContextType>>
	{
		private Renderable<ContextType> renderable;
		final int framebufferSize;
		final int framebufferSubdiv;
		
		private Vector3 position;
		private Vector3 intensity;
		
		Vector3 getPosition()
		{
			return position;
		}
		
		Vector3 getIntensity()
		{
			return intensity;
		}
		
		protected abstract void fitTexture(Renderable<ContextType> renderable, Framebuffer<ContextType> framebuffer) throws IOException;
		
		LightFit(Renderable<ContextType> renderable, int framebufferSize, int framebufferSubdiv)
		{
	    	this.renderable = renderable;
	    	this.framebufferSize = framebufferSize;
	    	this.framebufferSubdiv = framebufferSubdiv;
		}
    	
    	void fit() throws IOException
    	{
    		FramebufferObject<ContextType> framebuffer = 
				renderable.getContext().getFramebufferObjectBuilder(framebufferSize, framebufferSize)
					.addColorAttachments(new ColorAttachmentSpec(ColorFormat.RGBA32F), 2)
					.createFramebufferObject();

    		framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
	    	framebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
			
        	System.out.println("Fitting light...");
        	
        	fitTexture(renderable, framebuffer);
    	
	    	System.out.println("Aggregating light estimates...");
	    	
	    	float[] rawLightPositions = framebuffer.readFloatingPointColorBufferRGBA(0);
	        float[] rawLightIntensities = framebuffer.readFloatingPointColorBufferRGBA(1);
	        
	        framebuffer.delete(); // No need for this anymore
	        
	        Vector4 lightPositionSum = new Vector4(0, 0, 0, 0);
	        Vector4 lightIntensitySum = new Vector4(0, 0, 0, 0);
	        
	        for (int i = 0; i < framebuffer.getSize().height; i++)
	        {
	        	for (int j = 0; j < framebuffer.getSize().width; j++)
	        	{
	        		int indexStart = (i * framebuffer.getSize().width + j) * 4;
	        		lightPositionSum = lightPositionSum.plus(
	        				new Vector4(rawLightPositions[indexStart+0], rawLightPositions[indexStart+1], rawLightPositions[indexStart+2], 1.0f)
	        					.times(rawLightPositions[indexStart+3]));
	        		lightIntensitySum = lightIntensitySum.plus(
	        				new Vector4(rawLightIntensities[indexStart+0], rawLightIntensities[indexStart+1], rawLightIntensities[indexStart+2], 1.0f)
	        					.times(rawLightIntensities[indexStart+3]));
	        	}
	        }
	        
	        position = new Vector3(lightPositionSum.dividedBy(lightPositionSum.w));
	        intensity = new Vector3(lightIntensitySum.dividedBy(lightIntensitySum.w));
    	}
	}
	
	private static class TexSpaceLightFit<ContextType extends Context<ContextType>> extends LightFit<ContextType>
	{
		private File preprojDir;
		private int preprojCount;
		
		TexSpaceLightFit(Renderable<ContextType> renderable, File preprojDir, int preprojCount, int framebufferSize, int framebufferSubdiv)
		{
			super(renderable, framebufferSize, framebufferSubdiv);
			this.preprojDir = preprojDir;
			this.preprojCount = preprojCount;
		}
		
		@Override
		protected void fitTexture(Renderable<ContextType> renderable, Framebuffer<ContextType> framebuffer) throws IOException
		{
			int subdivSize = framebufferSize / framebufferSubdiv;
    		
    		for (int row = 0; row < framebufferSubdiv; row++)
	    	{
		    	for (int col = 0; col < framebufferSubdiv; col++)
	    		{
		    		Texture3D<ContextType> preprojectedViews = null;
			    	
			    	preprojectedViews = renderable.getContext().get2DColorTextureArrayBuilder(subdivSize, subdivSize, preprojCount).createTexture();
				    	
					for (int i = 0; i < preprojCount; i++)
					{
						preprojectedViews.loadLayer(i, new File(new File(preprojDir, String.format("%04d", i)), String.format("r%04dc%04d.png", row, col)), true);
					}
		    		
		    		renderable.program().setTexture("viewImages", preprojectedViews);
			    	
			    	renderable.program().setUniform("minTexCoord", 
		    				new Vector2((float)col / (float)framebufferSubdiv, (float)row / (float)framebufferSubdiv));
		    		
			    	renderable.program().setUniform("maxTexCoord", 
		    				new Vector2((float)(col+1) / (float)framebufferSubdiv, (float)(row+1) / (float)framebufferSubdiv));
			    	
			    	renderable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
			        renderable.getContext().finish();
			        
			        if (framebufferSubdiv > 1)
		    		{
		    			System.out.println("Block " + (row*framebufferSubdiv + col + 1) + "/" + (framebufferSubdiv * framebufferSubdiv) + " completed.");
		    		}
	    		}
	    	}
		}
	}
	
	private static class ImgSpaceLightFit<ContextType extends Context<ContextType>> extends LightFit<ContextType>
	{
		private Texture<ContextType> viewTextures;
		private Texture<ContextType> depthTextures;
		
		ImgSpaceLightFit(Renderable<ContextType> renderable, Texture<ContextType> viewTextures, Texture<ContextType> depthTextures, int framebufferSize, int framebufferSubdiv)
		{
			super(renderable, framebufferSize, framebufferSubdiv);
			
			this.viewTextures = viewTextures;
			this.depthTextures = depthTextures;
		}
		
		@Override
		protected void fitTexture(Renderable<ContextType> renderable, Framebuffer<ContextType> framebuffer)
		{
			int subdivSize = framebufferSize / framebufferSubdiv;
    		
    		for (int row = 0; row < framebufferSubdiv; row++)
	    	{
		    	for (int col = 0; col < framebufferSubdiv; col++)
	    		{
			    	renderable.program().setTexture("viewImages", viewTextures);
		    		renderable.program().setTexture("depthImages", depthTextures);
			    	
			    	renderable.program().setUniform("minTexCoord", 
		    				new Vector2((float)col / (float)framebufferSubdiv, (float)row / (float)framebufferSubdiv));
		    		
			    	renderable.program().setUniform("maxTexCoord", 
		    				new Vector2((float)(col+1) / (float)framebufferSubdiv, (float)(row+1) / (float)framebufferSubdiv));
			    	
			    	renderable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivSize, row * subdivSize, subdivSize, subdivSize);
			        renderable.getContext().finish();
			        
			        if (framebufferSubdiv > 1)
		    		{
		    			System.out.println("Block " + (row*framebufferSubdiv + col + 1) + "/" + (framebufferSubdiv * framebufferSubdiv) + " completed.");
		    		}
	    		}
	    	}
		}
	}
	
	private LightFit<ContextType> createTexSpaceLightFit(int framebufferSize, int framebufferSubdiv)
	{
		return new TexSpaceLightFit<ContextType>(getLightFitRenderable(), tmpDir, viewSet.getCameraPoseCount(), framebufferSize, framebufferSubdiv);
	}
	
	private LightFit<ContextType> createImgSpaceLightFit(Texture<ContextType> viewTextures, Texture<ContextType> depthTextures, int framebufferSize, int framebufferSubdiv)
	{
		return new ImgSpaceLightFit<ContextType>(getLightFitRenderable(), viewTextures, depthTextures, framebufferSize, framebufferSubdiv);
	}
	
	private Renderable<ContextType> getDiffuseFitRenderable()
	{
		Renderable<ContextType> renderable = context.createRenderable(diffuseFitProgram);
    	
    	renderable.addVertexBuffer("position", positionBuffer);
    	renderable.addVertexBuffer("texCoord", texCoordBuffer);
    	renderable.addVertexBuffer("normal", normalBuffer);
    	
    	renderable.program().setUniform("viewCount", viewSet.getCameraPoseCount());
    	renderable.program().setUniform("gamma", param.getGamma());
    	renderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
    	renderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
    	renderable.program().setUniform("infiniteLightSources", param.areLightSourcesInfinite());
    	if (viewSet.getLuminanceMap() == null)
        {
        	renderable.program().setUniform("useLuminanceMap", false);
        }
        else
        {
        	renderable.program().setUniform("useLuminanceMap", true);
        	renderable.program().setTexture("luminanceMap", viewSet.getLuminanceMap());
        }
    	
    	renderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
    	
    	if (!param.isImagePreprojectionUseEnabled())
    	{
	    	renderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
	    	renderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
    	}
    	
    	renderable.program().setUniform("delta", param.getDiffuseDelta());
    	renderable.program().setUniform("iterations", param.getDiffuseIterations());
    	renderable.program().setUniform("fit1Weight", param.getDiffuseInputNormalWeight());
    	renderable.program().setUniform("fit3Weight", param.getDiffuseComputedNormalWeight());
    	
		renderable.program().setUniformBuffer("LightIndices", viewSet.getLightIndexBuffer());
    	
    	if (lightPositionBuffer != null)
    	{
    		renderable.program().setUniformBuffer("LightPositions", lightPositionBuffer);
    	}
    	else
    	{
    		renderable.program().setUniformBuffer("LightPositions", viewSet.getLightPositionBuffer());
    	}
    	
    	if (lightIntensityBuffer != null)
    	{
    		renderable.program().setUniformBuffer("LightIntensities", lightIntensityBuffer);
    	}
    	else
    	{
    		renderable.program().setUniformBuffer("LightIntensities", viewSet.getLightIntensityBuffer());
    	}
    	
    	if (shadowMatrixBuffer != null)
    	{
	    	renderable.program().setUniform("shadowTestEnabled", param.isCameraVisibilityTestEnabled());
    		renderable.program().setUniformBuffer("ShadowMatrices", shadowMatrixBuffer);
    	}
    	else
    	{
    		renderable.program().setUniform("shadowTestEnabled", false);
    	}
    	
    	return renderable;
	}
	
	private static class DiffuseFit<ContextType extends Context<ContextType>>
	{
		private Framebuffer<ContextType> framebuffer;
		private Renderable<ContextType> renderable;
		
		private int subdiv;
		private int subdivWidth;
		private int subdivHeight;
		
		DiffuseFit(Renderable<ContextType> renderable, Framebuffer<ContextType> framebuffer, int subdiv)
		{
			this.subdiv = subdiv;
			subdivWidth = framebuffer.getSize().width / subdiv;
			subdivHeight = framebuffer.getSize().height / subdiv;
			
    		this.framebuffer = framebuffer;
	    	this.renderable = renderable;
		}
		
		void fit(int row, int col, Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages)
		{
			renderable.program().setTexture("viewImages", viewImages);
	    	renderable.program().setTexture("depthImages", depthImages);
	    	renderable.program().setTexture("shadowImages", shadowImages);
	    	
	    	renderable.program().setUniform("minTexCoord", 
    				new Vector2((float)col / (float)subdiv, (float)row / (float)subdiv));
    		
	    	renderable.program().setUniform("maxTexCoord", 
    				new Vector2((float)(col+1) / (float)subdiv, (float)(row+1) / (float)subdiv));
	    	
	        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivWidth, row * subdivHeight, subdivWidth, subdivHeight);
	        renderable.getContext().finish();
		}
	}
	
	private DiffuseFit<ContextType> createDiffuseFit(Framebuffer<ContextType> framebuffer, int subdiv)
	{
		return new DiffuseFit<ContextType>(getDiffuseFitRenderable(), framebuffer, subdiv);
	}
	
	private Renderable<ContextType> getSpecularFitRenderable()
	{
		Renderable<ContextType> renderable = context.createRenderable(specularFitProgram);
    	
    	renderable.addVertexBuffer("position", positionBuffer);
    	renderable.addVertexBuffer("texCoord", texCoordBuffer);
    	renderable.addVertexBuffer("normal", normalBuffer);

    	renderable.program().setUniform("viewCount", viewSet.getCameraPoseCount());
    	renderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
    	
    	if (!param.isImagePreprojectionUseEnabled())
    	{
	    	renderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
	    	renderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
    	}

    	renderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
    	renderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
    	renderable.program().setUniform("gamma", param.getGamma());
    	renderable.program().setUniform("infiniteLightSources", param.areLightSourcesInfinite());
    	if (viewSet.getLuminanceMap() == null)
        {
        	renderable.program().setUniform("useLuminanceMap", false);
        }
        else
        {
        	renderable.program().setUniform("useLuminanceMap", true);
        	renderable.program().setTexture("luminanceMap", viewSet.getLuminanceMap());
        }
    	
    	renderable.program().setUniform("computeRoughness", param.isSpecularRoughnessComputationEnabled());
    	renderable.program().setUniform("computeNormal", param.isSpecularNormalComputationEnabled());
    	renderable.program().setUniform("trueBlinnPhong", param.isTrueBlinnPhongSpecularEnabled());

    	renderable.program().setUniform("diffuseRemovalAmount", param.getSpecularSubtractDiffuseAmount());
    	renderable.program().setUniform("specularInfluenceScale", param.getSpecularInfluenceScale());
    	renderable.program().setUniform("determinantThreshold", param.getSpecularDeterminantThreshold());
    	renderable.program().setUniform("fit1Weight", param.getSpecularInputNormalDefaultRoughnessWeight());
    	renderable.program().setUniform("fit2Weight", param.getSpecularInputNormalComputedRoughnessWeight());
    	renderable.program().setUniform("fit4Weight", param.getSpecularComputedNormalWeight());
    	renderable.program().setUniform("defaultSpecularColor", new Vector3(0.0f, 0.0f, 0.0f));
    	renderable.program().setUniform("defaultSpecularRoughness", param.getDefaultSpecularRoughness());
    	renderable.program().setUniform("specularRoughnessScale", param.getSpecularRoughnessCap());

		renderable.program().setUniformBuffer("LightIndices", viewSet.getLightIndexBuffer());
    	
    	if (lightPositionBuffer != null)
    	{
    		renderable.program().setUniformBuffer("LightPositions", lightPositionBuffer);
    	}
    	else
    	{
    		renderable.program().setUniformBuffer("LightPositions", viewSet.getLightPositionBuffer());
    	}
    	
    	if (lightIntensityBuffer != null)
    	{
    		renderable.program().setUniformBuffer("LightIntensities", lightIntensityBuffer);
    	}
    	else
    	{
    		renderable.program().setUniformBuffer("LightIntensities", viewSet.getLightIntensityBuffer());
    	}
    	
    	if (shadowMatrixBuffer != null)
    	{
    		renderable.program().setUniform("shadowTestEnabled", param.isCameraVisibilityTestEnabled());
    		renderable.program().setUniformBuffer("ShadowMatrices", shadowMatrixBuffer);
    	}
    	else
    	{
    		renderable.program().setUniform("shadowTestEnabled", false);
    	}
    	
    	return renderable;
	}
	
	private static class SpecularFit<ContextType extends Context<ContextType>>
	{
		private Framebuffer<ContextType> framebuffer;
		private Renderable<ContextType> renderable;

		private int subdiv;
		private int subdivWidth;
		private int subdivHeight;
		
		SpecularFit(Renderable<ContextType> renderable, Framebuffer<ContextType> framebuffer, int subdiv)
		{
			this.subdiv = subdiv;
			subdivWidth = framebuffer.getSize().width / subdiv;
			subdivHeight = framebuffer.getSize().height / subdiv;
			
    		this.framebuffer = framebuffer;
	        this.renderable = renderable;
		}
		
		void fit(int row, int col, Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages, 
				Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate)
		{
			renderable.program().setTexture("viewImages", viewImages);
	    	renderable.program().setTexture("depthImages", depthImages);
	    	renderable.program().setTexture("shadowImages", shadowImages);
	    	
	    	renderable.program().setUniform("minTexCoord", 
    				new Vector2((float)col / (float)subdiv, (float)row / (float)subdiv));
    		
	    	renderable.program().setUniform("maxTexCoord", 
    				new Vector2((float)(col+1) / (float)subdiv, (float)(row+1) / (float)subdiv));

	    	renderable.program().setTexture("diffuseEstimate", diffuseEstimate);
	    	renderable.program().setTexture("normalEstimate", normalEstimate);

	        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivWidth, row * subdivHeight, subdivWidth, subdivHeight);
	        renderable.getContext().finish();
		}
	}
	
	private SpecularFit<ContextType> createSpecularFit(Framebuffer<ContextType> framebuffer, int subdiv)
	{
		return new SpecularFit<ContextType>(getSpecularFitRenderable(), framebuffer, subdiv);
	}
	
	private Renderable<ContextType> getSpecularFit2Renderable()
	{
		Renderable<ContextType> renderable = context.createRenderable(specularFit2Program);
    	
    	renderable.addVertexBuffer("position", positionBuffer);
    	renderable.addVertexBuffer("texCoord", texCoordBuffer);
    	renderable.addVertexBuffer("normal", normalBuffer);

    	renderable.program().setUniform("viewCount", viewSet.getCameraPoseCount());
    	renderable.program().setUniformBuffer("CameraPoses", viewSet.getCameraPoseBuffer());
    	
    	if (!param.isImagePreprojectionUseEnabled())
    	{
	    	renderable.program().setUniformBuffer("CameraProjections", viewSet.getCameraProjectionBuffer());
	    	renderable.program().setUniformBuffer("CameraProjectionIndices", viewSet.getCameraProjectionIndexBuffer());
    	}

    	renderable.program().setUniform("occlusionEnabled", param.isCameraVisibilityTestEnabled());
    	renderable.program().setUniform("occlusionBias", param.getCameraVisibilityTestBias());
    	renderable.program().setUniform("gamma", param.getGamma());
    	renderable.program().setUniform("infiniteLightSources", param.areLightSourcesInfinite());

    	if (viewSet.getLuminanceMap() == null)
        {
        	renderable.program().setUniform("useLuminanceMap", false);
        }
        else
        {
        	renderable.program().setUniform("useLuminanceMap", true);
        	renderable.program().setTexture("luminanceMap", viewSet.getLuminanceMap());
        }
    	
		renderable.program().setUniformBuffer("LightIndices", viewSet.getLightIndexBuffer());
    	
    	if (lightPositionBuffer != null)
    	{
    		renderable.program().setUniformBuffer("LightPositions", lightPositionBuffer);
    	}
    	else
    	{
    		renderable.program().setUniformBuffer("LightPositions", viewSet.getLightPositionBuffer());
    	}
    	
    	if (lightIntensityBuffer != null)
    	{
    		renderable.program().setUniformBuffer("LightIntensities", lightIntensityBuffer);
    	}
    	else
    	{
    		renderable.program().setUniformBuffer("LightIntensities", viewSet.getLightIntensityBuffer());
    	}
    	
    	if (shadowMatrixBuffer != null)
    	{
    		renderable.program().setUniform("shadowTestEnabled", param.isCameraVisibilityTestEnabled());
    		renderable.program().setUniformBuffer("ShadowMatrices", shadowMatrixBuffer);
    	}
    	else
    	{
    		renderable.program().setUniform("shadowTestEnabled", false);
    	}
    	
    	return renderable;
	}
	
	private static class SpecularFit2<ContextType extends Context<ContextType>>
	{
		private Framebuffer<ContextType> framebuffer;
		private Renderable<ContextType> renderable;

		private int subdiv;
		private int subdivWidth;
		private int subdivHeight;
		
		SpecularFit2(Renderable<ContextType> renderable, Framebuffer<ContextType> framebuffer, int subdiv)
		{
			this.subdiv = subdiv;
			subdivWidth = framebuffer.getSize().width / subdiv;
			subdivHeight = framebuffer.getSize().height / subdiv;
			
    		this.framebuffer = framebuffer;
	        this.renderable = renderable;
		}
		
		void fit(int row, int col, Texture<ContextType> viewImages, Texture<ContextType> depthImages, Texture<ContextType> shadowImages, 
				Texture<ContextType> diffuseEstimate, Texture<ContextType> normalEstimate, Texture<ContextType> roughnessEstimate)
		{
			renderable.program().setTexture("viewImages", viewImages);
	    	renderable.program().setTexture("depthImages", depthImages);
	    	renderable.program().setTexture("shadowImages", shadowImages);
	    	
	    	renderable.program().setUniform("minTexCoord", 
    				new Vector2((float)col / (float)subdiv, (float)row / (float)subdiv));
    		
	    	renderable.program().setUniform("maxTexCoord", 
    				new Vector2((float)(col+1) / (float)subdiv, (float)(row+1) / (float)subdiv));

	    	renderable.program().setTexture("diffuseEstimate", diffuseEstimate);
	    	renderable.program().setTexture("normalEstimate", normalEstimate);
	    	renderable.program().setTexture("roughnessEstimate", roughnessEstimate);

	        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer, col * subdivWidth, row * subdivHeight, subdivWidth, subdivHeight);
	        renderable.getContext().finish();
		}
	}
	
	private SpecularFit2<ContextType> createSpecularFit2(Framebuffer<ContextType> framebuffer, int subdiv)
	{
		return new SpecularFit2<ContextType>(getSpecularFit2Renderable(), framebuffer, subdiv);
	}

	public void execute() throws IOException
	{	
//		final int DEBUG_PIXEL_X = 322;
//		final int DEBUG_PIXEL_Y = param.getTextureSize() - 365;

    	System.out.println("Max vertex uniform components across all blocks:" + context.getMaxCombinedVertexUniformComponents());
    	System.out.println("Max fragment uniform components across all blocks:" + context.getMaxCombinedFragmentUniformComponents());
    	System.out.println("Max size of a uniform block in bytes:" + context.getMaxUniformBlockSize());
    	System.out.println("Max texture array layers:" + context.getMaxArrayTextureLayers());
		
		System.out.println("Loading view set...");
    	Date timestamp = new Date();
		
    	String[] vsetFileNameParts = vsetFile.getName().split("\\.");
    	String fileExt = vsetFileNameParts[vsetFileNameParts.length-1];
    	if (fileExt.equalsIgnoreCase("vset"))
    	{
    		System.out.println("Loading from VSET file.");
    		viewSet = ViewSet.loadFromVSETFile(vsetFile, context);
    	}
    	else if (fileExt.equalsIgnoreCase("xml"))
    	{
    		System.out.println("Loading from Agisoft Photoscan XML file.");
    		viewSet = ViewSet.loadFromAgisoftXMLFile(vsetFile, null, lightPosition, lightIntensity, context, null);
    	}
    	else
    	{
    		System.out.println("Unrecognized file type, aborting.");
    		return;
    	}
    	
    	outputDir.mkdir();
    	if (DEBUG)
    	{
    		new File(outputDir, "debug").mkdir();
    	}
    	
    	System.out.println("Loading view set completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	
    	if(viewSet.getLuminanceMap() == null)
    	{
    		System.out.println("WARNING: no luminance mapping found.  Reflectance values are not physically grounded.");
    	}
    	
    	System.out.println("Loading and compiling shader programs...");
    	timestamp = new Date();
		
		context.enableDepthTest();
    	context.enableBackFaceCulling();
    	
    	try
    	{
	    	
	    	depthRenderingProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders/common/depth.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders/common/depth.frag"))
	    			.createProgram();
	    	
	    	projTexProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders", "reflectance/projtex_single.frag"))
	    			.createProgram();
	    	
	    	lightFitProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders", param.isImagePreprojectionUseEnabled() ? 
	    					"texturefit/lightfit_texspace.frag" : "texturefit/lightfit_imgspace.frag"))
	    			.createProgram();
	    	
	    	diffuseFitProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders", param.isImagePreprojectionUseEnabled() ? 
	    					"texturefit/diffusefit_texspace.frag" : "texturefit/diffusefit_imgspace.frag"))
	    			.createProgram();
			
	    	specularFitProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders", param.isImagePreprojectionUseEnabled() ? 
	    					"texturefit/specularfit_texspace.frag" : "texturefit/specularfit_imgspace.frag"))
	    			.createProgram();
			
	    	specularFit2Program = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders", param.isImagePreprojectionUseEnabled() ? 
	    					"texturefit/specularfit2_texspace.frag" : "texturefit/specularfit2_imgspace.frag"))
	    			.createProgram();
	    	
	    	specularResidProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders", param.isImagePreprojectionUseEnabled() ? 
	    					"texturefit/specularresid_texspace.frag" : "texturefit/specularresid_imgspace.frag"))
	    			.createProgram();
			
	    	diffuseDebugProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders", "reflectance/projtex_multi.frag"))
	    			.createProgram();
			
	    	specularDebugProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texspace.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders", "texturefit/specularresid_imgspace.frag"))
	    			.createProgram();
			
	    	textureRectProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texture.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders", "common/texture.frag"))
	    			.createProgram();
			
	    	holeFillProgram = context.getShaderProgramBuilder()
	    			.addShader(ShaderType.VERTEX, new File("shaders", "common/texture.vert"))
	    			.addShader(ShaderType.FRAGMENT, new File("shaders", "texturefit/holefill.frag"))
	    			.createProgram();
			
	    	System.out.println("Shader compilation completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    	
	    	System.out.println("Loading mesh...");
	    	timestamp = new Date();
	    	
	    	loadMesh();
	    	
	    	System.out.println("Loading mesh completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    	
	    	tmpDir = new File(outputDir, "tmp");
	    	
	    	Texture3D<ContextType> viewTextures = null;
	    	Texture3D<ContextType> depthTextures = null;
	    	Texture3D<ContextType> shadowTextures = null;
	    	
	    	if (param.isImagePreprojectionUseEnabled() && param.isImagePreprojectionGenerationEnabled())
	    	{
	    		System.out.println("Pre-projecting images into texture space...");
		    	timestamp = new Date();
		    	
		    	tmpDir.mkdir();
		    	
		    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
		    	{
		    		File viewDir = new File(tmpDir, String.format("%04d", i));
		        	viewDir.mkdir();
		        	
		    		projectIntoTextureSpace(viewSet, projTexProgram, i, param.getTextureSize(), param.getTextureSubdivision(), 
		    			(framebuffer, row, col) -> 
	    				{
		    				try
		    				{
		    					framebuffer.saveColorBufferToFile(0, "PNG", new File(viewDir, String.format("r%04dc%04d.png", row, col)));
		    				}
		    				catch (IOException e)
		    				{
		    					e.printStackTrace();
		    				}
						});
			    	
			    	System.out.println("Completed " + (i+1) + "/" + viewSet.getCameraPoseCount());
		    	}
		    	
		    	System.out.println("Pre-projections completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    	}
	    	else
	    	{
	    		if (param.isImageRescalingEnabled())
	    		{
	    			System.out.println("Loading and rescaling images...");
	    			timestamp = new Date();
	    			
	    			viewTextures = context.get2DColorTextureArrayBuilder(param.getImageWidth(), param.getImageHeight(), viewSet.getCameraPoseCount())
	    							.setLinearFilteringEnabled(true)
	    							//.setMipmapsEnabled(true)
	    							.createTexture();
	    			
					// Create an FBO for downsampling
			    	FramebufferObject<ContextType> downsamplingFBO = 
		    			context.getFramebufferObjectBuilder(param.getImageWidth(), param.getImageHeight())
		    				.addEmptyColorAttachment()
		    				.createFramebufferObject();
			    	
			    	Renderable<ContextType> downsampleRenderable = context.createRenderable(textureRectProgram);
			    	VertexBuffer<ContextType> rectBuffer = context.createRectangle();
			    	downsampleRenderable.addVertexBuffer("position", rectBuffer);
			    	
			    	// Downsample and store each image
			    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
			    	{
			    		File imageFile = new File(imageDir, viewSet.getImageFileName(i));
						if (!imageFile.exists())
						{
							String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
					    	filenameParts[filenameParts.length - 1] = "png";
					    	String pngFileName = String.join(".", filenameParts);
					    	imageFile = new File(imageDir, pngFileName);
						}
			    		
			    		Texture2D<ContextType> fullSizeImage;
			    		if (maskDir == null)
		    			{
			    			fullSizeImage = context.get2DColorTextureBuilder(imageFile, true)
			    								.setLinearFilteringEnabled(true)
			    								.setMipmapsEnabled(true)
			    								.createTexture();
		    			}
			    		else
			    		{
			    			File maskFile = new File(maskDir, viewSet.getImageFileName(i));
							if (!maskFile.exists())
							{
								String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
						    	filenameParts[filenameParts.length - 1] = "png";
						    	String pngFileName = String.join(".", filenameParts);
						    	maskFile = new File(maskDir, pngFileName);
							}
							
			    			fullSizeImage = context.get2DColorTextureBuilder(imageFile, maskFile, true)
												.setLinearFilteringEnabled(true)
												.setMipmapsEnabled(true)
												.createTexture();
			    		}
			    		
			    		downsamplingFBO.setColorAttachment(0, viewTextures.getLayerAsFramebufferAttachment(i));
			    		downsamplingFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
			        	
			    		textureRectProgram.setTexture("tex", fullSizeImage);
			        	
			        	downsampleRenderable.draw(PrimitiveMode.TRIANGLE_FAN, downsamplingFBO);
			        	context.finish();
			        	
			        	if (rescaleDir != null)
			        	{
					    	String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
					    	filenameParts[filenameParts.length - 1] = "png";
					    	String pngFileName = String.join(".", filenameParts);
			        		downsamplingFBO.saveColorBufferToFile(0, "PNG", new File(rescaleDir, pngFileName));
			        	}
			        	
			        	fullSizeImage.delete();
			        	
						System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " images loaded and rescaled.");
			    	}
		
			    	rectBuffer.delete();
			    	downsamplingFBO.delete();
			    	
			    	// TODO why don't mipmaps work?
			    	//viewTextures.generateMipmaps();
			    	//context.finish();
			    	
		    		System.out.println("Image loading and rescaling completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    		
		    		System.err.println("Warning: Image rescaling is buggy; it may be necessary to reload the rescaled images and execute again."); // TODO fix this
	    		}
	    		else
	    		{
		    		System.out.println("Loading images...");
			    	timestamp = new Date();
			    	
			    	// Read a single image to get the dimensions for the texture array
			    	File imageFile = new File(imageDir, viewSet.getImageFileName(0));
					if (!imageFile.exists())
					{
						String[] filenameParts = viewSet.getImageFileName(0).split("\\.");
				    	filenameParts[filenameParts.length - 1] = "png";
				    	String pngFileName = String.join(".", filenameParts);
				    	imageFile = new File(imageDir, pngFileName);
					}
					BufferedImage img = ImageIO.read(new FileInputStream(imageFile));
					viewTextures = context.get2DColorTextureArrayBuilder(img.getWidth(), img.getHeight(), viewSet.getCameraPoseCount())
									.setLinearFilteringEnabled(true)
									.setMipmapsEnabled(true)
									.createTexture();
					
					for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
					{
						imageFile = new File(imageDir, viewSet.getImageFileName(i));
						if (!imageFile.exists())
						{
							String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
					    	filenameParts[filenameParts.length - 1] = "png";
					    	String pngFileName = String.join(".", filenameParts);
					    	imageFile = new File(imageDir, pngFileName);
						}
						
						if (maskDir == null)
						{
							viewTextures.loadLayer(i, imageFile, true);
						}
						else
						{
							File maskFile = new File(maskDir, viewSet.getImageFileName(i));
							if (!maskFile.exists())
							{
								String[] filenameParts = viewSet.getImageFileName(i).split("\\.");
						    	filenameParts[filenameParts.length - 1] = "png";
						    	String pngFileName = String.join(".", filenameParts);
						    	maskFile = new File(maskDir, pngFileName);
							}
							
							viewTextures.loadLayer(i, imageFile, maskFile, true);
						}
						
						System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " images loaded.");
					}
			    	
		    		System.out.println("Image loading completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    		}
	    		
	    		System.out.println("Creating depth maps...");
		    	timestamp = new Date();
		    	
		    	// Build depth textures for each view
		    	int width = viewTextures.getWidth();
		    	int height = viewTextures.getHeight();
		    	depthTextures = context.get2DDepthTextureArrayBuilder(width, height, viewSet.getCameraPoseCount()).createTexture();
		    	
		    	// Don't automatically generate any texture attachments for this framebuffer object
		    	FramebufferObject<ContextType> depthRenderingFBO = context.getFramebufferObjectBuilder(width, height).createFramebufferObject();
		    	
		    	Renderable<ContextType> depthRenderable = context.createRenderable(depthRenderingProgram);
		    	depthRenderable.addVertexBuffer("position", positionBuffer);
		    	
		    	// Render each depth texture
		    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
		    	{
		    		depthRenderingFBO.setDepthAttachment(depthTextures.getLayerAsFramebufferAttachment(i));
		        	depthRenderingFBO.clearDepthBuffer();
		        	
		        	depthRenderingProgram.setUniform("model_view", viewSet.getCameraPose(i));
		    		depthRenderingProgram.setUniform("projection", 
						viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
		    				.getProjectionMatrix(
								viewSet.getRecommendedNearPlane(), 
								viewSet.getRecommendedFarPlane()
							)
					);
		        	
		        	depthRenderable.draw(PrimitiveMode.TRIANGLES, depthRenderingFBO);
					//System.out.println((i+1) + "/" + viewSet.getCameraPoseCount() + " depth maps created.");
		    	}
	
		    	depthRenderingFBO.delete();
		    	
	    		System.out.println("Depth maps created in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    	}
	    	
	    	if (!param.areLightSourcesInfinite())
	    	{
		    	System.out.println("Beginning light fit...");
		    	timestamp = new Date();
		    	
		    	LightFit<ContextType> lightFit;
		    	
		    	if (param.isImagePreprojectionUseEnabled())
		    	{
		    		lightFit = createTexSpaceLightFit(param.getTextureSize(), param.getTextureSubdivision());
		    	}
		    	else
		    	{
		    		lightFit = createImgSpaceLightFit(viewTextures, depthTextures, param.getTextureSize(), param.getTextureSubdivision());
		    	}
	
	    		lightFit.fit();
	    		
	    		lightPosition = lightFit.getPosition();
	    		lightIntensity = lightFit.getIntensity();
		        
		        System.out.println("Light position: " + lightPosition.x + " " + lightPosition.y + " " + lightPosition.z);
		        System.out.println("Light intensity: " + lightIntensity.x + " " + lightIntensity.y + " " + lightIntensity.z);
		        
		        viewSet.setLightPosition(0, lightPosition);
		        viewSet.setLightIntensity(0, lightIntensity);
		        viewSet.writeVSETFileToStream(new FileOutputStream(new File(outputDir, "default.vset")));
		        
		        System.out.println("Light fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		        
		        if (!param.isImagePreprojectionUseEnabled())
		        {
			        System.out.println("Creating shadow maps...");
			    	timestamp = new Date();
			    	
			    	// Build shadow maps for each view
			    	int width = viewTextures.getWidth();
			    	int height = viewTextures.getHeight();
			    	shadowTextures = context.get2DDepthTextureArrayBuilder(width, height, viewSet.getCameraPoseCount()).createTexture();
			    	
			    	// Don't automatically generate any texture attachments for this framebuffer object
			    	FramebufferObject<ContextType> shadowRenderingFBO = context.getFramebufferObjectBuilder(width, height).createFramebufferObject();
			    	
			    	Renderable<ContextType> shadowRenderable = context.createRenderable(depthRenderingProgram);
			    	shadowRenderable.addVertexBuffer("position", positionBuffer);
			    	
			    	// Flatten the camera pose matrices into 16-component vectors and store them in the vertex list data structure.
			    	FloatVertexList flattenedShadowMatrices = new FloatVertexList(16, viewSet.getCameraPoseCount());
			    	
			    	// Render each shadow map
			    	for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
			    	{
			    		shadowRenderingFBO.setDepthAttachment(shadowTextures.getLayerAsFramebufferAttachment(i));
			    		shadowRenderingFBO.clearDepthBuffer();
			    		
			    		Matrix4 modelView = Matrix4.lookAt(new Vector3(viewSet.getCameraPoseInverse(i).times(new Vector4(lightPosition, 1.0f))), center, new Vector3(0, 1, 0));
			        	depthRenderingProgram.setUniform("model_view", modelView);
			        	
			    		Matrix4 projection = viewSet.getCameraProjection(viewSet.getCameraProjectionIndex(i))
								.getProjectionMatrix(
									viewSet.getRecommendedNearPlane(), 
									viewSet.getRecommendedFarPlane() * SHADOW_MAP_FAR_PLANE_CUSHION // double it for good measure
								);
			    		depthRenderingProgram.setUniform("projection", projection);
			        	
			    		shadowRenderable.draw(PrimitiveMode.TRIANGLES, shadowRenderingFBO);
			    		
			    		Matrix4 fullTransform = projection.times(modelView);
			    		
			    		int d = 0;
						for (int col = 0; col < 4; col++) // column
						{
							for (int row = 0; row < 4; row++) // row
							{
								flattenedShadowMatrices.set(i, d, fullTransform.get(row, col));
								d++;
							}
						}
			    	}
					
					// Create the uniform buffer
					shadowMatrixBuffer = context.createUniformBuffer().setData(flattenedShadowMatrices);
			
			    	shadowRenderingFBO.delete();
			    	
					System.out.println("Shadow maps created in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		        }
				
		    	FloatVertexList lightPositionList = new FloatVertexList(4, 1);
		    	lightPositionList.set(0, 0, lightPosition.x);
		    	lightPositionList.set(0, 1, lightPosition.y);
		    	lightPositionList.set(0, 2, lightPosition.z);
		    	lightPositionList.set(0, 3, 1.0f);
		    	
		        FloatVertexList lightIntensityList = new FloatVertexList(3, 1);
		        lightIntensityList.set(0, 0, lightIntensity.x);
		        lightIntensityList.set(0, 1, lightIntensity.y);
		        lightIntensityList.set(0, 2, lightIntensity.z);
		        
		        lightPositionBuffer = context.createUniformBuffer().setData(lightPositionList);
		        lightIntensityBuffer = context.createUniformBuffer().setData(lightIntensityList);
	    	}
			
			// Phong regression
	        
	        if (param.getTextureSubdivision() > 1)
	    	{
		    	System.out.println("Beginning model fitting (" + (param.getTextureSubdivision() * param.getTextureSubdivision()) + " blocks)...");
	    	}
	    	else
	    	{
		    	System.out.println("Setting up model fitting...");
	    	}
	    	timestamp = new Date();
	    	
	    	FramebufferObject<ContextType> diffuseFitFramebuffer = 
    			context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
					.addColorAttachments(4)
					.createFramebufferObject();

	    	FramebufferObject<ContextType> specularFitFramebuffer = 
    			context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
					.addColorAttachments(4)
					.createFramebufferObject();

	    	diffuseFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
	    	diffuseFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
	    	diffuseFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
	    	diffuseFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
	    	
	    	specularFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
	    	specularFitFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
	    	specularFitFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
	    	specularFitFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
	    	
    		DiffuseFit<ContextType> diffuseFit = createDiffuseFit(diffuseFitFramebuffer, param.getTextureSubdivision());
        	SpecularFit<ContextType> specularFit = createSpecularFit(specularFitFramebuffer, param.getTextureSubdivision());
        	
        	File diffuseTempDirectory = new File(tmpDir, "diffuse");
	    	File normalTempDirectory = new File(tmpDir, "normal");
	    	File specularTempDirectory = new File(tmpDir, "specular");
	    	File roughnessTempDirectory = new File(tmpDir, "roughness");
	    	File snormalTempDirectory = new File(tmpDir, "snormal");
	    	
	    	diffuseTempDirectory.mkdir();
	    	normalTempDirectory.mkdir();
	    	specularTempDirectory.mkdir();
	    	roughnessTempDirectory.mkdir();
	    	snormalTempDirectory.mkdir();
	    	
	    	int subdivSize = param.getTextureSize() / param.getTextureSubdivision();
        
        	if (param.getTextureSubdivision() == 1)
        	{
        		Texture3D<ContextType> preprojectedViews = null;
	        	
        		System.out.println("Setup finished in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	        	System.out.println("Fitting diffuse...");
		    	timestamp = new Date();
		    	
		    	if (param.isImagePreprojectionUseEnabled())
		    	{
	    			preprojectedViews = context.get2DColorTextureArrayBuilder(subdivSize, subdivSize, viewSet.getCameraPoseCount()).createTexture();
			    	
					for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
					{
						preprojectedViews.loadLayer(i, new File(new File(tmpDir, String.format("%04d", i)), String.format("r%04dc%04d.png", 0, 0)), true);
					}
		    	
			    	diffuseFit.fit(0, 0, preprojectedViews, null, null);
		    		
			        System.out.println("Diffuse fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
			        	
		        	System.out.println("Fitting specular...");
		        	timestamp = new Date();
		        
		        	specularFit.fit(0, 0, preprojectedViews, null, null, diffuseFitFramebuffer.getColorAttachmentTexture(0), diffuseFitFramebuffer.getColorAttachmentTexture(1));
		        	
		    		preprojectedViews.delete();
		    	}
	    		else
	    		{
	    			diffuseFit.fit(0, 0, viewTextures, depthTextures, shadowTextures);
		    		
			        System.out.println("Diffuse fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
			        	
		        	System.out.println("Fitting specular...");
		        	timestamp = new Date();
			        
			        specularFit.fit(0, 0, viewTextures, depthTextures, shadowTextures, diffuseFitFramebuffer.getColorAttachmentTexture(0), diffuseFitFramebuffer.getColorAttachmentTexture(1));
	    		}
	    		
		    	System.out.println("Specular fit completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
        	}
        	else
        	{
        		for (int row = 0; row < param.getTextureSubdivision(); row++)
    	    	{
    		    	for (int col = 0; col < param.getTextureSubdivision(); col++)
    	    		{
    	        		if (param.isImagePreprojectionUseEnabled())
    			    	{
    	        			Texture3D<ContextType> preprojectedViews = context.get2DColorTextureArrayBuilder(subdivSize, subdivSize, viewSet.getCameraPoseCount()).createTexture();
    				    	
    						for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
    						{
    							preprojectedViews.loadLayer(i, new File(new File(tmpDir, String.format("%04d", i)), String.format("r%04dc%04d.png", row, col)), true);
    						}

        	        		diffuseFit.fit(row, col, preprojectedViews, null, null);
        	        		
        	        		diffuseFitFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
    	    		        		"PNG", new File(diffuseTempDirectory, String.format("r%04dc%04d.png", row, col)));
    	    		        
    	    		        diffuseFitFramebuffer.saveColorBufferToFile(1, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
    	    		        		"PNG", new File(normalTempDirectory, String.format("r%04dc%04d.png", row, col)));
    	    		        
    	    		        specularFit.fit(row, col, preprojectedViews, null, null, diffuseFitFramebuffer.getColorAttachmentTexture(0), diffuseFitFramebuffer.getColorAttachmentTexture(1));

    	    	    		specularFitFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
    	    		        		"PNG", new File(specularTempDirectory, String.format("r%04dc%04d.png", row, col)));
    	    		        
    	    	    		specularFitFramebuffer.saveColorBufferToFile(1, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
    	    		        		"PNG", new File(roughnessTempDirectory, String.format("r%04dc%04d.png", row, col)));
    	    		        
    	    	    		specularFitFramebuffer.saveColorBufferToFile(2, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
    	    		        		"PNG", new File(snormalTempDirectory, String.format("r%04dc%04d.png", row, col)));

    			    		preprojectedViews.delete();
    			    	}
    	        		else
    	        		{
	    	        		diffuseFit.fit(row, col, viewTextures, depthTextures, shadowTextures);
	    	        		specularFit.fit(row, col, viewTextures, depthTextures, shadowTextures, diffuseFitFramebuffer.getColorAttachmentTexture(0), diffuseFitFramebuffer.getColorAttachmentTexture(1));
    	        		}
    	        		
    	        		System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
    	    		}
    	    	}
	        	
	        	System.out.println("Model fitting completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
        	}
	    	
	    	System.out.println("Filling empty regions...");
	    	timestamp = new Date();
	    	
	    	FramebufferObject<ContextType> holeFillBackFBO = 
				context.getFramebufferObjectBuilder(param.getTextureSize(), param.getTextureSize())
					.addColorAttachments(4)
					.createFramebufferObject();
	    	
	    	Renderable<ContextType> holeFillRenderable = context.createRenderable(holeFillProgram);
	    	VertexBuffer<ContextType> rectBuffer = context.createRectangle();
	    	holeFillRenderable.addVertexBuffer("position", rectBuffer);
	    	
	    	holeFillProgram.setUniform("minFillAlpha", 0.5f);
			
			System.out.println("Diffuse fill...");
	    	
	    	// Diffuse
	    	FramebufferObject<ContextType> holeFillFrontFBO = diffuseFitFramebuffer;
	    	for (int i = 0; i < param.getTextureSize() / 2; i++)
	    	{
	    		holeFillBackFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 1.0f);
	    		holeFillBackFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
	    		holeFillBackFBO.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
	    		holeFillBackFBO.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
	    		
	    		holeFillProgram.setTexture("input0", holeFillFrontFBO.getColorAttachmentTexture(0));
	    		holeFillProgram.setTexture("input1", holeFillFrontFBO.getColorAttachmentTexture(1));
	    		holeFillProgram.setTexture("input2", holeFillFrontFBO.getColorAttachmentTexture(2));
	    		holeFillProgram.setTexture("input3", holeFillFrontFBO.getColorAttachmentTexture(3));
	    		
	    		holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, holeFillBackFBO);
	    		context.finish();
	    		
	    		FramebufferObject<ContextType> tmp = holeFillFrontFBO;
	    		holeFillFrontFBO = holeFillBackFBO;
	    		holeFillBackFBO = tmp;
	    	}
	    	diffuseFitFramebuffer = holeFillFrontFBO;
			
			System.out.println("Specular fill...");
	    	
	    	// Specular
	    	holeFillFrontFBO = specularFitFramebuffer;
	    	for (int i = 0; i < param.getTextureSize() / 2; i++)
	    	{
	    		holeFillBackFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
	    		holeFillBackFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
	    		holeFillBackFBO.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
	    		holeFillBackFBO.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
	    		
	    		holeFillProgram.setTexture("input0", holeFillFrontFBO.getColorAttachmentTexture(0));
	    		holeFillProgram.setTexture("input1", holeFillFrontFBO.getColorAttachmentTexture(1));
	    		holeFillProgram.setTexture("input2", holeFillFrontFBO.getColorAttachmentTexture(2));
	    		holeFillProgram.setTexture("input3", holeFillFrontFBO.getColorAttachmentTexture(3));
	    		
	    		holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, holeFillBackFBO);
	    		context.finish();
	    		
	    		FramebufferObject<ContextType> tmp = holeFillFrontFBO;
	    		holeFillFrontFBO = holeFillBackFBO;
	    		holeFillBackFBO = tmp;
	    	}
	    	specularFitFramebuffer = holeFillFrontFBO;
	
			System.out.println("Empty regions filled in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	        
	    	System.out.println("Saving textures...");
	    	timestamp = new Date();
	    	
	    	File textureDirectory = new File(outputDir, "textures");
	    	
	    	textureDirectory.mkdirs();
	        
	    	diffuseFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(textureDirectory, "diffuse.png"));
	    	diffuseFitFramebuffer.saveColorBufferToFile(1, "PNG", new File(textureDirectory, "normal.png"));
	    	//diffuseFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(textureDirectory, "ambient.png"));
	    	if (DEBUG)
	    	{
	    		diffuseFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(textureDirectory, "ddebug.png"));
	    	}
	
	    	specularFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(textureDirectory, "specular-old.png"));
	    	specularFitFramebuffer.saveColorBufferToFile(1, "PNG", new File(textureDirectory, "roughness-old.png"));
	    	if (param.isSpecularNormalComputationEnabled())
	    	{
	    		specularFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(textureDirectory, "snormal-old.png"));
	    	}
	    	if (DEBUG)
	    	{
		    	specularFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(textureDirectory, "sdebug-old.png"));
	    	}
	
	    	System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    	
	    	System.out.println("Resampling...");
	    	timestamp = new Date();
	    	
			// Resample the reflectance data
			double[] roughnessValues = resample(viewSet, diffuseFitFramebuffer.getColorAttachmentTexture(0), diffuseFitFramebuffer.getColorAttachmentTexture(1));
			
	    	System.out.println("Creating specular reflectivity texture...");
			
			FloatVertexList roughnessList = new FloatVertexList(1, roughnessValues.length);
			
			for (int i = 0; i < roughnessValues.length; i++)
			{
				roughnessList.set(i, 0, (float)roughnessValues[i]);
			}
			
			if (roughnessValues != null)
			{
				Texture2D<ContextType> roughnessTexture = 
						context.get2DColorTextureBuilder(ROUGHNESS_TEXTURE_SIZE, ROUGHNESS_TEXTURE_SIZE, roughnessList)
							.setInternalFormat(ColorFormat.R32F)
							.setLinearFilteringEnabled(true)
							.setMipmapsEnabled(false)
							.createTexture();
				
				SpecularFit2<ContextType> specularFit2 = createSpecularFit2(specularFitFramebuffer, param.getTextureSubdivision());
				
				specularFitFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
				
				if (param.getTextureSubdivision() == 1)
	        	{
	        		Texture3D<ContextType> preprojectedViews = null;
			    	
			    	if (param.isImagePreprojectionUseEnabled())
			    	{
		    			preprojectedViews = context.get2DColorTextureArrayBuilder(subdivSize, subdivSize, viewSet.getCameraPoseCount()).createTexture();
				    	
						for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
						{
							preprojectedViews.loadLayer(i, new File(new File(tmpDir, String.format("%04d", i)), String.format("r%04dc%04d.png", 0, 0)), true);
						}
			        
			        	specularFit2.fit(0, 0, preprojectedViews, null, null, 
			        			diffuseFitFramebuffer.getColorAttachmentTexture(0), diffuseFitFramebuffer.getColorAttachmentTexture(1), roughnessTexture);
			        	
			    		preprojectedViews.delete();
			    	}
		    		else
		    		{
		    			specularFit2.fit(0, 0, viewTextures, depthTextures, shadowTextures, 
		    					diffuseFitFramebuffer.getColorAttachmentTexture(0), diffuseFitFramebuffer.getColorAttachmentTexture(1), roughnessTexture);
		    		}
	        	}
	        	else
	        	{
	        		for (int row = 0; row < param.getTextureSubdivision(); row++)
	    	    	{
	    		    	for (int col = 0; col < param.getTextureSubdivision(); col++)
	    	    		{
	    	        		if (param.isImagePreprojectionUseEnabled())
	    			    	{
	    	        			Texture3D<ContextType> preprojectedViews = context.get2DColorTextureArrayBuilder(subdivSize, subdivSize, viewSet.getCameraPoseCount()).createTexture();
	    				    	
	    						for (int i = 0; i < viewSet.getCameraPoseCount(); i++)
	    						{
	    							preprojectedViews.loadLayer(i, new File(new File(tmpDir, String.format("%04d", i)), String.format("r%04dc%04d.png", row, col)), true);
	    						}
	    	    		        
	    	    		        specularFit2.fit(row, col, preprojectedViews, null, null, 
	    	    		        		diffuseFitFramebuffer.getColorAttachmentTexture(0), diffuseFitFramebuffer.getColorAttachmentTexture(1), roughnessTexture);

	    	    	    		specularFitFramebuffer.saveColorBufferToFile(0, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
	    	    		        		"PNG", new File(specularTempDirectory, String.format("alt_r%04dc%04d.png", row, col)));
	    	    		        
	    	    	    		specularFitFramebuffer.saveColorBufferToFile(1, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
	    	    		        		"PNG", new File(roughnessTempDirectory, String.format("alt_r%04dc%04d.png", row, col)));
	    	    		        
	    	    	    		specularFitFramebuffer.saveColorBufferToFile(2, col * subdivSize, row * subdivSize, subdivSize, subdivSize, 
	    	    		        		"PNG", new File(snormalTempDirectory, String.format("alt_r%04dc%04d.png", row, col)));

	    			    		preprojectedViews.delete();
	    			    	}
	    	        		else
	    	        		{
		    	        		specularFit2.fit(row, col, viewTextures, depthTextures, shadowTextures, 
	    	        				diffuseFitFramebuffer.getColorAttachmentTexture(0), diffuseFitFramebuffer.getColorAttachmentTexture(1), roughnessTexture);
	    	        		}
	    	        		
	    	        		System.out.println("Block " + (row*param.getTextureSubdivision() + col + 1) + "/" + (param.getTextureSubdivision() * param.getTextureSubdivision()) + " completed.");
	    	    		}
	    	    	}
	        	}
				
				// Fill holes
				holeFillFrontFBO = specularFitFramebuffer;
		    	for (int i = 0; i < param.getTextureSize() / 2; i++)
		    	{
		    		holeFillBackFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    		holeFillBackFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    		holeFillBackFBO.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    		holeFillBackFBO.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    		
		    		holeFillProgram.setTexture("input0", holeFillFrontFBO.getColorAttachmentTexture(0));
		    		holeFillProgram.setTexture("input1", holeFillFrontFBO.getColorAttachmentTexture(1));
		    		holeFillProgram.setTexture("input2", holeFillFrontFBO.getColorAttachmentTexture(2));
		    		holeFillProgram.setTexture("input3", holeFillFrontFBO.getColorAttachmentTexture(3));
		    		
		    		holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, holeFillBackFBO);
		    		context.finish();
		    		
		    		FramebufferObject<ContextType> tmp = holeFillFrontFBO;
		    		holeFillFrontFBO = holeFillBackFBO;
		    		holeFillBackFBO = tmp;
		    	}
		    	specularFitFramebuffer = holeFillFrontFBO;
				
    	    	specularFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(textureDirectory, "specular.png"));
			}
			
			if (viewTextures != null)
	        {
	        	viewTextures.delete();
	        }
	        
	        if (depthTextures != null)
	        {
	        	depthTextures.delete();
	        }
	        
	        if (shadowTextures != null)
	        {
	        	shadowTextures.delete();
	        }
	        
	        lightPositionBuffer.delete();
	        lightIntensityBuffer.delete();
	        
	        if (shadowMatrixBuffer != null)
	        {
	        	shadowMatrixBuffer.delete();
	        }
	    	
	    	diffuseFitFramebuffer.delete();
	    	specularFitFramebuffer.delete();
	
	    	System.out.println("Resampling completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
    	}
    	finally
    	{
			if (projTexProgram != null) projTexProgram.delete();
			if (diffuseFitProgram != null) diffuseFitProgram.delete();
			if (specularFitProgram != null) specularFitProgram.delete();
			if (diffuseDebugProgram != null) diffuseDebugProgram.delete();
			if (specularDebugProgram != null) specularDebugProgram.delete();
			if (depthRenderingProgram != null) depthRenderingProgram.delete();
			
			if (viewSet != null) viewSet.deleteOpenGLResources();
			if (positionBuffer != null) positionBuffer.delete();
			if (normalBuffer != null) normalBuffer.delete();
			if (texCoordBuffer != null) texCoordBuffer.delete();
			if (tangentBuffer != null) tangentBuffer.delete();
    	}
	}
}
