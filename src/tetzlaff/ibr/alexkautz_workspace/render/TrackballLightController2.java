package tetzlaff.ibr.alexkautz_workspace.render;

import tetzlaff.gl.window.KeyCodes;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.listeners.KeyPressListener;
import tetzlaff.gl.window.listeners.KeyReleaseListener;
import tetzlaff.mvc.controllers.LightController;
import tetzlaff.ibr.alexkautz_workspace.render.*;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.impl.TrackballLightModel;

public class TrackballLightController2 implements LightController, KeyPressListener, KeyReleaseListener
{
	private TrackballLightModel2 model;
	private TrackballController2 lightControlTrackball;
	private TrackballController2[] trackballs;

	public TrackballLightController2()
	{
		this(4);
	}

	public TrackballLightController2(int lightCount)
	{
    	this.model = new TrackballLightModel2(lightCount);
    	
    	this.lightControlTrackball = TrackballController2.getBuilder()
    			.setSensitivity(1.0f)
    			.setPrimaryButtonIndex(0)
    			.setSecondaryButtonIndex(1)
    			.setModel(this.model.getLightTrackballModel2())
    			.create();
    	this.lightControlTrackball.setEnabled(false);
    	this.lightControlTrackball.setInverted(true);
    	
    	this.trackballs = new TrackballController2[lightCount];
    	
    	for (int i = 0; i < lightCount; i++)
    	{
    		TrackballController2 newTrackball = TrackballController2.getBuilder()
		    		.setSensitivity(1.0f)
					.setPrimaryButtonIndex(0)
					.setSecondaryButtonIndex(1)
					.setModel(this.model.getTrackballModel2(i))
					.create();
    		trackballs[i] = newTrackball;
    		newTrackball.setEnabled(i == 0);
    	}
	}
	
	@Override
	public void addAsWindowListener(Window<?> window)
	{
		window.addKeyPressListener(this);
		window.addKeyReleaseListener(this);
		
		lightControlTrackball.addAsWindowListener(window);
		
		for (TrackballController2 t : trackballs)
		{
			//t.addAsWindowListener(window);
		}
	}
	
	@Override
	public TrackballLightModel2 getLightModel()
	{
		return this.model;
	}
	
	public ReadonlyCameraModel getCurrentCameraModel()
	{
		return this.model.asCameraModel();
	}

	@Override
	public void keyPressed(Window<?> window, int keycode, ModifierKeys mods) 
	{
		if (keycode >= KeyCodes.ONE && keycode <= KeyCodes.FOUR)
		{
			int selection = keycode - KeyCodes.ONE;

			if (mods.getAltModifier())
			{
				if (selection < trackballs.length && window.getModifierKeys().getAltModifier())
				{
					this.trackballs[this.model.getSelectedLightIndex()].setEnabled(false);
					this.model.setSelectedLightIndex(selection);
					this.trackballs[this.model.getSelectedLightIndex()].setEnabled(true);
				}
			}
			else if (selection < model.getLightCount() && selection != model.getSelectedLightIndex())
			{
				model.enableLightTrackball(selection);
				lightControlTrackball.setEnabled(true);
				trackballs[model.getSelectedLightIndex()].setEnabled(false);
			}
		}
	}

	@Override
	public void keyReleased(Window<?> window, int keycode, ModifierKeys mods) 
	{
		if (keycode >= KeyCodes.ONE && keycode <= KeyCodes.FOUR)
		{
			int selection = keycode - KeyCodes.ONE;
			
			if (selection < trackballs.length)
			{
				model.disableLightTrackball(selection);
				
				if (model.getTrackballLightCount() == 0)
				{
					trackballs[this.model.getSelectedLightIndex()].setEnabled(true);
					lightControlTrackball.setEnabled(false);
				}
			}
		}
	}
}
