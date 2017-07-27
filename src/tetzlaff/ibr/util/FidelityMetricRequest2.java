package tetzlaff.ibr.util;

import org.ejml.simple.SimpleMatrix;
import tetzlaff.gl.Context;
import tetzlaff.gl.vecmath.IntVector3;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.IBRSettings;
import tetzlaff.ibr.LoadingMonitor;
import tetzlaff.ibr.rendering.IBRResources;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class FidelityMetricRequest2 implements IBRRequest 
{
	private final static boolean DEBUG = false;
	
	private final static boolean USE_RENDERER_WEIGHTS = false;
	private final static boolean USE_PERCEPTUALLY_LINEAR_ERROR = false;
	
    private File fidelityExportPath;
    private File fidelityVSETFile;
    private IBRSettings settings;
    
	public FidelityMetricRequest2(File exportPath, File targetVSETFile, IBRSettings settings)
	{
		this.fidelityExportPath = exportPath;
		this.fidelityVSETFile = targetVSETFile;
		this.settings = settings;
	}
	
	private static class MatrixSystem
	{
		SimpleMatrix mA;
		SimpleMatrix b;
		List<Integer> activePixels;
	}
	
	private IBRResources<?> resources;

	private byte[][] images;
	private byte[][] weights;
	private float unitReflectanceEncoding;
	
	private MatrixSystem getMatrixSystem(int targetViewIndex, List<Integer> viewIndexList, Function<IntVector3, Vector3> decodeFunction)
	{
		MatrixSystem result = new MatrixSystem();
		
		result.activePixels = new ArrayList<Integer>();
        for (int i = 0; i < weights[targetViewIndex].length; i++)
        {
        	if ((0x000000FF & weights[targetViewIndex][i]) > 0)
        	{
        		result.activePixels.add(i);
        	}
        }
		
        result.mA = new SimpleMatrix(result.activePixels.size() * 3, viewIndexList == null ? images.length : viewIndexList.size());
        result.b = new SimpleMatrix(result.activePixels.size() * 3, 1);
		
		for (int i = 0; i < result.activePixels.size(); i++)
        {
        	double weight = (0x000000FF & weights[targetViewIndex][result.activePixels.get(i)]) / 255.0;
        	
        	Vector3 targetColor = decodeFunction.apply(new IntVector3(
        			0x000000FF & images[targetViewIndex][3 * result.activePixels.get(i)],
        			0x000000FF & images[targetViewIndex][3 * result.activePixels.get(i) + 1],
        			0x000000FF & images[targetViewIndex][3 * result.activePixels.get(i) + 2]));
        	
        	result.b.set(3 * i, weight * targetColor.x);
        	result.b.set(3 * i + 1, weight * targetColor.y);
        	result.b.set(3 * i + 2, weight * targetColor.z);
    		
        	if (viewIndexList == null)
        	{
	        	for (int j = 0; j < images.length; j++)
	        	{
	        		Vector3 color = decodeFunction.apply(new IntVector3(
	            			0x000000FF & images[j][3 * result.activePixels.get(i)],
	            			0x000000FF & images[j][3 * result.activePixels.get(i) + 1],
	            			0x000000FF & images[j][3 * result.activePixels.get(i) + 2]));
	        		
	            	result.mA.set(3 * i, j, weight * color.x);
	            	result.mA.set(3 * i + 1, j, weight * color.y);
	            	result.mA.set(3 * i + 2, j, weight * color.z);
	        	}
        	}
        	else
        	{
        		for (int j = 0; j < viewIndexList.size(); j++)
	        	{
	        		Vector3 color = decodeFunction.apply(new IntVector3(
	            			0x000000FF & images[viewIndexList.get(j)][3 * result.activePixels.get(i)],
	            			0x000000FF & images[viewIndexList.get(j)][3 * result.activePixels.get(i) + 1],
	            			0x000000FF & images[viewIndexList.get(j)][3 * result.activePixels.get(i) + 2]));
	        		
	            	result.mA.set(3 * i, j, weight * color.x);
	            	result.mA.set(3 * i + 1, j, weight * color.y);
	            	result.mA.set(3 * i + 2, j, weight * color.z);
	        	}
        	}
        }
		
		return result;
	}
	
	private float[] generateViewWeights(List<Integer> viewIndexList, int targetViewIndex)
	{
		float[] viewWeights = new float[resources.viewSet.getCameraPoseCount()];
		float viewWeightSum = 0.0f;
		
		for (int k = 0; k < viewIndexList.size(); k++)
		{
			int viewIndex = viewIndexList.get(k).intValue();
			
			Vector3 viewDir = resources.viewSet.getCameraPose(viewIndex).times(resources.geometry.getCentroid().asPosition()).getXYZ().negated().normalized();
			Vector3 targetDir = resources.viewSet.getCameraPose(viewIndex).times(
					resources.viewSet.getCameraPose(targetViewIndex).quickInverse(0.01f).getColumn(3)
						.minus(resources.geometry.getCentroid().asPosition())).getXYZ().normalized();
			
			viewWeights[viewIndex] = 1.0f / (float)Math.max(0.000001, 1.0 - Math.pow(Math.max(0.0, targetDir.dot(viewDir)), this.settings.getWeightExponent())) - 1.0f;
			viewWeightSum += viewWeights[viewIndex];
		}
		
		for (int i = 0; i < viewWeights.length; i++)
		{
			viewWeights[i] /= viewWeightSum;
		}
		
		return viewWeights;
	}
	
	private <ContextType extends Context<ContextType>> 
		double calculateHeuristicError(List<Integer> viewIndexList, int targetViewIndex)
	{
		float[] viewWeights = this.generateViewWeights(viewIndexList, targetViewIndex);
		
		// Alternate error calculation method that should give the same result in theory
		MatrixSystem system = getMatrixSystem(targetViewIndex, viewIndexList,
		encodedVector -> new Vector3(
				encodedVector.x / unitReflectanceEncoding,
				encodedVector.y / unitReflectanceEncoding,
				encodedVector.z / unitReflectanceEncoding));
		
		SimpleMatrix weightVector = new SimpleMatrix(viewIndexList.size(), 1);
		for (int i = 0; i < viewIndexList.size(); i++)
		{
			int viewIndex = viewIndexList.get(i).intValue();
			weightVector.set(i, viewWeights[viewIndex]);
		}
		
	    SimpleMatrix recon = system.mA.mult(weightVector);
	    SimpleMatrix error = recon.minus(system.b);
		return error.normF() / Math.sqrt(system.b.numRows() / 3);
	}

	@Override
	public <ContextType extends Context<ContextType>> void executeRequest(
			ContextType context, IBRRenderable<ContextType> renderable,
			LoadingMonitor callback) throws Exception 
	{
		// TODO Auto-generated method stub
		
	}
}
