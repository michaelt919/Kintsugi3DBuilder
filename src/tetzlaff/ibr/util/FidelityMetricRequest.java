package tetzlaff.ibr.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.ColorFormat;
import tetzlaff.gl.Context;
import tetzlaff.gl.Drawable;
import tetzlaff.gl.Framebuffer;
import tetzlaff.gl.FramebufferObject;
import tetzlaff.gl.PrimitiveMode;
import tetzlaff.gl.Program;
import tetzlaff.gl.ShaderType;
import tetzlaff.gl.UniformBuffer;
import tetzlaff.gl.nativebuffer.NativeDataType;
import tetzlaff.gl.nativebuffer.NativeVectorBuffer;
import tetzlaff.gl.nativebuffer.NativeVectorBufferFactory;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.IBRSettings;
import tetzlaff.ibr.LoadingMonitor;
import tetzlaff.ibr.ViewSet;
import tetzlaff.ibr.rendering.IBRResources;

public class FidelityMetricRequest implements IBRRequest
{
    private File fidelityExportPath;
    private File fidelityVSETFile;
    private IBRSettings settings;
    
	public FidelityMetricRequest(File exportPath, File targetVSETFile, IBRSettings settings)
	{
		this.fidelityExportPath = exportPath;
		this.fidelityVSETFile = targetVSETFile;
		this.settings = settings;
	}
	
	private <ContextType extends Context<ContextType>> 
		double calculateError(ContextType context, IBRResources<ContextType> resources, Drawable<ContextType> drawable, Framebuffer<ContextType> framebuffer, 
				NativeVectorBuffer viewIndexList, int targetViewIndex, int activeViewCount)
	{
		resources.setupShaderProgram(drawable.program(), false);
		drawable.program().setUniform("weightExponent", this.settings.getWeightExponent());
		drawable.program().setUniform("isotropyFactor", this.settings.getIsotropyFactor());
		drawable.program().setUniform("occlusionEnabled", resources.depthTextures != null && this.settings.isOcclusionEnabled());
		drawable.program().setUniform("occlusionBias", this.settings.getOcclusionBias());
    	
    	drawable.program().setUniform("model_view", resources.viewSet.getCameraPose(targetViewIndex));
    	drawable.program().setUniform("viewPos", resources.viewSet.getCameraPose(targetViewIndex).quickInverse(0.01f).getColumn(3).getXYZ());
    	drawable.program().setUniform("projection", 
    			resources.viewSet.getCameraProjection(resources.viewSet.getCameraProjectionIndex(targetViewIndex))
				.getProjectionMatrix(resources.viewSet.getRecommendedNearPlane(), resources.viewSet.getRecommendedFarPlane()));

    	drawable.program().setUniform("targetViewIndex", targetViewIndex);
		
    	try (UniformBuffer<ContextType> viewIndexBuffer = context.createUniformBuffer().setData(viewIndexList))
    	{
	    	drawable.program().setUniformBuffer("ViewIndices", viewIndexBuffer);
	    	drawable.program().setUniform("viewCount", activeViewCount);
	    	
	    	framebuffer.clearColorBuffer(0, -1.0f, -1.0f, -1.0f, -1.0f);
	    	framebuffer.clearDepthBuffer();
	    	
	    	drawable.draw(PrimitiveMode.TRIANGLES, framebuffer);
    	}

    	try
    	{
	        if (activeViewCount == resources.viewSet.getCameraPoseCount() - 1 /*&& this.assets.viewSet.getImageFileName(i).matches(".*R1[^1-9].*")*/)
	        {
		    	File fidelityImage = new File(new File(fidelityExportPath.getParentFile(), "debug"), resources.viewSet.getImageFileName(targetViewIndex));
		        framebuffer.saveColorBufferToFile(0, "PNG", fidelityImage);
	        }
    	}
    	catch(IOException e)
    	{
    		e.printStackTrace();
    	}
        	
        double sumSqError = 0.0;
        //double sumWeights = 0.0;
        double sumMask = 0.0;

    	float[] fidelityArray = framebuffer.readFloatingPointColorBufferRGBA(0);
    	for (int k = 0; 4 * k + 3 < fidelityArray.length; k++)
    	{
			if (fidelityArray[4 * k + 1] >= 0.0f)
			{
				sumSqError += fidelityArray[4 * k];
				//sumWeights += fidelityArray[4 * k + 1];
				sumMask += 1.0;
			}
    	}
    	
    	return Math.sqrt(sumSqError / sumMask);
	}

	@Override
	public <ContextType extends Context<ContextType>> void executeRequest(ContextType context, IBRRenderable<ContextType> renderable, LoadingMonitor callback) throws IOException
	{
		IBRResources<ContextType> resources = renderable.getResources();
		
		System.out.println("\nView Importance:");
		
    	Vector3[] viewDirections = new Vector3[resources.viewSet.getCameraPoseCount()];
    	
    	for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
    	{
    		viewDirections[i] = resources.viewSet.getCameraPoseInverse(i).getColumn(3).getXYZ()
    				.minus(resources.geometry.getCentroid()).normalized();
		}
    	
    	double[][] viewDistances = new double[resources.viewSet.getCameraPoseCount()][resources.viewSet.getCameraPoseCount()];
    	
    	for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
    	{
    		for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
    		{
    			viewDistances[i][j] = Math.acos(Math.max(-1.0, Math.min(1.0f, viewDirections[i].dot(viewDirections[j]))));
    		}
    	}
    	
    	try
    	(
			Program<ContextType> fidelityProgram = context.getShaderProgramBuilder()
				.addShader(ShaderType.VERTEX, new File("shaders/common/texspace_noscale.vert"))
				.addShader(ShaderType.FRAGMENT, new File("shaders/relight/fidelity.frag"))
				.createProgram();
    			
			FramebufferObject<ContextType> framebuffer = context.buildFramebufferObject(256, 256/*1024, 1024*/)
				.addColorAttachment(ColorFormat.RG32F)
				.createFramebufferObject();
    			
			PrintStream out = new PrintStream(fidelityExportPath);
		)
    	{
    		context.getState().disableBackFaceCulling();
    		
    		Drawable<ContextType> drawable = context.createDrawable(fidelityProgram);
        	drawable.addVertexBuffer("position", resources.positionBuffer);
        	drawable.addVertexBuffer("texCoord", resources.texCoordBuffer);
        	drawable.addVertexBuffer("normal", resources.normalBuffer);
        	drawable.addVertexBuffer("tangent", resources.tangentBuffer);
        	
    		double[] slopes = new double[resources.viewSet.getCameraPoseCount()];
    		double[] peaks = new double[resources.viewSet.getCameraPoseCount()];
    		
			if (callback != null)
			{
				callback.setMaximum(resources.viewSet.getCameraPoseCount());
			}
			
			new File(fidelityExportPath.getParentFile(), "debug").mkdir();
    		
    		for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
			{
    			System.out.println(resources.viewSet.getImageFileName(i));
    			out.print(resources.viewSet.getImageFileName(i) + "\t");
    			
    			double lastMinDistance = 0.0;
    			double minDistance;
    			int activeViewCount;
    			double sumMask = 0.0;
    			
    			List<Double> distances = new ArrayList<Double>();
    			List<Double> errors = new ArrayList<Double>();
    			
    			distances.add(0.0);
    			errors.add(0.0);
    			
    			do 
    			{
			    	NativeVectorBuffer viewIndexList = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.INT, 1, resources.viewSet.getCameraPoseCount());
			    	
			    	activeViewCount = 0;
			    	minDistance = Float.MAX_VALUE;
			    	for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
			    	{
			    		if (i != j && viewDistances[i][j] > lastMinDistance)
			    		{
			    			minDistance = Math.min(minDistance, viewDistances[i][j]);
			    			viewIndexList.set(activeViewCount, 0, j);
			    			activeViewCount++;
			    		}
			    	}
			    	
			    	if (activeViewCount > 0)
			    	{
				    	if (sumMask >= 0.0)
				    	{
					        distances.add(minDistance);
					        errors.add(calculateError(context, resources, drawable, framebuffer, viewIndexList, i, activeViewCount));
					    	lastMinDistance = minDistance;
				    	}
			    	}
    			}
    			while(sumMask >= 0.0 && activeViewCount > 0 && minDistance < /*0*/ Math.PI / 4);
    			
    			// Fit the error v. distance data to a quadratic with a few constraints.
    			// First, the quadratic must pass through the origin.
    			// Second, the slope at the origin must be positive.
    			// Finally, the "downward" slope of the quadratic will be clamped to the quadratic's maximum value 
    			// to ensure that the function is monotonically increasing or constant.
    			// (So only half of the quadratic will actually be used.)
    			double peak = -1.0, slope = -1.0;
    			double maxDistance = distances.get(distances.size() - 1);
    			double prevPeak, prevSlope, prevMaxDistance;
    			
    			// Every time we fit a quadratic, the data that would have been clamped on the downward slope messes up the fit.
    			// So we should keep redoing the fit without that data affecting the initial slope and only affecting the peak value.
    			// This continues until convergence (no new data points are excluded from the quadratic).
    			do
    			{
	    			double sumSquareDistances = 0.0;
	    			double sumCubeDistances = 0.0;
	    			double sumFourthDistances = 0.0;
	    			double sumErrorDistanceProducts = 0.0;
	    			double sumErrorSquareDistanceProducts = 0.0;
	    			
	    			double sumHighErrors = 0.0;
	    			int countHighErrors = 0;
	    			
	    			for (int k = 0; k < distances.size(); k++)
	    			{
	    				double distance = distances.get(k);
	    				double error = errors.get(k);
	    				
	    				if (distance < maxDistance)
	    				{
	    					double distanceSq = distance * distance;
	    				
		    				sumSquareDistances += distanceSq;
		    				sumCubeDistances += distance * distanceSq;
		    				sumFourthDistances += distanceSq * distanceSq;
		    				sumErrorDistanceProducts += error * distance;
		    				sumErrorSquareDistanceProducts += error * distanceSq;
	    				}
	    				else
	    				{
	    					sumHighErrors += error;
	    					countHighErrors++;
	    				}
	    			}
	    			
	    			prevPeak = peak;
    				prevSlope = slope;
    				
    				// Fit error vs. distance to a quadratic using least squares: a*x^2 + slope * x = error
	    			double d = (sumCubeDistances * sumCubeDistances - sumFourthDistances * sumSquareDistances);
	    			double a = (sumCubeDistances * sumErrorDistanceProducts - sumSquareDistances * sumErrorSquareDistanceProducts) / d;
	    			
	    			slope = (sumCubeDistances * sumErrorSquareDistanceProducts - sumFourthDistances * sumErrorDistanceProducts) / d;
	    			
	    			if (slope <= 0.0 || !Double.isFinite(slope) || countHighErrors > errors.size() - 5)
	    			{
	    				if (prevSlope < 0.0)
	    				{
	    					// If its the first iteration, use a linear function
		    				// peak=0 is a special case for designating a linear function
		    				peak = 0.0;
		    				slope = sumErrorDistanceProducts / sumSquareDistances;
	    				}
	    				else
	    				{
		    				// Revert to the previous peak and slope
		    				slope = prevSlope;
		    				peak = prevPeak;
	    				}
	    			}
	    			else
	    			{
		    			// Peak can be determined from a and the slope.
		    			double leastSquaresPeak = slope * slope / (-4 * a);

		    			if (Double.isFinite(leastSquaresPeak) && leastSquaresPeak > 0.0)
		    			{
		    				if (countHighErrors == 0)
		    				{
		    					peak = leastSquaresPeak;
		    				}
		    				else
		    				{
				    			// Do a weighted average between the least-squares peak and the average of all the errors that would be on the downward slope of the quadratic,
				    			// but are instead clamped to the maximum of the quadratic.
				    			// Clamp the contribution of the least-squares peak to be no greater than twice the average of the other values.
				    			peak = (Math.min(2 * sumHighErrors / countHighErrors, leastSquaresPeak) * (errors.size() - countHighErrors) + sumHighErrors) / errors.size();
		    				}
		    			}
		    			else if (prevPeak < 0.0)
	    				{
	    					// If its the first iteration, use a linear function
		    				// peak=0 is a special case for designating a linear function
		    				peak = 0.0;
		    				slope = sumErrorDistanceProducts / sumSquareDistances;
	    				}
	    				else
	    				{
		    				// Revert to the previous peak and slope
		    				slope = prevSlope;
		    				peak = prevPeak;
	    				}
	    			}
	    			
	    			// Update the max distance and previous max distance.
	    			prevMaxDistance = maxDistance;
	    			maxDistance = 2 * peak / slope;
    			}
    			while(maxDistance < prevMaxDistance && peak > 0.0);
    			
    			if (errors.size() >= 2)
    			{
    				out.println(slope + "\t" + peak + "\t" + minDistance + "\t" + errors.get(1));
    			}
    			
    			System.out.println("Slope: " + slope);
    			System.out.println("Peak: " + peak);
    			System.out.println();
    			
    			slopes[i] = slope;
    			peaks[i] = peak;
    			
    			for (Double distance : distances)
    			{
    				out.print(distance + "\t");
    			}
    			out.println();

    			for (Double error : errors)
    			{
    				out.print(error + "\t");
    			}
    			out.println();
    			
    			out.println();
		        
		        if (callback != null)
		        {
		        	callback.setProgress(i);
		        }
			}
    		
    		if (fidelityVSETFile != null && fidelityVSETFile.exists())
    		{
	    		out.println();
	    		out.println("Expected error for views in target view set:");
	    		out.println();
	    		
	    		ViewSet targetViewSet = ViewSet.loadFromVSETFile(fidelityVSETFile);
	    		
	    		Vector3[] targetDirections = new Vector3[targetViewSet.getCameraPoseCount()];
	    		
	    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
	        	{
	    			targetDirections[i] = targetViewSet.getCameraPoseInverse(i).getColumn(3).getXYZ()
	        				.minus(resources.geometry.getCentroid()).normalized();
	    		}
	    		
	    		// Determine a function describing the error of each quadratic view by blending the slope and peak parameters from the known views.
	    		double[] targetSlopes = new double[targetViewSet.getCameraPoseCount()];
	    		double[] targetPeaks = new double[targetViewSet.getCameraPoseCount()];
	    		
	    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
	    		{
	    			double weightedSlopeSum = 0.0;
	    			double weightSum = 0.0;
	    			double weightedPeakSum = 0.0;
	    			double peakWeightSum = 0.0;
	    			
	    			for (int k = 0; k < slopes.length; k++)
	    			{
	    				double weight = 1 / Math.max(0.000001, 1.0 - 
	    						Math.pow(Math.max(0.0, targetDirections[i].dot(viewDirections[k])), this.settings.getWeightExponent())) 
							- 1.0;
	    				
	    				if (peaks[k] > 0)
    					{
	    					weightedPeakSum += weight * peaks[k];
	    					peakWeightSum += weight;
    					}
	    				
						weightedSlopeSum += weight * slopes[k];
	    				weightSum += weight;
	    			}
	    			
	    			targetSlopes[i] = weightedSlopeSum / weightSum;
	    			targetPeaks[i] = peakWeightSum == 0.0 ? 0.0 : weightedPeakSum / peakWeightSum;
	    		}
	    		
	    		double[] targetDistances = new double[targetViewSet.getCameraPoseCount()];
	    		double[] targetErrors = new double[targetViewSet.getCameraPoseCount()];
	    		
	    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
	    		{
    				targetDistances[i] = Double.MAX_VALUE;
	    		}
	    		
	    		boolean[] originalUsed = new boolean[resources.viewSet.getCameraPoseCount()];
				
				NativeVectorBuffer viewIndexList = NativeVectorBufferFactory.getInstance().createEmpty(NativeDataType.INT, 1, resources.viewSet.getCameraPoseCount());
	    		int activeViewCount = 0;
	    		
	    		// Print views that are only in the original view set and NOT in the target view set
	    		// This also initializes the distances for the target views.
	    		for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
	    		{
	    			// First determine if the view is in the target view set
	    			boolean found = false;
	    			for (int i = 0; !found && i < targetViewSet.getCameraPoseCount(); i++)
	    			{
	    				if (targetViewSet.getImageFileName(i).contains(resources.viewSet.getImageFileName(j).split("\\.")[0]))
	    				{
	    					found = true;
	    				}
	    			}
	    			
	    			if (!found)
	    			{
	    				// If it isn't, then print it to the file
	    				originalUsed[j] = true;
	    				out.print(resources.viewSet.getImageFileName(j).split("\\.")[0] + "\t" + slopes[j] + "\t" + peaks[j] + "\tn/a\t" + 
	    						calculateError(context, resources, drawable, framebuffer, viewIndexList, j, activeViewCount) + "\t");

	    				viewIndexList.set(activeViewCount, 0, j);
	    				activeViewCount++;

			    		double cumError = 0.0;
			    		
			    		for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
			    		{
			    			if (!originalUsed[k])
			    			{
			    				cumError += calculateError(context, resources, drawable, framebuffer, viewIndexList, k, activeViewCount);
			    			}
			    		}
			    		
			    		out.println(cumError);
	    				
	    				// Then update the distances for all of the target views
	    				for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		    			{
		    				targetDistances[i] = Math.min(targetDistances[i], Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(viewDirections[j])))));
		    			}
	    			}
	    		}
	    		
				// Now update the errors for all of the target views
				for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
    			{
    				if (Double.isFinite(targetPeaks[i]))
					{
    					double peakDistance = 2 * targetPeaks[i] / targetSlopes[i];
    					if (targetDistances[i] > peakDistance)
    					{
    						targetErrors[i] = targetPeaks[i];
    					}
    					else
    					{
    						targetErrors[i] = targetSlopes[i] * targetDistances[i] - targetSlopes[i] * targetSlopes[i] * targetDistances[i] * targetDistances[i] / (4 * targetPeaks[i]);
    					}
					}
    				else
    				{
    					targetErrors[i] = targetSlopes[i] * targetDistances[i];
    				}
    			}
	    		
	    		boolean[] targetUsed = new boolean[targetErrors.length];

	    		int unusedOriginalViews = 0;
	    		for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
	    		{
	    			if (!originalUsed[j])
	    			{
	    				unusedOriginalViews++;
	    			}
	    		}
	    		
	    		// Views that are in both the target view set and the original view set
	    		// Go through these views in order of importance so that when loaded viewset = target viewset, it generates a ground truth ranking.
	    		while(unusedOriginalViews > 0)
	    		{
	    			double maxError = -1.0;
	    			int maxErrorTargetIndex = -1;
	    			int maxErrorOriginalIndex = -1;
	    			
	    			// Determine which view to do next.  Must be in both view sets and currently have more error than any other view in both view sets.
		    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		    		{
	    	    		for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++) 
		    			{
		    				if (targetViewSet.getImageFileName(i).contains(resources.viewSet.getImageFileName(j).split("\\.")[0]))
		    				{
		    					// Can't be previously used and must have more error than any other view
				    			if (!originalUsed[j] && targetErrors[i] > maxError)
			    				{
			    					maxError = targetErrors[i];
			    					maxErrorTargetIndex = i;
			    					maxErrorOriginalIndex = j;
			    				}
		    				}
		    			}
		    		}
		    		
		    		// Print the view to the file
		    		out.print(targetViewSet.getImageFileName(maxErrorTargetIndex).split("\\.")[0] + "\t" + targetSlopes[maxErrorTargetIndex] + "\t" + targetPeaks[maxErrorTargetIndex] + "\t" + 
	    					targetDistances[maxErrorTargetIndex] + "\t" + targetErrors[maxErrorTargetIndex] + "\t");
					
		    		// Flag that its been used
					targetUsed[maxErrorTargetIndex] = true;
					originalUsed[maxErrorOriginalIndex] = true;
					
					double expectedCumError = 0.0;
	    			
					// Update all of the other target distances and errors that haven't been used yet
	    			for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++) 
	    			{
    					// Don't update previously used views
	    				if (!targetUsed[i])
	    				{
	    					// distance
	    					targetDistances[i] = Math.min(targetDistances[i], 
	    							Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(targetDirections[maxErrorTargetIndex])))));

	    					// error
	        				if (Double.isFinite(targetPeaks[i]))
	    					{
	        					double peakDistance = 2 * targetPeaks[i] / targetSlopes[i];
	        					if (targetDistances[i] > peakDistance)
	        					{
	        						targetErrors[i] = targetPeaks[i];
	        					}
	        					else
	        					{
	        						targetErrors[i] = targetSlopes[i] * targetDistances[i] - targetSlopes[i] * targetSlopes[i] * targetDistances[i] * targetDistances[i] / (4 * targetPeaks[i]);
	        					}
	    					}
	        				else
	        				{
	        					targetErrors[i] = targetSlopes[i] * targetDistances[i];
	        				}
	        				
	        				expectedCumError += targetErrors[i];
	    				}
	    			}

		    		out.println(expectedCumError);
	    			
	    			// Count how many views from the original view set haven't been used.
	    			unusedOriginalViews = 0;
		    		for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
		    		{
		    			if (!originalUsed[j])
		    			{
		    				unusedOriginalViews++;
		    			}
		    		}
	    		}

	    		// Views that are in the target view set and NOT in the original view set
    			int unused;
	    		do
	    		{
	    			unused = 0;
	    			double maxError = -1.0;
	    			int maxErrorIndex = -1;

	    			// Determine which view to do next.  Must be in both view sets and currently have more error than any other view in both view sets.
		    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		    		{
    					// Can't be previously used and must have more error than any other view
		    			if (!targetUsed[i])
		    			{
		    				// Keep track of number of unused views at the same time
		    				unused++;
		    				
		    				if (targetErrors[i] > maxError)
		    				{
		    					maxError = targetErrors[i];
		    					maxErrorIndex = i;
		    				}
		    			}
		    		}
		    		
		    		if (maxErrorIndex >= 0)
		    		{
			    		// Print the view to the file
		    			out.print(targetViewSet.getImageFileName(maxErrorIndex).split("\\.")[0] + "\t" + targetSlopes[maxErrorIndex] + "\t" + targetPeaks[maxErrorIndex] + "\t" + 
    	    					targetDistances[maxErrorIndex] + "\t" + targetErrors[maxErrorIndex] + "\t"); 
		    			
		    			// Flag that its been used
		    			targetUsed[maxErrorIndex] = true;
			    		unused--;
			    		
			    		double cumError = 0.0;
		    			
						// Update all of the other target distances and errors
			    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++) 
		    			{
	    					// Don't update previously used views
		    				if (!targetUsed[i])
		    				{
		    					// distance
		    					targetDistances[i] = Math.min(targetDistances[i], 
		    							Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(targetDirections[maxErrorIndex])))));
	
		    					// error
		        				if (Double.isFinite(targetPeaks[i]))
		    					{
		        					double peakDistance = 2 * targetPeaks[i] / targetSlopes[i];
		        					if (targetDistances[i] > peakDistance)
		        					{
		        						targetErrors[i] = targetPeaks[i];
		        					}
		        					else
		        					{
		        						targetErrors[i] = targetSlopes[i] * targetDistances[i] - targetSlopes[i] * targetSlopes[i] * targetDistances[i] * targetDistances[i] / (4 * targetPeaks[i]);
		        					}
		    					}
		        				else
		        				{
		        					targetErrors[i] = targetSlopes[i] * targetDistances[i];
		        				}
		        				
		        				cumError += targetErrors[i];
		    				}
		    			}
			    		
			    		out.println(cumError);
		    		}
	    		}
	    		while(unused > 0);
    		}
    	}
	}
}
