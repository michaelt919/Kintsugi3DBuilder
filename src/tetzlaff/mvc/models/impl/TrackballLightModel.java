package tetzlaff.mvc.models.impl;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.mvc.models.LightModel;
import tetzlaff.mvc.models.ReadonlyCameraModel;

public class TrackballLightModel extends LightModelBase implements LightModel
{
	private int selectedLightIndex;
	private boolean[] lightTrackballEnabled;
	private int trackballLightCount = 0;
	private TrackballModel lightTrackballModel;
	private TrackballModel[] trackballModels;
	private Matrix4 cameraPoseOverride;

	public TrackballLightModel(int lightCount) 
	{
		super(lightCount);
		
		this.selectedLightIndex = 0;
		
		this.lightTrackballEnabled = new boolean[lightCount];
    	this.trackballModels = new TrackballModel[lightCount];
    	this.lightTrackballModel = new TrackballModel();
    	
    	this.setLightColor(0, new Vector3(1.0f, 1.0f, 1.0f));
    	
    	for (int i = 0; i < lightCount; i++)
    	{
    		TrackballModel newTrackball = new TrackballModel();
    		trackballModels[i] = newTrackball;
    		lightTrackballEnabled[i] = false;
    	}
	}
	
	public ReadonlyCameraModel asCameraModel()
	{
		return () -> cameraPoseOverride != null ? cameraPoseOverride : trackballModels[this.selectedLightIndex].getLookMatrix();
	}
	
	public TrackballModel getTrackballModel(int i)
	{
		return this.trackballModels[i];
	}

	public int getSelectedLightIndex() 
	{
		return this.selectedLightIndex;
	}

	public void setSelectedLightIndex(int index) 
	{
		this.selectedLightIndex = index;
	}
	
	public TrackballModel getLightTrackballModel()
	{
		return this.lightTrackballModel;
	}
	
	public int getTrackballLightCount()
	{
		return this.trackballLightCount;
	}
	
	public void enableLightTrackball(int index)
	{
		if (!lightTrackballEnabled[index])
		{
			lightTrackballEnabled[index] = true;
			trackballLightCount++;
			
			if (trackballLightCount == 1)
			{
				// no lights controlled -> one light controlled
				lightTrackballModel.setTrackballMatrix(Matrix4.IDENTITY);
				lightTrackballModel.setScale(1.0f);
			}
		}
	}
	
	public void disableLightTrackball(int index)
	{
		if (lightTrackballEnabled[index])
		{
			lightTrackballEnabled[index] = false;
			trackballLightCount--;
			
			this.trackballModels[index].setTrackballMatrix(
					this.trackballModels[index].getTrackballMatrix()
					.times(this.trackballModels[this.selectedLightIndex].getTrackballMatrix().quickInverse(0.001f))
					.times(lightTrackballModel.getTrackballMatrix())
					.times(this.trackballModels[this.selectedLightIndex].getTrackballMatrix()));
		}
	}

	@Override
	public boolean isLightVisualizationEnabled(int i) 
	{
		return i != this.selectedLightIndex;
	}
	
	@Override
	public boolean isLightWidgetEnabled(int i) 
	{
		return i == 0;
	}

	@Override
	public Matrix4 getLightMatrix(int i) 
	{
		if (lightTrackballEnabled[i])
		{
			return trackballModels[i].getLookMatrix()
					//.times(trackballs.get(i).getTrackballMatrix().quickInverse(0.001f))
					.times(trackballModels[selectedLightIndex].getTrackballMatrix().quickInverse(0.001f))
					.times(lightTrackballModel.getTrackballMatrix())
					.times(trackballModels[selectedLightIndex].getTrackballMatrix())
					;//.times(trackballs.get(i).getTrackballMatrix());
		}
		else
		{
			return this.trackballModels[i].getLookMatrix();
		}
	}

	@Override
	public void setLightMatrix(int i, Matrix4 lightMatrix) 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Vector3 getLightCenter(int i) 
	{
		return this.trackballModels[i].getCenter();
	}

	@Override
	public void setLightCenter(int i, Vector3 lightTargetPoint) 
	{
		throw new UnsupportedOperationException();
	}

	public void overrideCameraPose(Matrix4 cameraPoseOverride) 
	{
		this.cameraPoseOverride = cameraPoseOverride;
	}

	public void removeCameraPoseOverride()
	{
		this.cameraPoseOverride = null;
	}

	@Override
	public Matrix4 getEnvironmentMapMatrix() 
	{
		return getLightMatrix(0);
	}
}
