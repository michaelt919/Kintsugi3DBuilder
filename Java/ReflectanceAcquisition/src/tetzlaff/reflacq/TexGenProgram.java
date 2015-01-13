package tetzlaff.reflacq;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.util.Precision;

import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLResource;
import tetzlaff.ulf.UnstructuredLightField;
import tetzlaff.window.glfw.GLFWWindow;

public class TexGenProgram
{
	private static final double MIN_SIGMA = 1.0 / 256.0;
	
	private static double getSpecularIntensity(double incomingIntensity, double incomingSigma, double outgoingIntensity, double outgoingSigma, double specularRoughness)
	{
		if (incomingIntensity <= 0.0 || incomingSigma <= 0.0 || outgoingIntensity <= 0.0 || outgoingSigma <= 0.0 || specularRoughness <= 0.0)
		{
			return 0.0;
		}
		else
		{
			// TODO dependence on sigma
			return outgoingIntensity / incomingIntensity;
		}
	}
	
	private static double getSpecularRoughness(double incomingSigma, double outgoingSigma)
	{
		if (outgoingSigma <= incomingSigma)
		{
			return MIN_SIGMA;
		}
		else
		{
			return Math.sqrt(outgoingSigma*outgoingSigma - incomingSigma*incomingSigma);
		}
	}
	
    public static void main(String[] args) 
    {
    	JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
		fileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
		
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
		{
			OpenGLContext ulfToTexContext = new GLFWWindow(800, 800, "Texture Generation");
	    	ulfToTexContext.enableDepthTest();
	    	ulfToTexContext.enableBackFaceCulling();
	    	
	    	String lightFieldDirectory = fileChooser.getSelectedFile().getParent();
	    	int textureSize = 256;
	    	
	    	short blackThreshold = 32;
	    	double gamma = 2.2;
	    	
	    	double lightIntensityRed = 1.0;
	    	double lightIntensityGreen = 1.0;
	    	double lightIntensityBlue = 1.0;
	    	double lightSigmaRed = 0.2;
	    	double lightSigmaGreen = 0.2;
	    	double lightSigmaBlue = 0.2;
	        
	        try
	        {
	    		OpenGLProgram genTexProgram = new OpenGLProgram(new File("shaders\\ulfTex.vert"), new File("shaders\\ulfTex.frag"));
	    		UnstructuredLightField lightField = UnstructuredLightField.loadFromDirectory(lightFieldDirectory);
		    	
		    	OpenGLRenderable renderable = new OpenGLRenderable(genTexProgram);
		    	Iterable<OpenGLResource> vboResources = renderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
		    	
		    	renderable.program().setTexture("imageTextures", lightField.viewSet.getTextures());
		    	renderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	renderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
		    	renderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
				
		    	renderable.program().setTexture("depthTextures", lightField.depthTextures);
		    	
		    	renderable.program().setUniform("occlusionEnabled", lightField.settings.isOcclusionEnabled());
		    	renderable.program().setUniform("occlusionBias", lightField.settings.getOcclusionBias());
		    	
		    	OpenGLFramebufferObject framebuffer = new OpenGLFramebufferObject(textureSize, textureSize, 1, false);
		    	
		    	System.out.println("Projecting light field images into texture space...");
		    	Date timestamp = new Date();
		    	
		    	new File(lightFieldDirectory + "\\output\\textures").mkdirs();
		    	
		    	int[][] viewTextureData = new int[lightField.viewSet.getCameraPoseCount()][];
		    	
		    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
		    	{
			    	framebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
			    	framebuffer.clearDepthBuffer();
			    	
			    	renderable.program().setUniform("cameraPoseIndex", i);
			        renderable.draw(PrimitiveMode.TRIANGLES, framebuffer);
			        
			        viewTextureData[i] = framebuffer.readColorBufferARGB(0);
		    	}
		        
		        for (OpenGLResource r : vboResources)
		    	{
		    		r.delete();
		    	}

		    	framebuffer.delete();
		        lightField.deleteOpenGLResources();
		        genTexProgram.delete();
		    	
				System.out.println("Projections completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
				
				System.out.println("Sorting texels by intensity...");
		    	timestamp = new Date();
		    	
				int sampleCount = viewTextureData.length;
				int texelCount = textureSize*textureSize;
				// Use shorts since Java doesn't support unsigned bytes
		    	Short[][] redTexels = new Short[texelCount][sampleCount];
		    	Short[][] greenTexels = new Short[texelCount][sampleCount];
		    	Short[][] blueTexels = new Short[texelCount][sampleCount];
		    	int[] alphaSums = new int[texelCount];
		    	
		    	for (int i = 0; i < sampleCount; i++)
		    	{
		    		// Flip the data vertically
			        for (int y = 0; y < textureSize / 2; y++)
			        {
			        	int limit = (y + 1) * textureSize;
			        	for (int j1 = y * textureSize, j2 = (textureSize - y - 1) * textureSize; j1 < limit; j1++, j2++)
			        	{
			        		alphaSums[j1] += ((viewTextureData[i][j2] & 0xFF000000) >>> 24);
			    			redTexels[j1][i] = (short)((viewTextureData[i][j2] & 0x00FF0000) >>> 16);
			    			greenTexels[j1][i] = (short)((viewTextureData[i][j2] & 0x0000FF00) >>> 8);
			    			blueTexels[j1][i] = (short)(viewTextureData[i][j2] & 0x000000FF);
			    			
			    			alphaSums[j2] += ((viewTextureData[i][j1] & 0xFF000000) >>> 24);
			    			redTexels[j2][i] = (short)((viewTextureData[i][j1] & 0x00FF0000) >>> 16);
			    			greenTexels[j2][i] = (short)((viewTextureData[i][j1] & 0x0000FF00) >>> 8);
			    			blueTexels[j2][i] = (short)(viewTextureData[i][j1] & 0x000000FF);
			        	}
			        }
		    	}

		        BufferedImage outImg = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);
		        for (int y = 0, i = 0; y < textureSize; y++)
		        {
		        	for (int x = 0; x < textureSize; x++, i++)
		        	{
		        		outImg.setRGB(x, y, 0xFF800000 | Math.min(alphaSums[i] / sampleCount, 0x000000FF) << 8);
		        	}
		        }
		        File outputFile = new File(lightFieldDirectory + String.format("\\output\\sums.png"));
		        ImageIO.write(outImg, "PNG", outputFile);
		    	
		    	for (int i = 0; i < texelCount; i++)
		    	{
			    	Collections.sort(Arrays.asList(redTexels[i]));
			    	Collections.sort(Arrays.asList(greenTexels[i]));
			    	Collections.sort(Arrays.asList(blueTexels[i]));
		    	}

				System.out.println("Sorting completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
				
				System.out.println("Fitting model...");
		    	timestamp = new Date();
				
				int[] diffuseImgData = new int[texelCount];
				int[] specularIntensityData = new int[texelCount];
				int[] specularRoughnessData = new int[texelCount];
				int[] specularOffsetData = new int[texelCount];
				
				LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer(100, 1e-5, 1e-5, 1e-5, Precision.SAFE_MIN);
				
				for (int texelIndex = 0; texelIndex < texelCount; texelIndex++)
				{
					if (texelCount > 100 && texelIndex % (texelCount / 100) == 0)
					{
						System.out.println(100 * texelIndex / texelCount + "% complete.");
					}
					
					int diffuseIndex = sampleCount / 2;
					while (diffuseIndex < sampleCount - 1 && 
						redTexels[texelIndex][diffuseIndex] <= blackThreshold &&
						greenTexels[texelIndex][diffuseIndex] <= blackThreshold && 
						blueTexels[texelIndex][diffuseIndex] <= blackThreshold)
					{
						diffuseIndex++;
					}
						
	        		short diffuseRed = redTexels[texelIndex][diffuseIndex];
	        		short diffuseGreen = greenTexels[texelIndex][diffuseIndex];
	        		short diffuseBlue = blueTexels[texelIndex][diffuseIndex];
					diffuseImgData[texelIndex] = 0xFF000000 | diffuseRed << 16 | diffuseGreen << 8 | diffuseBlue;
					
					if (sampleCount - diffuseIndex > 3)
					{
						WeightedObservedPoints redPoints = new WeightedObservedPoints();
				        WeightedObservedPoints greenPoints = new WeightedObservedPoints();
				        WeightedObservedPoints bluePoints = new WeightedObservedPoints();
	
				        for (int sampleIndex = diffuseIndex + 1; sampleIndex < sampleCount; sampleIndex++) 
				        {
				        	// Assume diffuse index is at an angle of pi/2 (between dominant light direction and viewing direction)
				        	// theta is measured in radians / (pi/2) - i.e. theta=1.0 is a 90 degree angle, theta = 0.0 is a 0 degree angle
				        	double theta = (double)(sampleCount - sampleIndex - 0.5) / (double)(sampleCount - diffuseIndex - 0.5);
				        	
				        	short red = redTexels[texelIndex][sampleIndex];
			        		short green = greenTexels[texelIndex][sampleIndex];
				        	short blue = blueTexels[texelIndex][sampleIndex];
				        	
				        	if (red < 255 && green < 255 && blue < 255)
				        	{
				        		// Add sample points at theta and negative theta to ensure that the Gaussian is centered
				        		double redRelative = Math.pow(red, gamma) - Math.pow(diffuseRed, gamma);
				        		redPoints.add(theta, redRelative);
				        		redPoints.add(-theta, redRelative);

				        		double greenRelative = Math.pow(green, gamma) - Math.pow(diffuseGreen, gamma);
				        		greenPoints.add(theta, greenRelative);
				        		greenPoints.add(-theta, greenRelative);
				        		
				        		double blueRelative = Math.pow(blue, gamma) - Math.pow(diffuseBlue, gamma);
				        		bluePoints.add(theta, blueRelative);
				        		bluePoints.add(-theta, blueRelative);
				        	}
				        }
	
				        try
				        {
				        	CustomGaussianCurveFitter redFitter = CustomGaussianCurveFitter.create().withOptimizer(optimizer);
					        double[] redFit = redFitter.fit(redPoints.toList());
					        double redNorm = redFit[0];
					        double redCenter = redFit[1]; // Should be zero
					        double redSigma = redFit[2];
					        double redRoughness = getSpecularRoughness(lightSigmaRed, redSigma);
					        double redIntensity = Math.pow(getSpecularIntensity(lightIntensityRed, lightSigmaRed, redNorm, redSigma, redRoughness), 1 / gamma);
					        short redIntensityInteger = (short)Math.min(Math.round(redIntensity), 255);
					        short redRoughnessInteger = (short)Math.min(Math.max(Math.round(redRoughness*256 - 1), 0), 255);
					        short redOffsetInteger = (short)Math.min(Math.round(redCenter*255), 255);

					        CustomGaussianCurveFitter greenFitter = CustomGaussianCurveFitter.create().withOptimizer(optimizer);
							double[] greenFit = greenFitter.fit(greenPoints.toList());
					        double greenNorm = greenFit[0];
					        double greenCenter = greenFit[1]; // Should be zero
					        double greenSigma = greenFit[2];
					        double greenRoughness = getSpecularRoughness(lightSigmaGreen, greenSigma);
					        double greenIntensity = Math.pow(getSpecularIntensity(lightIntensityGreen, lightSigmaGreen, greenNorm, greenSigma, greenRoughness), 1 / gamma);
					        short greenIntensityInteger = (short)Math.min(Math.max(Math.round(greenIntensity), 0), 255);
					        short greenRoughnessInteger = (short)Math.min(Math.max(Math.round(greenRoughness*256 - 1), 0), 255);
					        short greenOffsetInteger = (short)Math.min(Math.round(greenCenter*255), 255);
					        
					        CustomGaussianCurveFitter blueFitter = CustomGaussianCurveFitter.create().withOptimizer(optimizer);
							double[] blueFit = blueFitter.fit(bluePoints.toList());
					        double blueNorm = blueFit[0];
					        double blueCenter = blueFit[1]; // Should be zero
					        double blueSigma = blueFit[2];
					        double blueRoughness = getSpecularRoughness(lightSigmaBlue, blueSigma);
					        double blueIntensity = Math.pow(getSpecularIntensity(lightIntensityBlue, lightSigmaBlue, blueNorm, blueSigma, blueRoughness), 1 / gamma);
					        short blueIntensityInteger = (short)Math.min(Math.max(Math.round(blueIntensity), 0), 255);
					        short blueRoughnessInteger = (short)Math.min(Math.max(Math.round(blueRoughness*256 - 1), 0), 255);
					        short blueOffsetInteger = (short)Math.min(Math.round(blueCenter*255), 255);
				        
					        specularIntensityData[texelIndex] = 0xFF000000 | redIntensityInteger << 16 | greenIntensityInteger << 8 | blueIntensityInteger;
					        specularRoughnessData[texelIndex] = 0xFF000000 | redRoughnessInteger << 16 | greenRoughnessInteger << 8 | blueRoughnessInteger;
					        specularOffsetData[texelIndex] = 0xFF000000 | redOffsetInteger << 16 | greenOffsetInteger << 8 | blueOffsetInteger;
				        }
				        catch(Exception e)
				        {
				        	e.printStackTrace();
				        }
					}
					else
					{
						specularIntensityData[texelIndex] = 0xFF000000;
				        specularRoughnessData[texelIndex] = 0xFF000000;
				        specularOffsetData[texelIndex] = 0xFF000000;
					}
				}
				
				System.out.println("Model fitting completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");

		    	System.out.println("Writing to output directory: " + lightFieldDirectory + "\\output");
				timestamp = new Date();
		    	
		    	outImg = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);
		        outImg.setRGB(0, 0, textureSize, textureSize, diffuseImgData, 0, textureSize);
		        outputFile = new File(lightFieldDirectory + String.format("\\output\\textures\\diffuse.png"));
		        ImageIO.write(outImg, "PNG", outputFile);
		    	
		    	outImg = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);
		        outImg.setRGB(0, 0, textureSize, textureSize, specularIntensityData, 0, textureSize);
		        outputFile = new File(lightFieldDirectory + String.format("\\output\\textures\\specularIntensity.png"));
		        ImageIO.write(outImg, "PNG", outputFile);
		    	
		    	outImg = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);
		        outImg.setRGB(0, 0, textureSize, textureSize, specularRoughnessData, 0, textureSize);
		        outputFile = new File(lightFieldDirectory + String.format("\\output\\textures\\specularRoughness.png"));
		        ImageIO.write(outImg, "PNG", outputFile);
		    	
		    	outImg = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);
		        outImg.setRGB(0, 0, textureSize, textureSize, specularOffsetData, 0, textureSize);
		        outputFile = new File(lightFieldDirectory + String.format("\\output\\textures\\specularOffset.png"));
		        ImageIO.write(outImg, "PNG", outputFile);
				
				for (int i = 0; i < sampleCount; i++)
		    	{
			        outImg = new BufferedImage(textureSize, textureSize, BufferedImage.TYPE_INT_ARGB);
			        for (int y = 0, j = 0; y < textureSize; y++)
			        {
			        	for (int x = 0; x < textureSize; x++, j++)
			        	{
			        		outImg.setRGB(x, y, 0xFF000000 | (redTexels[j][i] << 16) | (greenTexels[j][i] << 8) | blueTexels[j][i]);
			        	}
			        }
			        
			        outputFile = new File(lightFieldDirectory + String.format("\\output\\textures\\%1$04d.png", i));
			        ImageIO.write(outImg, "PNG", outputFile);
			        
		    	}
				System.out.println("Output files saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
	        
	        GLFWWindow.closeAllWindows();
		}
		
        System.exit(0);
    }
}
