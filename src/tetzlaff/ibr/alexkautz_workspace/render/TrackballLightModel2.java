package tetzlaff.ibr.alexkautz_workspace.render;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.mvc.models.LightModel;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.impl.LightModelBase;

public class TrackballLightModel2 extends LightModelBase implements LightModel
{
	private int selectedLightIndex;
	private boolean[] lightTrackballEnabled;
	private int trackballLightCount = 0;
	private TrackballModel2 lightTrackballModel2;
	private TrackballModel2[] TrackballModel2s;
	private Matrix4 cameraPoseOverride;

	public TrackballLightModel2(int lightCount)
	{
		super(lightCount);
		
		this.selectedLightIndex = 0;
		
		this.lightTrackballEnabled = new boolean[lightCount];
    	this.TrackballModel2s = new TrackballModel2[lightCount];
    	this.lightTrackballModel2 = new TrackballModel2();
    	
    	this.setLightColor(0, new Vector3(1.0f, 1.0f, 1.0f));
    	
    	for (int i = 0; i < lightCount; i++)
    	{
    		TrackballModel2 newTrackball = new TrackballModel2();
    		TrackballModel2s[i] = newTrackball;
    		lightTrackballEnabled[i] = false;
    	}
	}
	
	public ReadonlyCameraModel asCameraModel()
	{
		return () -> cameraPoseOverride != null ? cameraPoseOverride : TrackballModel2s[this.selectedLightIndex].getLookMatrix();
	}
	
	public TrackballModel2 getTrackballModel2(int i)
	{
		return this.TrackballModel2s[i];
	}

	public int getSelectedLightIndex() 
	{
		return this.selectedLightIndex;
	}

	public void setSelectedLightIndex(int index) 
	{
		this.selectedLightIndex = index;
	}
	
	public TrackballModel2 getLightTrackballModel2()
	{
		return this.lightTrackballModel2;
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
				lightTrackballModel2.setTrackballMatrix(Matrix4.IDENTITY);
				lightTrackballModel2.setScale(1.0f);
			}
		}
	}
	
	public void disableLightTrackball(int index)
	{
		if (lightTrackballEnabled[index])
		{
			lightTrackballEnabled[index] = false;
			trackballLightCount--;
			
			this.TrackballModel2s[index].setTrackballMatrix(
					this.TrackballModel2s[index].getTrackballMatrix()
					.times(this.TrackballModel2s[this.selectedLightIndex].getTrackballMatrix().quickInverse(0.001f))
					.times(lightTrackballModel2.getTrackballMatrix())
					.times(this.TrackballModel2s[this.selectedLightIndex].getTrackballMatrix()));
		}
	}

	@Override
	public boolean isLightVisualizationEnabled(int i) 
	{
		//return i != this.selectedLightIndex;
		return true;
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
			return TrackballModel2s[i].getLookMatrix()
					//.times(trackballs.get(i).getTrackballMatrix().quickInverse(0.001f))
					.times(TrackballModel2s[selectedLightIndex].getTrackballMatrix().quickInverse(0.001f))
					.times(lightTrackballModel2.getTrackballMatrix())
					.times(TrackballModel2s[selectedLightIndex].getTrackballMatrix())
					;//.times(trackballs.get(i).getTrackballMatrix());
		}
		else
		{
			return this.TrackballModel2s[i].getLookMatrix();
		}
	}

	@Override
	public void setLightMatrix(int i, Matrix4 lightMatrix) 
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
	public Vector3 getLightCenter(int i) 
	{
		return this.TrackballModel2s[i].getCenter();
	}

	@Override
	public void setLightCenter(int i, Vector3 lightTargetPoint) 
	{
		throw new UnsupportedOperationException();
	}
}
