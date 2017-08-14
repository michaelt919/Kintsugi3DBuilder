package tetzlaff.mvc.controllers.impl;

import tetzlaff.gl.window.KeyCodes;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.listeners.KeyPressListener;
import tetzlaff.gl.window.listeners.KeyReleaseListener;
import tetzlaff.mvc.controllers.LightController;
import tetzlaff.mvc.models.ReadonlyCameraModel;
import tetzlaff.mvc.models.old.TrackballLightingModel;

public class TrackballLightController implements LightController, KeyPressListener, KeyReleaseListener
{
	private TrackballLightingModel model;
	private TrackballController lightControlTrackball;
	private TrackballController[] trackballs;
	
	public TrackballLightController()
	{
		this(4);
	}
	
	public TrackballLightController(int lightCount)
	{
    	this.model = new TrackballLightingModel(lightCount);
    	
    	this.lightControlTrackball = TrackballController.getBuilder()
    			.setSensitivity(1.0f)
    			.setPrimaryButtonIndex(0)
    			.setSecondaryButtonIndex(1)
    			.setModel(this.model.getLightTrackballModel())
    			.create();
    	this.lightControlTrackball.setEnabled(false);
    	this.lightControlTrackball.setInverted(true);
    	
    	this.trackballs = new TrackballController[lightCount];
    	
    	for (int i = 0; i < lightCount; i++)
    	{
    		TrackballController newTrackball = TrackballController.getBuilder()
		    		.setSensitivity(1.0f)
					.setPrimaryButtonIndex(0)
					.setSecondaryButtonIndex(1)
					.setModel(this.model.getTrackballModel(i))
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
		
		for (TrackballController t : trackballs)
		{
			t.addAsWindowListener(window);
		}
	}
	
	@Override
	public TrackballLightingModel getLightingModel()
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
