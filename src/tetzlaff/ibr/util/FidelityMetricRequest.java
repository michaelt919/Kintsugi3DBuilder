package tetzlaff.ibr.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import tetzlaff.gl.Context;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.LoadingMonitor;
import tetzlaff.ibr.ViewSet;
import tetzlaff.ibr.rendering.IBRResources;
import tetzlaff.ibr.rendering2.to_sort.IBRSettings2;
import tetzlaff.ibr.util.fidelity.FidelityEvaluationTechnique;
import tetzlaff.ibr.util.fidelity.TextureFitFidelityTechnique;
import tetzlaff.util.CubicHermiteSpline;

public class FidelityMetricRequest implements IBRRequest
{
	private final static boolean DEBUG = true;
	
	private final static boolean USE_RENDERER_WEIGHTS = true;
	private final static boolean USE_PERCEPTUALLY_LINEAR_ERROR = false;
	
    private File fidelityExportPath;
    private File fidelityVSETFile;
    private IBRSettings2 settings;
    
	public FidelityMetricRequest(File exportPath, File targetVSETFile, IBRSettings2 settings)
	{
		this.fidelityExportPath = exportPath;
		this.fidelityVSETFile = targetVSETFile;
		this.settings = settings;
	}
	
	private double estimateErrorQuadratic(double baseline, double slope, double peak, double distance)
	{
		if (Double.isFinite(peak))
		{
			double peakDistance = 2 * peak / slope;
			if (distance > peakDistance)
			{
				return baseline + peak;
			}
			else
			{
				return baseline + slope * distance - slope * slope * distance * distance / (4 * peak);
			}
		}
		else
		{
			return baseline + slope * distance;
		}
	}
	
	private double estimateErrorFromSplines(List<Vector3> directions, List<CubicHermiteSpline> splines, Vector3 targetDirection, double targetDistance)
	{
		PriorityQueue<AbstractMap.SimpleEntry<Double, CubicHermiteSpline>> splineQueue 
			= new PriorityQueue<>(Comparator.<AbstractMap.SimpleEntry<Double, CubicHermiteSpline>>comparingDouble(entry -> entry.getKey())
					.reversed());
		
		for (int i = 0; i < directions.size(); i++)
		{
			double distance = Math.acos(Math.max(-1.0, Math.min(1.0f, directions.get(i).dot(targetDirection))));
			splineQueue.add(new AbstractMap.SimpleEntry<Double, CubicHermiteSpline>(distance, splines.get(i)));
			if (splineQueue.size() > 5)
			{
				splineQueue.remove();
			}
		}
		
		double thresholdInv = Math.min(500000.0, 1.0 / splineQueue.remove().getKey());
		
		double sum = 0.0;
		double sumWeights = 0.0;
		while (!splineQueue.isEmpty())
		{
			AbstractMap.SimpleEntry<Double, CubicHermiteSpline> next = splineQueue.remove();
			double weight = Math.min(1000000.0, 1.0 / next.getKey()) - thresholdInv;
			sum += weight * next.getValue().applyAsDouble(targetDistance);
			sumWeights += weight;
		}
		
		return sum / sumWeights;
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
		
		File debugDirectory = null;
		if (DEBUG)
		{
			debugDirectory = new File(fidelityExportPath.getParentFile(), "debug");
			debugDirectory.mkdir();
		}
    	
    	try
    	(
			FidelityEvaluationTechnique<ContextType> fidelityTechnique = 
				new TextureFitFidelityTechnique<ContextType>(USE_PERCEPTUALLY_LINEAR_ERROR);
//    			USE_RENDERER_WEIGHTS ? new IBRFidelityTechnique<ContextType>()
//					: new LinearSystemFidelityTechnique<ContextType>(USE_PERCEPTUALLY_LINEAR_ERROR, debugDirectory);
    			
			PrintStream out = new PrintStream(fidelityExportPath);
		)
    	{
        	fidelityTechnique.initialize(resources, settings, 256);
    		
    		double[] slopes = new double[resources.viewSet.getCameraPoseCount()];
    		double[] peaks = new double[resources.viewSet.getCameraPoseCount()];
    		double[] baselines = new double[resources.viewSet.getCameraPoseCount()];
    		
			if (callback != null)
			{
				callback.setMaximum(resources.viewSet.getCameraPoseCount());
				callback.setProgress(0.0);
			}
			
			CubicHermiteSpline[] errorFunctions = new CubicHermiteSpline[resources.viewSet.getCameraPoseCount()];
			
			out.println("#Name\tBaseline\tSlope\tPeak\tMinDistance\tError\t(CumError)");
    		
    		for (int i = 0; i < resources.viewSet.getCameraPoseCount(); i++)
			{
    			System.out.println(resources.viewSet.getImageFileName(i));
    			out.print(resources.viewSet.getImageFileName(i) + "\t");
    			
    			double lastMinDistance = 0.0;
    			double minDistance;
    			
    			List<Double> distances = new ArrayList<Double>();
    			List<Double> errors = new ArrayList<Double>();
    			
    			baselines[i] = fidelityTechnique.evaluateBaselineError(i, DEBUG ? 
    				new File(new File(fidelityExportPath.getParentFile(), "debug"),
    						"baseline_" + renderable.getActiveViewSet().getImageFileName(i)) 
						: null);
    			
    			distances.add(0.0);
    			errors.add(baselines[i]);
    			
    			List<Integer> activeViewIndexList;
    			
		    	do 
    			{
    				activeViewIndexList = new ArrayList<Integer>();
    			
			    	minDistance = Float.MAX_VALUE;
			    	for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
			    	{
			    		if (i != j && viewDistances[i][j] > lastMinDistance)
			    		{
			    			minDistance = Math.min(minDistance, viewDistances[i][j]);
			    			activeViewIndexList.add(j);
			    			fidelityTechnique.updateActiveViewIndexList(activeViewIndexList);
			    		}
			    	}
			    	
			    	if (activeViewIndexList.size() > 0)
			    	{
				        distances.add(minDistance);
				        
				        errors.add(fidelityTechnique.evaluateError(i, 
				        		DEBUG && activeViewIndexList.size() == resources.viewSet.getCameraPoseCount() - 1 ? 
				        				new File(new File(fidelityExportPath.getParentFile(), "debug"), 
			        							renderable.getActiveViewSet().getImageFileName(i)) 
				        				: null));
				        
				    	lastMinDistance = minDistance;
			    	}
    			}
    			while(Double.isFinite(errors.get(errors.size() - 1)) && activeViewIndexList.size() > 0 && minDistance < /*0*/ Math.PI / 4);
    			
    			double[] errorArray = new double[errors.size()];
    			double[] distanceArray = new double[distances.size()];
    			
    			for (int k = 0; k < errors.size(); k++)
    			{
    				errorArray[k] = errors.get(k);
    			}
    			
    			for (int k = 0; k < distances.size(); k++)
    			{
    				distanceArray[k] = distances.get(k);
    			}
    			
    			errorFunctions[i] = new CubicHermiteSpline(distanceArray, errorArray, true);
    			
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
	    			
	    			double sumHighErrorDiffs = 0.0;
	    			int countHighErrorDiffs = 0;
	    			
	    			for (int k = 0; k < distances.size(); k++)
	    			{
	    				double distance = distances.get(k);
	    				double errorDiff = Math.max(0.0, errors.get(k) - baselines[i]);
	    				
	    				if (distance < maxDistance)
	    				{
	    					double distanceSq = distance * distance;
	    				
		    				sumSquareDistances += distanceSq;
		    				sumCubeDistances += distance * distanceSq;
		    				sumFourthDistances += distanceSq * distanceSq;
		    				sumErrorDistanceProducts += errorDiff * distance;
		    				sumErrorSquareDistanceProducts += errorDiff * distanceSq;
	    				}
	    				else
	    				{
	    					sumHighErrorDiffs += errorDiff;
	    					countHighErrorDiffs++;
	    				}
	    			}
	    			
	    			prevPeak = peak;
    				prevSlope = slope;
    				
    				// Fit error vs. distance to a quadratic using least squares: a*x^2 + slope * x = error
	    			double d = (sumCubeDistances * sumCubeDistances - sumFourthDistances * sumSquareDistances);
	    			double a = (sumCubeDistances * sumErrorDistanceProducts - sumSquareDistances * sumErrorSquareDistanceProducts) / d;
	    			
	    			slope = (sumCubeDistances * sumErrorSquareDistanceProducts - sumFourthDistances * sumErrorDistanceProducts) / d;
	    			
	    			if (slope <= 0.0 || !Double.isFinite(slope) || countHighErrorDiffs > errors.size() - 5)
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
		    				if (countHighErrorDiffs == 0)
		    				{
		    					peak = leastSquaresPeak;
		    				}
		    				else
		    				{
				    			// Do a weighted average between the least-squares peak and the average of all the errors that would be on the downward slope of the quadratic,
				    			// but are instead clamped to the maximum of the quadratic.
				    			// Clamp the contribution of the least-squares peak to be no greater than twice the average of the other values.
				    			peak = (Math.min(2 * sumHighErrorDiffs / countHighErrorDiffs, leastSquaresPeak) * (errors.size() - countHighErrorDiffs) + sumHighErrorDiffs) / errors.size();
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
    				out.println(baselines[i] + "\t" + slope + "\t" + peak + "\t" + minDistance + "\t" + errors.get(1));
    			}

    			System.out.println("Baseline: " + baselines[i]);
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
	    		
    			if (callback != null)
    			{
    				callback.setMaximum(resources.viewSet.getCameraPoseCount());
		        	callback.setProgress(0);
    			}
	    		
	    		Vector3[] targetDirections = new Vector3[targetViewSet.getCameraPoseCount()];
	    		
	    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
	        	{
	    			targetDirections[i] = targetViewSet.getCameraPoseInverse(i).getColumn(3).getXYZ()
	        				.minus(resources.geometry.getCentroid()).normalized();
	    		}
	    		
	    		// Determine a function describing the error of each quadratic view by blending the slope and peak parameters from the known views.
	    		double[] targetBaselines = new double[targetViewSet.getCameraPoseCount()];
	    		double[] targetSlopes = new double[targetViewSet.getCameraPoseCount()];
	    		double[] targetPeaks = new double[targetViewSet.getCameraPoseCount()];
	    		
	    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
	    		{
	    			double weightedBaselineSum = 0.0;
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
	    				
	    				weightedBaselineSum += weight * baselines[k];
						weightedSlopeSum += weight * slopes[k];
	    				weightSum += weight;
	    			}
	    			
	    			targetBaselines[i] = weightedBaselineSum / weightSum;
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
				
	    		List<Integer> activeViewIndexList = new ArrayList<Integer>();
	    		fidelityTechnique.updateActiveViewIndexList(activeViewIndexList);
	    		
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
	    				out.print(resources.viewSet.getImageFileName(j).split("\\.")[0] + "\t" + baselines[j] + "\t" + slopes[j] + "\t" + peaks[j] + "\tn/a\t");
	    				
	    				out.print(fidelityTechnique.evaluateError(j) + "\t");
	    				
	    				activeViewIndexList.add(j);
	    				fidelityTechnique.updateActiveViewIndexList(activeViewIndexList);

			    		double cumError = 0.0;
			    		
			    		for (int k = 0; k < resources.viewSet.getCameraPoseCount(); k++)
			    		{
			    			if (!originalUsed[k] || !fidelityTechnique.isGuaranteedInterpolating())
			    			{
			    				cumError += fidelityTechnique.evaluateError(k);
			    			}
			    		}
			    		
			    		out.println(cumError);
	    				
	    				// Then update the distances for all of the target views
	    				for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		    			{
		    				targetDistances[i] = Math.min(targetDistances[i], Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(viewDirections[j])))));
		    			}
	    			}
	    			
			        if (callback != null)
			        {
			        	callback.setProgress(activeViewIndexList.size());
			        }
	    		}
	    		
				// Now update the errors for all of the target views
				for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
    			{
					if (!fidelityTechnique.isGuaranteedMonotonic())
					{
						targetErrors[i] = estimateErrorQuadratic(targetBaselines[i], targetSlopes[i], targetPeaks[i], targetDistances[i]);
					}
					else
					{
						targetErrors[i] = estimateErrorFromSplines(Arrays.asList(viewDirections), Arrays.asList(errorFunctions), targetDirections[i], targetDistances[i]);
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
//	    			double maxError = -1.0;
	    			int nextViewTargetIndex = -1;
	    			int nextViewOriginalIndex = -1;
	    			
	    			// Determine which view to do next.  Must be in both view sets and currently have more error than any other view in both view sets.
//		    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
//		    		{
//	    	    		for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++) 
//		    			{
//		    				if (targetViewSet.getImageFileName(i).contains(resources.viewSet.getImageFileName(j).split("\\.")[0]))
//		    				{
//		    					// Can't be previously used and must have more error than any other view
//				    			if (!originalUsed[j] && targetErrors[i] > maxError)
//			    				{
//			    					maxError = targetErrors[i];
//			    					nextViewTargetIndex = i;
//			    					nextViewOriginalIndex = j;
//			    				}
//		    				}
//		    			}
//		    		}
		    		
	    			double minTotalError = Double.MAX_VALUE;
	    			
	    			int activeViewCount = activeViewIndexList.size();
	    			activeViewIndexList.add(-1);
	    			
		    		for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++)
		    		{
		    			if (!targetUsed[i])
		    			{
			    			for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++) 
			    			{
		    	    			if (targetViewSet.getImageFileName(i).contains(resources.viewSet.getImageFileName(j).split("\\.")[0]))
		    	    			{
		    	    				activeViewIndexList.set(activeViewCount, j);
		    	    				fidelityTechnique.updateActiveViewIndexList(activeViewIndexList);
		    	    				
		    	    				double totalError = 0.0;
			    	    			for (int k = 0; k < targetViewSet.getCameraPoseCount(); k++)
			    	    			{
			    	    				if ((k != i && !targetUsed[k]) || !fidelityTechnique.isGuaranteedInterpolating())
			    	    				{
				    	    				for (int l = 0; l < resources.viewSet.getCameraPoseCount(); l++)
				    	    				{
				    	    					if (targetViewSet.getImageFileName(k).contains(resources.viewSet.getImageFileName(l).split("\\.")[0]))
				    		    				{
				    	    						totalError += fidelityTechnique.evaluateError(l);
				    	    	    				break;
				    		    				}
				    	    				}
			    	    				}
			    	    			}
			    	    			
			    	    			if (totalError < minTotalError)
			    	    			{
			    	    				nextViewTargetIndex = i;
			    	    				nextViewOriginalIndex = j;
			    	    				minTotalError = totalError;
			    	    			}
			    	    			
			    	    			break;
		    	    			}
			    			}
		    			}
		    		}
		    		
		    		// Print the view to the file
		    		out.print(targetViewSet.getImageFileName(nextViewTargetIndex).split("\\.")[0] + "\t" + targetBaselines[nextViewTargetIndex] + "\t" + targetSlopes[nextViewTargetIndex] + "\t" + targetPeaks[nextViewTargetIndex] + "\t" + 
	    					targetDistances[nextViewTargetIndex] + "\t" + targetErrors[nextViewTargetIndex] + "\t");
					
		    		// Flag that its been used
					targetUsed[nextViewTargetIndex] = true;
					originalUsed[nextViewOriginalIndex] = true;
					activeViewIndexList.set(activeViewCount, nextViewOriginalIndex);
					
					double expectedTotalError = 0.0;
	    			
					// Update all of the other target distances and errors that haven't been used yet
	    			for (int i = 0; i < targetViewSet.getCameraPoseCount(); i++) 
	    			{
    					// Don't update previously used views
	    				if (!targetUsed[i])
	    				{
	    					// distance
	    					targetDistances[i] = Math.min(targetDistances[i], 
	    							Math.acos(Math.max(-1.0, Math.min(1.0f, targetDirections[i].dot(targetDirections[nextViewTargetIndex])))));

	    					// error
	    					if (!fidelityTechnique.isGuaranteedMonotonic())
		    				{
		    					targetErrors[i] = estimateErrorQuadratic(targetBaselines[i], targetSlopes[i], targetPeaks[i], targetDistances[i]);
		    				}
	    					else
	    					{
	    						targetErrors[i] = estimateErrorFromSplines(Arrays.asList(viewDirections), Arrays.asList(errorFunctions), targetDirections[i], targetDistances[i]);
	    					}
	        				expectedTotalError += targetErrors[i];
	    				}
	    			}

		    		out.println(expectedTotalError + "\t" + minTotalError);
	    			
	    			// Count how many views from the original view set haven't been used.
	    			unusedOriginalViews = 0;
		    		for (int j = 0; j < resources.viewSet.getCameraPoseCount(); j++)
		    		{
		    			if (!originalUsed[j])
		    			{
		    				unusedOriginalViews++;
		    			}
		    		}
		    		
			        if (callback != null)
			        {
			        	callback.setProgress(activeViewIndexList.size());
			        }
	    		}

	    		// Views that are in the target view set and NOT in the original view set
    			int unused;
    			double maxError;
	    		do
	    		{
	    			unused = 0;
	    			maxError = -1.0;
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
		    			out.print(targetViewSet.getImageFileName(maxErrorIndex).split("\\.")[0] + "\t" + targetBaselines[maxErrorIndex] + "\t" + targetSlopes[maxErrorIndex] + "\t" + targetPeaks[maxErrorIndex] + "\t" + 
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
		    					if (!fidelityTechnique.isGuaranteedMonotonic())
			    				{
			    					targetErrors[i] = estimateErrorQuadratic(targetBaselines[i], targetSlopes[i], targetPeaks[i], targetDistances[i]);
			    				}
		    					else
		    					{
		    						targetErrors[i] = estimateErrorFromSplines(Arrays.asList(viewDirections), Arrays.asList(errorFunctions), targetDirections[i], targetDistances[i]);
		    					}
		        				cumError += targetErrors[i];
		    				}
		    			}
			    		
			    		out.println(cumError);
		    		}
	    		}
	    		while(maxError > 0.0 && unused > 0);
    		}
    	}
    	catch (Exception e) 
    	{
			e.printStackTrace();
		}
	}
}
