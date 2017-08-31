package tetzlaff.mvc.old.models;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.LightWidgetModel;
import tetzlaff.models.ReadonlyCameraModel;

public class TrackballLightingModel extends LightingModelBase
{
    private int selectedLightIndex;
    private final boolean[] lightTrackballEnabled;
    private int trackballLightCount = 0;
    private final TrackballModel lightTrackballModel;
    private final TrackballModel[] trackballModels;
    private Matrix4 cameraPoseOverride;

    public TrackballLightingModel(int lightCount)
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

    private static class DummyLightWidgetModel implements LightWidgetModel
    {
        @Override
        public boolean areWidgetsEnabled()
        {
            return false;
        }

        @Override
        public boolean isAzimuthWidgetVisible()
        {
            return false;
        }

        @Override
        public boolean isAzimuthWidgetSelected()
        {
            return false;
        }

        @Override
        public boolean isInclinationWidgetVisible()
        {
            return false;
        }

        @Override
        public boolean isInclinationWidgetSelected()
        {
            return false;
        }

        @Override
        public boolean isDistanceWidgetVisible()
        {
            return false;
        }

        @Override
        public boolean isDistanceWidgetSelected()
        {
            return false;
        }

        @Override
        public boolean isCenterWidgetVisible()
        {
            return false;
        }

        @Override
        public boolean isCenterWidgetSelected()
        {
            return false;
        }

        @Override
        public void setWidgetsEnabled(boolean widgetsEnabled)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAzimuthWidgetVisible(boolean azimuthWidgetVisible)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAzimuthWidgetSelected(boolean azimuthWidgetSelected)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setInclinationWidgetVisible(boolean inclinationWidgetVisible)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setInclinationWidgetSelected(boolean inclinationWidgetSelected)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setDistanceWidgetVisible(boolean distanceWidgetVisible)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setDistanceWidgetSelected(boolean distanceWidgetSelected)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCenterWidgetVisible(boolean centerWidgetVisible)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCenterWidgetSelected(boolean centerWidgetSelected)
        {
            throw new UnsupportedOperationException();
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
    public boolean isLightVisualizationEnabled(int index)
    {
        return index != this.selectedLightIndex;
    }

    @Override
    public boolean areLightWidgetsEthereal()
    {
        return true;
    }

    @Override
    public LightWidgetModel getLightWidgetModel(int index)
    {
        return new DummyLightWidgetModel();
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
        return this.trackballModels[i].getTarget();
    }

    @Override
    public void setLightCenter(int i, Vector3 lightCenter)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLightWidgetsEthereal(boolean lightWidgetsEthereal)
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

    @Override
    public void setEnvironmentMapMatrix(Matrix4 environmentMapMatrix)
    {
        this.setLightMatrix(0, environmentMapMatrix);
    }
}
