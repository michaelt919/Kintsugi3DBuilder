package tetzlaff.models.impl;//Created by alexk on 7/21/2017.

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.EnvironmentMapModel;
import tetzlaff.models.ExtendedLightingModel;
import tetzlaff.models.LightWidgetModel;

public abstract class LightingModelBase implements ExtendedLightingModel
{
    private boolean lightWidgetsEthereal;
    private final LightWidgetModel[] lightWidgetModels;
    private final EnvironmentMapModel environmentMapModel;

    private static class LightWidgetModelImpl implements LightWidgetModel
    {
        private boolean widgetsEnabled = true;
        private boolean azimuthWidgetVisible = true;
        private boolean azimuthWidgetSelected = false;
        private boolean inclinationWidgetVisible = true;
        private boolean inclinationWidgetSelected = false;
        private boolean distanceWidgetVisible = true;
        private boolean distanceWidgetSelected = false;
        private boolean centerWidgetVisible = true;
        private boolean centerWidgetSelected = false;

        @Override
        public boolean areWidgetsEnabled()
        {
            return widgetsEnabled;
        }

        @Override
        public boolean isAzimuthWidgetVisible()
        {
            return azimuthWidgetVisible;
        }

        @Override
        public boolean isAzimuthWidgetSelected()
        {
            return azimuthWidgetSelected;
        }

        @Override
        public boolean isInclinationWidgetVisible()
        {
            return inclinationWidgetVisible;
        }

        @Override
        public boolean isInclinationWidgetSelected()
        {
            return inclinationWidgetSelected;
        }

        @Override
        public boolean isDistanceWidgetVisible()
        {
            return distanceWidgetVisible;
        }

        @Override
        public boolean isDistanceWidgetSelected()
        {
            return distanceWidgetSelected;
        }

        @Override
        public boolean isCenterWidgetVisible()
        {
            return centerWidgetVisible;
        }

        @Override
        public boolean isCenterWidgetSelected()
        {
            return centerWidgetSelected;
        }

        @Override
        public void setWidgetsEnabled(boolean widgetsEnabled)
        {
            this.widgetsEnabled = widgetsEnabled;
        }

        @Override
        public void setAzimuthWidgetVisible(boolean azimuthWidgetVisible)
        {
            this.azimuthWidgetVisible = azimuthWidgetVisible;
        }

        @Override
        public void setAzimuthWidgetSelected(boolean azimuthWidgetSelected)
        {
            this.azimuthWidgetSelected = azimuthWidgetSelected;
        }

        @Override
        public void setInclinationWidgetVisible(boolean inclinationWidgetVisible)
        {
            this.inclinationWidgetVisible = inclinationWidgetVisible;
        }

        @Override
        public void setInclinationWidgetSelected(boolean inclinationWidgetSelected)
        {
            this.inclinationWidgetSelected = inclinationWidgetSelected;
        }

        @Override
        public void setDistanceWidgetVisible(boolean distanceWidgetVisible)
        {
            this.distanceWidgetVisible = distanceWidgetVisible;
        }

        @Override
        public void setDistanceWidgetSelected(boolean distanceWidgetSelected)
        {
            this.distanceWidgetSelected = distanceWidgetSelected;
        }

        @Override
        public void setCenterWidgetVisible(boolean centerWidgetVisible)
        {
            this.centerWidgetVisible = centerWidgetVisible;
        }

        @Override
        public void setCenterWidgetSelected(boolean centerWidgetSelected)
        {
            this.centerWidgetSelected = centerWidgetSelected;
        }
    }

    protected LightingModelBase(int lightCount, EnvironmentMapModel environmentMapModel)
    {
        this.lightWidgetModels = new LightWidgetModel[lightCount];
        for (int i = 0; i < lightCount; i++)
        {
            lightWidgetModels[i] = new LightWidgetModelImpl();
        }

        this.environmentMapModel = environmentMapModel;
    }

    @Override
    public final Vector3 getAmbientLightColor() 
    {
        return environmentMapModel.getAmbientLightColor();
    }

    @Override
    public final void setAmbientLightColor(Vector3 ambientLightColor)
    {
        environmentMapModel.setAmbientLightColor(ambientLightColor);
    }

    @Override
    public boolean isEnvironmentMappingEnabled()
    {
        return environmentMapModel.isEnvironmentMappingEnabled();
    }

    @Override
    public void setEnvironmentMappingEnabled(boolean environmentMappingEnabled)
    {
        environmentMapModel.setEnvironmentMappingEnabled(environmentMappingEnabled);
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix() 
    {
        return environmentMapModel.getEnvironmentMapMatrix();
    }

    @Override
    public void setEnvironmentMapMatrix(Matrix4 environmentMapMatrix)
    {
        environmentMapModel.setEnvironmentMapMatrix(environmentMapMatrix);
    }

    @Override
    public boolean areLightWidgetsEthereal()
    {
        return this.lightWidgetsEthereal;
    }

    @Override
    public void setLightWidgetsEthereal(boolean lightWidgetsEthereal)
    {
        this.lightWidgetsEthereal = lightWidgetsEthereal;
    }

    @Override
    public LightWidgetModel getLightWidgetModel(int index)
    {
        return this.lightWidgetModels[index];
    }
}
