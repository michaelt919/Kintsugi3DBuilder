package tetzlaff.ibr.javafx.models;//Created by alexk on 7/28/2017.

import java.io.File;
import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import tetzlaff.gl.vecmath.Matrix4;
import tetzlaff.gl.vecmath.Vector3;
import tetzlaff.ibr.javafx.controllers.scene.environment_map.EVSetting;
import tetzlaff.models.ReadonlyEnvironmentMapModel;

public class JavaFXEnvironmentMapModel implements ReadonlyEnvironmentMapModel 
{
    private ObservableValue<EVSetting> selected;
    
    public JavaFXEnvironmentMapModel() 
    {
        super();
    }

    public void setSelected(ObservableValue<EVSetting> selected)
    {
        this.selected = selected;
        this.selected.addListener(settingChange);
    }
    
    private boolean selectedExists()
    {
        return (selected != null && selected.getValue() != null);
    }

    @Override
    public Vector3 getAmbientLightColor() 
    {
        if(selectedExists())
        {
            if(selected.getValue().isEvUseColor())
            {
                Color color = selected.getValue().getEvColor();
                return new Vector3((float) color.getRed(),(float) color.getBlue(),(float) color.getGreen()).times((float) selected.getValue().getEvColorIntensity());
            }
            else
            {
                if(selected.getValue().isEvUseImage())
                {
                    return new Vector3(1f);
                }
                else 
                {
                    return Vector3.ZERO;
                }
            }
        }
        else return Vector3.ZERO;
    }

    private final ChangeListener<File> evFileChange = (observable, oldFile, newFile) ->
    {
    	// TODO NEWUI don't reload if oldFile == newFile
        if(newFile != null)
        {
        	try 
            {
        		System.out.println("Loading environment map file " + newFile.getName());
        		JavaFXModels.getInstance().getLoadingModel().loadEnvironmentMap(newFile);
			} 
            catch (IOException e) 
            {
				e.printStackTrace();
			}
        	
            if(selectedExists()) 
        	{
            	selected.getValue().setFirstEVLoaded(true);
        	}
        }
    };

    private final ChangeListener<EVSetting> settingChange = (observable, oldSetting, newSetting) ->
    {
        if (newSetting != null) 
        {
            newSetting.evImageFileProperty().addListener(evFileChange);
            evFileChange.changed(null, null, newSetting.getEvImageFile());
        }
        
        if (oldSetting != null) 
        {
            oldSetting.evImageFileProperty().removeListener(evFileChange);
        }
    };

    @Override
    public boolean getEnvironmentMappingEnabled()
    {
        if(selectedExists())
        {
            return selected.getValue().isEvUseImage();
        }
        else 
    	{
        	return false;
    	}
    }

    @Override
    public Matrix4 getEnvironmentMapMatrix() 
    {
        if(selectedExists())
        {
            double azmuth = selected.getValue().getEvRotation();
            return Matrix4.rotateY(Math.toRadians(azmuth));
        }
        else 
        {
            return Matrix4.IDENTITY;
        }
    }
}
