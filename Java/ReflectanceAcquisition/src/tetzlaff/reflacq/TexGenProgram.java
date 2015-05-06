package tetzlaff.reflacq;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.helpers.FloatVertexList;
import tetzlaff.gl.helpers.Matrix3;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.gl.opengl.OpenGLContext;
import tetzlaff.gl.opengl.OpenGLFramebufferObject;
import tetzlaff.gl.opengl.OpenGLProgram;
import tetzlaff.gl.opengl.OpenGLRenderable;
import tetzlaff.gl.opengl.OpenGLResource;
import tetzlaff.gl.opengl.OpenGLTextureArray;
import tetzlaff.gl.opengl.OpenGLVertexBuffer;
import tetzlaff.ulf.UnstructuredLightField;
import tetzlaff.window.glfw.GLFWWindow;

public class TexGenProgram
{
	public static void main(String[] args)
    {
    	Date timestamp;
		
    	OpenGLContext ulfToTexContext = new GLFWWindow(800, 800, "Texture Generation");
    	ulfToTexContext.enableDepthTest();
    	ulfToTexContext.enableBackFaceCulling();
    	
    	int textureSize = 2048;
    	float gamma = 2.2f;
    	
    	Vector3 guessSpecularColor = new Vector3(1.0f, 1.0f, 1.0f);
    	boolean computeRoughness = true;
    	boolean useViewSetNormal = false;
    	boolean computeSpecularNormal = false;
    	float specularInfluenceScale = 0.35f;
    	float guessSpecularOrthoExp = 1.0f;
    	float guessSpecularWeight = 0.0f;
    	float delta = 0.05f;
    	float minDiffuseSamplePct = 0.1f;
    	float diffuseDeterminantThreshold = 1.0f;
    	float specularBias = 0.0f;
    	float specularRoughnessCap = 0.5f;
    	float weightSumThreshold = 0.01f;
    	float specularDeterminantThreshold = (computeSpecularNormal ? 0.000001f : 0.005f);
    	float determinantExponent = 1.0f;
    	float minFillAlphaSpecular = 0.1f;
    	float maxDiffuseRemovalFactor = 0.98f;
    	float maxSpecularRemovalFactor = 0.98f;
    	int fittingIterations = 1;
    	int diffuseFillIterations = 0;
    	int specularFillIterations = textureSize;
    	
    	int debugPixelX = 322, debugPixelY = textureSize - 365;
    	
        try
        {
	    	OpenGLProgram diffuseDebugProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "projtex.frag"));
    		OpenGLProgram diffuseFitProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "diffusefit.frag"));
    		OpenGLProgram specularFitProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "specularfit.frag"));
    		OpenGLProgram specularDebugProgram = new OpenGLProgram(new File("shaders", "texspace.vert"), new File("shaders", "speculardebug.frag"));
    		OpenGLProgram holeFillProgram = new OpenGLProgram(new File("shaders", "passthrough2d.vert"), new File("shaders", "holefill.frag"));
    		
    		JFileChooser fileChooser = new JFileChooser(new File("").getAbsolutePath());
    		fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
    		fileChooser.setFileFilter(new FileNameExtensionFilter("View Set files (.vset)", "vset"));
    		
    		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
    		{
    	    	File vsetFile = fileChooser.getSelectedFile();
    	    	File lightFieldDirectory = vsetFile.getParentFile();
    	    	UnstructuredLightField lightField = UnstructuredLightField.loadFromVSETFile(vsetFile);
		    	
		    	System.out.println("Fitting to model...");
		    	timestamp = new Date();
		    	
		    	OpenGLRenderable diffuseFitRenderable = new OpenGLRenderable(diffuseFitProgram);
		    	
		    	diffuseFitRenderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
		    	
		    	diffuseFitRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
		    	diffuseFitRenderable.program().setTexture("depthImages", lightField.depthTextures);
		    	diffuseFitRenderable.program().setUniform("occlusionEnabled", lightField.settings.isOcclusionEnabled());
		    	diffuseFitRenderable.program().setUniform("occlusionBias", lightField.settings.getOcclusionBias());
		    	diffuseFitRenderable.program().setUniform("viewCount", lightField.viewSet.getCameraPoseCount());
		    	
		    	diffuseFitRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	diffuseFitRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
		    	diffuseFitRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
		    	
		    	diffuseFitRenderable.program().setUniform("gamma", gamma);
		    	diffuseFitRenderable.program().setUniform("delta", delta);
		    	diffuseFitRenderable.program().setUniform("minDiffuseSamples", (int)Math.ceil(minDiffuseSamplePct * lightField.viewSet.getCameraPoseCount()));
		    	diffuseFitRenderable.program().setUniform("guessSpecularColor", guessSpecularColor);
		    	diffuseFitRenderable.program().setUniform("guessSpecularWeight", guessSpecularWeight);
		    	diffuseFitRenderable.program().setUniform("guessSpecularOrthoExp", guessSpecularOrthoExp);
		    	diffuseFitRenderable.program().setUniform("determinantThreshold", diffuseDeterminantThreshold);
		    	
		    	OpenGLRenderable specularFitRenderable = new OpenGLRenderable(specularFitProgram);
		    	
		    	specularFitRenderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
		    	
		    	specularFitRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
		    	specularFitRenderable.program().setTexture("depthImages", lightField.depthTextures);
		    	specularFitRenderable.program().setUniform("occlusionEnabled", lightField.settings.isOcclusionEnabled());
		    	specularFitRenderable.program().setUniform("occlusionBias", lightField.settings.getOcclusionBias());
		    	specularFitRenderable.program().setUniform("viewCount", lightField.viewSet.getCameraPoseCount());
		    	
		    	specularFitRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	specularFitRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
		    	specularFitRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
		    	
		    	specularFitRenderable.program().setUniform("gamma", gamma);
		    	specularFitRenderable.program().setUniform("computeRoughness", computeRoughness);
		    	specularFitRenderable.program().setUniform("computeNormal", computeSpecularNormal);
		    	specularFitRenderable.program().setUniform("useViewSetNormal", useViewSetNormal);
		    	specularFitRenderable.program().setUniform("specularBias", specularBias);
		    	specularFitRenderable.program().setUniform("specularInfluenceScale", specularInfluenceScale);
		    	specularFitRenderable.program().setUniform("specularRoughnessCap", specularRoughnessCap);
		    	specularFitRenderable.program().setUniform("defaultSpecularColor", new Vector3(0.0f, 0.0f, 0.0f));
		    	specularFitRenderable.program().setUniform("defaultSpecularRoughness", specularRoughnessCap);
		    	specularFitRenderable.program().setUniform("weightSumThreshold", weightSumThreshold);
		    	specularFitRenderable.program().setUniform("determinantThreshold", specularDeterminantThreshold);
		    	specularFitRenderable.program().setUniform("determinantExponent", determinantExponent);
		    	
		    	if (lightField.viewSet.getLightPositionBuffer() != null && lightField.viewSet.getLightIndexBuffer() != null)
	    		{
		    		diffuseFitRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
		    		diffuseFitRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
		    		specularFitRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
		    		specularFitRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
	    		}

		    	OpenGLFramebufferObject diffuseFitBackFramebuffer = new OpenGLFramebufferObject(textureSize, textureSize, 8, true, false);
		    	OpenGLFramebufferObject diffuseFitFrontFramebuffer = new OpenGLFramebufferObject(textureSize, textureSize, 8, true, false);
		    	OpenGLFramebufferObject specularFitBackFramebuffer = new OpenGLFramebufferObject(textureSize, textureSize, 8, true, false);
		    	OpenGLFramebufferObject specularFitFrontFramebuffer = new OpenGLFramebufferObject(textureSize, textureSize, 8, true, false);

		    	diffuseFitFrontFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(5, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(6, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitFrontFramebuffer.clearColorBuffer(7, 0.0f, 0.0f, 0.0f, 0.0f);
		    	
		    	diffuseFitBackFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitBackFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitBackFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitBackFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitBackFramebuffer.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitBackFramebuffer.clearColorBuffer(5, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitBackFramebuffer.clearColorBuffer(6, 0.0f, 0.0f, 0.0f, 0.0f);
		    	diffuseFitBackFramebuffer.clearColorBuffer(7, 0.0f, 0.0f, 0.0f, 0.0f);
		    	
		    	specularFitBackFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitBackFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitBackFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitBackFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitBackFramebuffer.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitBackFramebuffer.clearColorBuffer(5, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitBackFramebuffer.clearColorBuffer(6, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitBackFramebuffer.clearColorBuffer(7, 0.0f, 0.0f, 0.0f, 0.0f);
		    	
		    	specularFitFrontFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitFrontFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitFrontFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitFrontFramebuffer.clearColorBuffer(3, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitFrontFramebuffer.clearColorBuffer(4, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitFrontFramebuffer.clearColorBuffer(5, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitFrontFramebuffer.clearColorBuffer(6, 0.0f, 0.0f, 0.0f, 0.0f);
		    	specularFitFrontFramebuffer.clearColorBuffer(7, 0.0f, 0.0f, 0.0f, 0.0f);
		    	
		        for (int i = 0; i < fittingIterations; i++)
		        {
		        	System.out.println("Starting iteration " + i);
			    	diffuseFitRenderable.program().setUniform("specularRemovalFactor", maxSpecularRemovalFactor * (float)i / (float)fittingIterations);
			    	diffuseFitRenderable.program().setUniform("specularRoughnessCap", specularRoughnessCap);
			    	diffuseFitRenderable.program().setTexture("diffuseEstimate", diffuseFitFrontFramebuffer.getColorAttachmentTexture(0));
			    	diffuseFitRenderable.program().setTexture("normalEstimate", diffuseFitFrontFramebuffer.getColorAttachmentTexture(1));
		        	diffuseFitRenderable.program().setTexture("specularColorEstimate", specularFitBackFramebuffer.getColorAttachmentTexture(0));
		        	diffuseFitRenderable.program().setTexture("roughnessEstimate", specularFitBackFramebuffer.getColorAttachmentTexture(1));
			    	diffuseFitRenderable.program().setTexture("specularNormalEstimate", specularFitBackFramebuffer.getColorAttachmentTexture(2));
		        	diffuseFitRenderable.program().setTexture("previousError", specularFitBackFramebuffer.getColorAttachmentTexture(3));
			    	
			        diffuseFitRenderable.draw(PrimitiveMode.TRIANGLES, diffuseFitBackFramebuffer);

			    	specularFitRenderable.program().setUniform("diffuseRemovalFactor", maxDiffuseRemovalFactor * (float)(i + 1) / (float)fittingIterations);
			    	specularFitRenderable.program().setTexture("diffuseEstimate", diffuseFitBackFramebuffer.getColorAttachmentTexture(0));
			    	specularFitRenderable.program().setTexture("normalEstimate", diffuseFitBackFramebuffer.getColorAttachmentTexture(1));
			    	specularFitRenderable.program().setTexture("previousError", diffuseFitBackFramebuffer.getColorAttachmentTexture(2));
			    	
			    	if (i == 0)
		    		{
			    		// Use a texture that should be empty for the first iteration
				    	specularFitRenderable.program().setTexture("previousError", diffuseFitFrontFramebuffer.getColorAttachmentTexture(2));
		    		}
			    	else
			    	{
				    	specularFitRenderable.program().setTexture("previousError", diffuseFitBackFramebuffer.getColorAttachmentTexture(2));
			    	}

			        specularFitRenderable.draw(PrimitiveMode.TRIANGLES, specularFitBackFramebuffer);
			        
			        // Swap buffers
			        OpenGLFramebufferObject tmp = diffuseFitBackFramebuffer;
			        diffuseFitBackFramebuffer = diffuseFitFrontFramebuffer;
			        diffuseFitFrontFramebuffer = tmp;
			        
			        tmp = specularFitBackFramebuffer;
			        specularFitBackFramebuffer = specularFitFrontFramebuffer;
			        specularFitFrontFramebuffer = tmp;
			        
//			    	if (i == 0 || (i >= fittingIterations / 2 && (fittingIterations <= 16 || i % (fittingIterations / 16) == 0)))
//			    	{
//				    	new File(lightFieldDirectory, String.format("output/debug/stages/%04d", i)).mkdirs();
//				    	
//				        diffuseFitFrontFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuse.png", i)));
//				        diffuseFitFrontFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/normal.png", i)));
//				        diffuseFitFrontFramebuffer.saveColorBufferToFile(2, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseError.png", i)));
//				        //diffuseFitFrontFramebuffer.saveColorBufferToFile(3, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseDebug1.png", i)));
//				        //diffuseFitFrontFramebuffer.saveColorBufferToFile(4, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseDebug2.png", i)));
//				        //diffuseFitFrontFramebuffer.saveColorBufferToFile(5, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseDebug3.png", i)));
//				        //diffuseFitFrontFramebuffer.saveColorBufferToFile(6, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseDebug4.png", i)));
//				        //diffuseFitFrontFramebuffer.saveColorBufferToFile(7, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/diffuseDebug5.png", i)));
//	
//				        specularFitFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specular.png", i)));
//				        specularFitFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/roughness.png", i)));
//				        specularFitFramebuffer.saveColorBufferToFile(2, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularError.png", i)));
//	//			        specularFitFramebuffer.saveColorBufferToFile(3, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularDebug1.png", i)));
//	//			        specularFitFramebuffer.saveColorBufferToFile(4, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularDebug2.png", i)));
//	//			        specularFitFramebuffer.saveColorBufferToFile(5, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularDebug3.png", i)));
//	//			        specularFitFramebuffer.saveColorBufferToFile(6, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularDebug4.png", i)));
//	//			        specularFitFramebuffer.saveColorBufferToFile(7, "PNG", new File(lightFieldDirectory, String.format("output/debug/stages/%04d/specularDebug5.png", i)));
//			    	}
			        
			    	ulfToTexContext.flush();
		        }

		    	System.out.println("Model fitting completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	System.out.println("Saving raw textures with holes for debugging...");
		    	timestamp = new Date();
		        
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, "output/debug/diffuse.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, "output/debug/normal.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(2, "PNG", new File(lightFieldDirectory, "output/debug/diffuseError.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(3, "PNG", new File(lightFieldDirectory, "output/debug/diffuse1.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(4, "PNG", new File(lightFieldDirectory, "output/debug/diffuse2.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(5, "PNG", new File(lightFieldDirectory, "output/debug/diffuse3.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(6, "PNG", new File(lightFieldDirectory, "output/debug/diffuse4.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(7, "PNG", new File(lightFieldDirectory, "output/debug/diffuse5.png"));

		        specularFitFrontFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, "output/debug/specular.png"));
		        specularFitFrontFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, "output/debug/roughness.png"));
		        specularFitFrontFramebuffer.saveColorBufferToFile(2, "PNG", new File(lightFieldDirectory, "output/debug/snormal.png"));
		        specularFitFrontFramebuffer.saveColorBufferToFile(3, "PNG", new File(lightFieldDirectory, "output/debug/specularError.png"));
		        specularFitFrontFramebuffer.saveColorBufferToFile(4, "PNG", new File(lightFieldDirectory, "output/debug/specular1.png"));
		        specularFitFrontFramebuffer.saveColorBufferToFile(5, "PNG", new File(lightFieldDirectory, "output/debug/specular2.png"));
		        specularFitFrontFramebuffer.saveColorBufferToFile(6, "PNG", new File(lightFieldDirectory, "output/debug/specular3.png"));
		        specularFitFrontFramebuffer.saveColorBufferToFile(7, "PNG", new File(lightFieldDirectory, "output/debug/specular4.png"));
	    		
		    	System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
	    		
	    		System.out.println("Generating diffuse debug info...");
		    	timestamp = new Date();

		    	new File(lightFieldDirectory, "output/debug").mkdirs();
	    		
	    		OpenGLFramebufferObject diffuseDebugFBO = new OpenGLFramebufferObject(textureSize, textureSize, 2, true, false);
		    	OpenGLRenderable diffuseDebugRenderable = new OpenGLRenderable(diffuseDebugProgram);
		    	
		    	Iterable<OpenGLResource> worldToTextureVBOResources = diffuseDebugRenderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
		    	diffuseDebugRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
		    	diffuseDebugRenderable.program().setTexture("depthImages", lightField.depthTextures);
		    	diffuseDebugRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	diffuseDebugRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
		    	diffuseDebugRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
		    	diffuseDebugRenderable.program().setUniform("occlusionEnabled", lightField.settings.isOcclusionEnabled());
		    	diffuseDebugRenderable.program().setUniform("occlusionBias", lightField.settings.getOcclusionBias());
		    	
		    	//new File(lightFieldDirectory, "output/debug/diffuse/projpos").mkdirs();
		    	
		    	PrintStream diffuseInfo = new PrintStream(new File(lightFieldDirectory, "output/debug/diffuseInfo.txt"));
		    	
		    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
		    	{
		    		diffuseDebugRenderable.program().setUniform("viewIndex", i);
		    		
		    		diffuseDebugFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    		diffuseDebugFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    		diffuseDebugFBO.clearDepthBuffer();
		    		diffuseDebugRenderable.draw(PrimitiveMode.TRIANGLES, diffuseDebugFBO);
		    		
		    		//diffuseDebugFBO.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, String.format("output/debug/diffuse/%04d.png", i)));
		    		//diffuseDebugFBO.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, String.format("output/debug/diffuse/projpos/%04d.png", i)));
		    		
		    		Matrix4 cameraPose = lightField.viewSet.getCameraPose(i);
		    		Vector3 lightPosition = new Matrix3(cameraPose).transpose().times(
	    				lightField.viewSet.getLightPosition(lightField.viewSet.getLightPositionIndex(i))
	    					.minus(new Vector3(cameraPose.getColumn(3))));
		    		int[] colorData = diffuseDebugFBO.readColorBufferARGB(0, debugPixelX, debugPixelY, 1, 1);
		    		float[] positionData = diffuseDebugFBO.readFloatingPointColorBufferRGBA(1, debugPixelX, debugPixelY, 1, 1);
		    		diffuseInfo.println(
	    				lightPosition.x + "\t" +
						lightPosition.y + "\t" +
						lightPosition.z + "\t" +
	    				positionData[0] + "\t" +
	    				positionData[1] + "\t" +
	    				positionData[2] + "\t" +
						((colorData[0] & 0xFF000000) >>> 24) + "\t" + 
						((colorData[0] & 0x00FF0000) >>> 16) + "\t" + 
						((colorData[0] & 0x0000FF00) >>> 8) + "\t" +
						(colorData[0] & 0x000000FF));
		    	}	
		    	
		    	diffuseInfo.flush();
		    	diffuseInfo.close();	    	
		    	
		    	diffuseDebugFBO.delete();
		    	for (OpenGLResource r : worldToTextureVBOResources)
		    	{
		    		r.delete();
		    	}
		    	
				System.out.println("Diffuse debug info completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	System.out.println("Generating specular debug info...");
		    	timestamp = new Date();
	    		
		    	OpenGLFramebufferObject specularDebugFBO = new OpenGLFramebufferObject(textureSize, textureSize, 2, true, false);
		    	OpenGLRenderable specularDebugRenderable = new OpenGLRenderable(specularDebugProgram);
		    	
		    	Iterable<OpenGLResource> specularDebugResources = specularDebugRenderable.addVertexMesh("position", "texCoord", "normal", lightField.proxy);
		    	specularDebugRenderable.program().setTexture("viewImages", lightField.viewSet.getTextures());
		    	specularDebugRenderable.program().setTexture("depthImages", lightField.depthTextures);
		    	specularDebugRenderable.program().setUniform("occlusionEnabled", lightField.settings.isOcclusionEnabled());
		    	specularDebugRenderable.program().setUniform("occlusionBias", lightField.settings.getOcclusionBias());
		    	specularDebugRenderable.program().setTexture("diffuse", diffuseFitFrontFramebuffer.getColorAttachmentTexture(0));
		    	specularDebugRenderable.program().setTexture("normalMap", diffuseFitFrontFramebuffer.getColorAttachmentTexture(1));
		    	specularDebugRenderable.program().setUniformBuffer("CameraPoses", lightField.viewSet.getCameraPoseBuffer());
		    	specularDebugRenderable.program().setUniformBuffer("CameraProjections", lightField.viewSet.getCameraProjectionBuffer());
		    	specularDebugRenderable.program().setUniformBuffer("CameraProjectionIndices", lightField.viewSet.getCameraProjectionIndexBuffer());
		    	specularDebugRenderable.program().setUniform("gamma", gamma);
		    	specularDebugRenderable.program().setUniform("diffuseRemovalFactor", 1.0f);
		    	
		    	if (lightField.viewSet.getLightPositionBuffer() != null && lightField.viewSet.getLightIndexBuffer() != null)
	    		{
		    		specularDebugRenderable.program().setUniformBuffer("LightPositions", lightField.viewSet.getLightPositionBuffer());
		    		specularDebugRenderable.program().setUniformBuffer("LightIndices", lightField.viewSet.getLightIndexBuffer());
	    		}
		    	
		    	//new File(lightFieldDirectory, "output/debug/specular/rDotV").mkdirs();
		    	
		    	PrintStream specularInfo = new PrintStream(new File(lightFieldDirectory, "output/debug/specularInfo.txt"));
		    	
		    	for (int i = 0; i < lightField.viewSet.getCameraPoseCount(); i++)
		    	{
		    		specularDebugRenderable.program().setUniform("viewIndex", i);
		    		
		    		specularDebugFBO.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    		specularDebugFBO.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    		specularDebugFBO.clearDepthBuffer();
		    		specularDebugRenderable.draw(PrimitiveMode.TRIANGLES, specularDebugFBO);
		    		
		    		//specularDebugFBO.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, String.format("output/debug/specular/%04d.png", i)));
		    		//specularDebugFBO.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, String.format("output/debug/specular/rDotV/%04d.png", i)));
		    		
		    		int[] colorData = specularDebugFBO.readColorBufferARGB(0, debugPixelX, debugPixelY, 1, 1);
		    		int[] rDotVData = specularDebugFBO.readColorBufferARGB(1, debugPixelX, debugPixelY, 1, 1);
		    		specularInfo.println(	(rDotVData[0] & 0x000000FF) + "\t" +
										((colorData[0] & 0xFF000000) >>> 24) + "\t" + 
		    							((colorData[0] & 0x00FF0000) >>> 16) + "\t" + 
		    							((colorData[0] & 0x0000FF00) >>> 8) + "\t" +
		    							(colorData[0] & 0x000000FF));
		    	}
		    	
		    	specularInfo.flush();
		    	specularInfo.close();
		    	
		    	specularDebugFBO.delete();
		    	for (OpenGLResource r : specularDebugResources)
		    	{
		    		r.delete();
		    	}
		    	
				System.out.println("Specular debug info completed in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
				
				// Delete light field resources at this point to conserve memory
		        lightField.deleteOpenGLResources();
		    	
		    	System.out.println("Filling holes...");
		    	timestamp = new Date();
		    	
		    	OpenGLRenderable holeFillRenderable = new OpenGLRenderable(holeFillProgram);
		    	holeFillRenderable.addVertexBuffer("position", new OpenGLVertexBuffer(new FloatVertexList(2, 4, new float[] { -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f })), true);
		    	
		    	OpenGLFramebufferObject holeFillBackFramebuffer = new OpenGLFramebufferObject(textureSize, textureSize, 3, true, false);
		    	OpenGLFramebufferObject holeFillFrontFramebuffer = new OpenGLFramebufferObject(textureSize, textureSize, 3, true, false);

		    	holeFillFrontFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	holeFillFrontFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    	
		    	holeFillRenderable.program().setUniform("fillAll", false);
		    	holeFillRenderable.program().setUniform("minFillAlpha", 0.0f);
		    	
		    	holeFillRenderable.program().setUniform("defaultColor0", new Vector4(0.0f, 0.0f, 0.0f, 1.0f));
		    	holeFillRenderable.program().setUniform("defaultColor1", new Vector4(0.5f, 0.5f, 0.5f, 1.0f));
		    	
		    	for (int i = 0; i < diffuseFillIterations; i++)
		    	{
		    		holeFillRenderable.program().setTexture("mask0", diffuseFitFrontFramebuffer.getColorAttachmentTexture(0));
		    		holeFillRenderable.program().setTexture("mask1", diffuseFitFrontFramebuffer.getColorAttachmentTexture(1));

		    		if (i == 0)
		    		{
			    		holeFillRenderable.program().setTexture("input0", diffuseFitFrontFramebuffer.getColorAttachmentTexture(0));
			    		holeFillRenderable.program().setTexture("input1", diffuseFitFrontFramebuffer.getColorAttachmentTexture(1));
		    		}
		    		else
		    		{
			    		holeFillRenderable.program().setTexture("input0", holeFillFrontFramebuffer.getColorAttachmentTexture(0));
			    		holeFillRenderable.program().setTexture("input1", holeFillFrontFramebuffer.getColorAttachmentTexture(1));
		    		}
		    		
		    		if (i == diffuseFillIterations - 1)
		    		{
		    			// Final iteration: render to final framebuffer and ensure that every pixel has an alpha of 1.0
				    	holeFillRenderable.program().setUniform("fillAll", true);
		    			holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, diffuseFitBackFramebuffer);
		    		}
		    		else
		    		{
				    	holeFillBackFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
				    	holeFillBackFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
				    	
		    			holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, holeFillBackFramebuffer);
		    		}
		    		
		    		// Swap buffers
			        OpenGLFramebufferObject tmp = holeFillBackFramebuffer;
			        holeFillBackFramebuffer = holeFillFrontFramebuffer;
			        holeFillFrontFramebuffer = tmp;

			    	ulfToTexContext.flush();
		    	}

		    	holeFillFrontFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
		    	holeFillFrontFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
		    	holeFillFrontFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
		    	
		    	holeFillRenderable.program().setUniform("fillAll", false);
		    	holeFillRenderable.program().setUniform("minFillAlpha", minFillAlphaSpecular);
		    	
		    	holeFillRenderable.program().setUniform("defaultColor0", new Vector4(0.0f, 0.0f, 0.0f, 1.0f));
		    	holeFillRenderable.program().setUniform("defaultColor1", new Vector4(1.0f, 1.0f, 1.0f, 1.0f));
		    	holeFillRenderable.program().setUniform("defaultColor2", new Vector4(0.5f, 0.5f, 0.5f, 1.0f));
		    	
		    	for (int i = 0; i < specularFillIterations; i++)
		    	{
		    		// Intentionally use mask from diffuse fit since its more reliable
		    		holeFillRenderable.program().setTexture("mask0", diffuseFitFrontFramebuffer.getColorAttachmentTexture(0));
		    		holeFillRenderable.program().setTexture("mask1", diffuseFitFrontFramebuffer.getColorAttachmentTexture(0));
		    		holeFillRenderable.program().setTexture("mask2", diffuseFitFrontFramebuffer.getColorAttachmentTexture(0));

		    		if (i == 0)
		    		{
			    		holeFillRenderable.program().setTexture("input0", specularFitFrontFramebuffer.getColorAttachmentTexture(0));
			    		holeFillRenderable.program().setTexture("input1", specularFitFrontFramebuffer.getColorAttachmentTexture(1));
			    		holeFillRenderable.program().setTexture("input2", specularFitFrontFramebuffer.getColorAttachmentTexture(2));
		    		}
		    		else
		    		{
			    		holeFillRenderable.program().setTexture("input0", holeFillFrontFramebuffer.getColorAttachmentTexture(0));
			    		holeFillRenderable.program().setTexture("input1", holeFillFrontFramebuffer.getColorAttachmentTexture(1));
			    		holeFillRenderable.program().setTexture("input2", holeFillFrontFramebuffer.getColorAttachmentTexture(2));
		    		}
		    		
		    		if (i == specularFillIterations - 1)
		    		{
		    			// Final iteration: render to final framebuffer and ensure that every pixel has an alpha of 1.0
				    	holeFillRenderable.program().setUniform("fillAll", true);
				    	holeFillRenderable.program().setUniform("minFillAlpha", 0.0f);
		    			holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, specularFitBackFramebuffer);
		    		}
		    		else
		    		{
				    	holeFillBackFramebuffer.clearColorBuffer(0, 0.0f, 0.0f, 0.0f, 0.0f);
				    	holeFillBackFramebuffer.clearColorBuffer(1, 0.0f, 0.0f, 0.0f, 0.0f);
				    	holeFillBackFramebuffer.clearColorBuffer(2, 0.0f, 0.0f, 0.0f, 0.0f);
				    	
		    			holeFillRenderable.draw(PrimitiveMode.TRIANGLE_FAN, holeFillBackFramebuffer);
		    		}
		    		
		    		// Swap buffers
			        OpenGLFramebufferObject tmp = holeFillBackFramebuffer;
			        holeFillBackFramebuffer = holeFillFrontFramebuffer;
			        holeFillFrontFramebuffer = tmp;

			    	ulfToTexContext.flush();
		    	}
		    	
		    	if (diffuseFillIterations > 0)
		    	{
			        // Swap buffers
			        OpenGLFramebufferObject tmp = diffuseFitBackFramebuffer;
			        diffuseFitBackFramebuffer = diffuseFitFrontFramebuffer;
			        diffuseFitFrontFramebuffer = tmp;
		    	}
		        
		    	if (specularFillIterations > 0)
		    	{
		    		// Swap buffers
		    		OpenGLFramebufferObject tmp = specularFitBackFramebuffer;
			        specularFitBackFramebuffer = specularFitFrontFramebuffer;
			        specularFitFrontFramebuffer = tmp;
		    	}
		    	
	    		System.out.println("Holes filled in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		    	
		    	System.out.println("Saving textures...");
		    	timestamp = new Date();
		    	
		    	new File(lightFieldDirectory, "output/textures").mkdirs();
		        
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, "output/textures/diffuse.png"));
		        diffuseFitFrontFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, "output/textures/normal.png"));

		        specularFitFrontFramebuffer.saveColorBufferToFile(0, "PNG", new File(lightFieldDirectory, "output/textures/specular.png"));
		        specularFitFrontFramebuffer.saveColorBufferToFile(1, "PNG", new File(lightFieldDirectory, "output/textures/roughness.png"));
		        specularFitFrontFramebuffer.saveColorBufferToFile(2, "PNG", new File(lightFieldDirectory, "output/textures/snormal.png"));

		    	System.out.println("Textures saved in " + (new Date().getTime() - timestamp.getTime()) + " milliseconds.");
		        
		    	diffuseFitFrontFramebuffer.delete();
		    	specularFitFrontFramebuffer.delete();
		    	diffuseFitBackFramebuffer.delete();
		    	specularFitBackFramebuffer.delete();
    		}
    		
    		diffuseDebugProgram.delete();
    		diffuseFitProgram.delete();
    		specularFitProgram.delete();
    		specularDebugProgram.delete();
    		holeFillProgram.delete();
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        
        GLFWWindow.closeAllWindows();
        System.out.println("Process terminated with no errors.");
        System.exit(0);
	}
}
