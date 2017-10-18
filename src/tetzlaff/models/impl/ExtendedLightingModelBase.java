package tetzlaff.models.impl;//Created by alexk on 7/21/2017.

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.models.*;

public abstract class ExtendedLightingModelBase<LightInstanceType extends LightInstanceModel> implements ExtendedLightingModel
{
    private boolean lightWidgetsEthereal;
    private final List<LightInstanceType> lightInstanceModels;
    private final LightWidgetModel[] lightWidgetModels;
    private final EnvironmentModel environmentModel;

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

    protected ExtendedLightingModelBase(int lightCount, IntFunction<LightInstanceType> lightInstanceCreator, EnvironmentModel environmentModel)
    {
        this.lightInstanceModels = new ArrayList<>(lightCount);
        this.lightWidgetModels = new LightWidgetModel[lightCount];
        for (int i = 0; i < lightCount; i++)
        {
            lightInstanceModels.add(lightInstanceCreator.apply(i));
            lightWidgetModels[i] = new LightWidgetModelImpl();
        }

        this.environmentModel = environmentModel;
    }

    @Override
    public final Vector3 getBackgroundColor()
    {
        return environmentModel.getBackgroundColor();
    }

    @Override
    public final void setBackgroundColor(Vector3 backgroundColor)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public BackgroundMode getBackgroundMode()
    {
        return environmentModel.getBackgroundMode();
    }

    @Override
    public void setBackgroundMode(BackgroundMode backgroundMode)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Vector3 getAmbientLightColor() 
    {
        return environmentModel.getEnvironmentColor();
    }

    @Override
    public final void setAmbientLightColor(Vector3 ambientLightColor)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEnvironmentMappingEnabled()
    {
        return environmentModel.isEnvironmentMappingEnabled();
    }

    @Override
    public void setEnvironmentMappingEnabled(boolean enabled)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix() 
    {
        return environmentModel.getEnvironmentMapMatrix();
    }

    @Override
    public void setEnvironmentMapMatrix(Matrix4 environmentMapMatrix)
    {
        throw new UnsupportedOperationException();
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

    @Override
    public boolean isLightVisualizationEnabled(int index)
    {
        return true;
    }

    @Override
    public Vector3 getLightColor(int i)
    {
        return lightInstanceModels.get(i).getColor();
    }

    @Override
    public Matrix4 getLightMatrix(int i)
    {
        return lightInstanceModels.get(i).getLookMatrix();
    }

    @Override
    public Vector3 getLightCenter(int i)
    {
        return lightInstanceModels.get(i).getTarget();
    }

    @Override
    public LightInstanceType getLight(int index)
    {
        return this.lightInstanceModels.get(index);
    }

    @Override
    public void setLightColor(int i, Vector3 lightColor)
    {
        if (this.isLightWidgetEnabled(i))
        {
            this.lightInstanceModels.get(i).setColor(lightColor);
        }
    }

    @Override
    public void setLightMatrix(int i, Matrix4 lightMatrix)
    {
        if (this.isLightWidgetEnabled(i))
        {
            this.lightInstanceModels.get(i).setLookMatrix(lightMatrix);
        }
    }

    @Override
    public void setLightCenter(int i, Vector3 lightCenter)
    {
        if (this.isLightWidgetEnabled(i))
        {
            this.lightInstanceModels.get(i).setTarget(lightCenter);
        }
    }

    @Override
    public EnvironmentModel getEnvironmentModel()
    {
        return this.environmentModel;
    }
}
