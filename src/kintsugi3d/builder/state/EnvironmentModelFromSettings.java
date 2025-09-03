package kintsugi3d.builder.state;

import javafx.scene.paint.Color;
import kintsugi3d.builder.core.Global;
import kintsugi3d.builder.state.project.EnvironmentSettings;
import kintsugi3d.gl.vecmath.Matrix4;
import kintsugi3d.gl.vecmath.Vector3;
import kintsugi3d.util.EncodableColorImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;

public abstract class EnvironmentModelFromSettings implements EnvironmentModel
{
    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentModelFromSettings.class);

    protected long lastEnvironmentMapTime;
    protected long lastBackplateTime;
    private boolean environmentMapLoaded = false;
    private boolean backplateLoaded = false;

    protected abstract EnvironmentSettings getEnvironmentSettings();

    protected abstract void onEnvironmentMapImageLoaded(EncodableColorImage environmentMapImage);

    @Override
    public Vector3 getEnvironmentColor()
    {
        if (isEnabled())
        {
            EnvironmentSettings selectedEnvironment = getEnvironmentSettings();
            if (selectedEnvironment.isEnvironmentColorEnabled() && (environmentMapLoaded || !selectedEnvironment.isEnvironmentImageEnabled()))
            {
                Color color = selectedEnvironment.getEnvironmentColor();
                return new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue())
                    .times((float) selectedEnvironment.getEnvironmentColorIntensity());
            }
            else if (selectedEnvironment.isEnvironmentImageEnabled() && environmentMapLoaded)
            {
                return new Vector3((float) selectedEnvironment.getEnvironmentColorIntensity());
            }
            else
            {
                return Vector3.ZERO;
            }
        }
        else
        {
            return Vector3.ZERO;
        }
    }

    @Override
    public Vector3 getBackgroundColor()
    {
        if (isEnabled())
        {
            EnvironmentSettings selectedEnvironment = getEnvironmentSettings();

            if (selectedEnvironment.isBackplateColorEnabled() && (backplateLoaded || !selectedEnvironment.isBackplateImageEnabled()))
            {
                Color color = selectedEnvironment.getBackplateColor();
                return new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue())
                    .times((float) selectedEnvironment.getBackgroundIntensity());
            }
            else if (selectedEnvironment.isBackplateImageEnabled() && backplateLoaded)
            {
                return new Vector3((float) selectedEnvironment.getBackgroundIntensity());
            }
            else if (selectedEnvironment.isEnvironmentColorEnabled() && (environmentMapLoaded || !selectedEnvironment.isEnvironmentImageEnabled()))
            {
                Color color = selectedEnvironment.getEnvironmentColor();
                return new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue())
                    .times((float)(selectedEnvironment.getBackgroundIntensity() * selectedEnvironment.getEnvironmentColorIntensity()));
            }
            else if (selectedEnvironment.isEnvironmentImageEnabled() && environmentMapLoaded)
            {
                return new Vector3((float)(selectedEnvironment.getBackgroundIntensity() * selectedEnvironment.getEnvironmentColorIntensity()));
            }
            else
            {
                return Vector3.ZERO;
            }
        }
        else
        {
            return Vector3.ZERO;
        }
    }

    public void loadEnvironmentMap(File newFile)
    {
        environmentMapLoaded = false;
        long lastModified = newFile.lastModified();

        new Thread(() ->
        {
            try
            {
                LOG.info("Loading environment map file " + newFile.getName());
                Optional<EncodableColorImage> environmentMapImage = Global.state().getIOModel().loadEnvironmentMap(newFile);
                onEnvironmentMapImageLoaded(environmentMapImage.orElse(null));
            }
            catch (FileNotFoundException e)
            {
                LOG.error("Failed to find environment map file '{}':", newFile.getName(), e);
            }

            if (isEnabled())
            {
                getEnvironmentSettings().setEnvironmentLoaded(true);
            }

            environmentMapLoaded = true;
            lastEnvironmentMapTime = lastModified;

        }).start();
    }

    public void loadBackplate(File newFile)
    {
        backplateLoaded = false;
        lastBackplateTime = newFile.lastModified();
        backplateLoaded = true;

        new Thread(() ->
        {
            try
            {
                LOG.info("Loading backplate file " + newFile.getName());
                Global.state().getIOModel().loadBackplate(newFile);
            }
            catch (FileNotFoundException e)
            {
                LOG.error("Failed to find backplate file '{}':", newFile.getName(), e);
            }

            if (isEnabled())
            {
                getEnvironmentSettings().setBackplateLoaded(true);
            }
        })
        .start();
    }

    @Override
    public BackgroundMode getBackgroundMode()
    {
        EnvironmentSettings selectedEnvironment = getEnvironmentSettings();
        if (this.backplateLoaded && isEnabled() && selectedEnvironment.isBackplateImageEnabled())
        {
            return BackgroundMode.IMAGE;
        }
        else if (selectedEnvironment.isBackplateColorEnabled())
        {
            return BackgroundMode.COLOR;
        }
        else if (isEnvironmentMappingEnabled())
        {
            return BackgroundMode.ENVIRONMENT_MAP;
        }
        else
        {
            return BackgroundMode.NONE;
        }
    }

    @Override
    public boolean isEnvironmentMappingEnabled()
    {
        return this.environmentMapLoaded && isEnabled() && getEnvironmentSettings().isEnvironmentImageEnabled();
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix()
    {
        if (isEnabled())
        {
            double azimuth = getEnvironmentSettings().getEnvironmentRotation();
            return Matrix4.rotateY(Math.toRadians(azimuth));
        }
        else
        {
            return Matrix4.IDENTITY;
        }
    }

    @Override
    public float getEnvironmentMapFilteringBias()
    {
        return isEnabled() ? (float)getEnvironmentSettings().getEnvironmentFilteringBias() : 0;
    }

    @Override
    public float getEnvironmentRotation()
    {
        return isEnabled() ? (float)(getEnvironmentSettings().getEnvironmentRotation() * Math.PI / 180) : 0.0f;
    }

    @Override
    public void setEnvironmentRotation(float environmentRotation)
    {
        if (isEnabled() && !getEnvironmentSettings().isLocked())
        {
            getEnvironmentSettings().setEnvironmentRotation(environmentRotation * 180 / Math.PI);
        }
    }

    @Override
    public float getEnvironmentIntensity()
    {
        return isEnabled() ? (float)getEnvironmentSettings().getEnvironmentColorIntensity() : 0.0f;
    }

    @Override
    public void setEnvironmentIntensity(float environmentIntensity)
    {
        if (isEnabled() && !getEnvironmentSettings().isLocked())
        {
            getEnvironmentSettings().setEnvironmentIntensity(environmentIntensity);
        }
    }

    @Override
    public float getBackgroundIntensity()
    {
        return isEnabled() ? (float)getEnvironmentSettings().getBackgroundIntensity() : 0.0f;
    }

    @Override
    public void setBackgroundIntensity(float backgroundIntensity)
    {
        if (isEnabled() && !getEnvironmentSettings().isLocked())
        {
            getEnvironmentSettings().setBackgroundIntensity(backgroundIntensity);
        }
    }

    @Override
    public boolean isGroundPlaneEnabled()
    {
        return isEnabled() && getEnvironmentSettings().isGroundPlaneEnabled();
    }

    @Override
    public Vector3 getGroundPlaneColor()
    {
        if (isEnabled())
        {
            Color color = getEnvironmentSettings().getGroundPlaneCrolor();
            return new Vector3((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue());
        }
        else
        {
            return new Vector3(1);
        }

    }

    @Override
    public float getGroundPlaneHeight()
    {
        return isEnabled() ? (float)getEnvironmentSettings().getGroundPlaneHeight() : 0.0f;
    }

    @Override
    public float getGroundPlaneSize()
    {
        return isEnabled() ? (float)getEnvironmentSettings().getGroundPlaneSize() : 1.0f;
    }
}
