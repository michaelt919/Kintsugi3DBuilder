package tetzlaff.mvc.controllers;

import java.util.List;

import tetzlaff.mvc.models.ReadonlyLightModel;
import tetzlaff.mvc.models.TrackballLightModel;
import tetzlaff.window.KeyCodes;
import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;
import tetzlaff.window.listeners.KeyPressListener;
import tetzlaff.window.listeners.KeyReleaseListener;

public class TrackballLightController implements LightController, KeyPressListener, KeyReleaseListener
{
	private TrackballLightModel model;
	private TrackballController lightControlTrackball;
	private List<TrackballController> trackballs;
	
	public TrackballLightController()
	{
		this(4);
	}
	
	public TrackballLightController(int lightCount)
	{
    	this.lightControlTrackball = TrackballController.getBuilder()
    			.setSensitivity(1.0f)
    			.setPrimaryButtonIndex(0)
    			.setSecondaryButtonIndex(1)
    			.setModel(this.model.getLightTrackballModel())
    			.create();
    	this.lightControlTrackball.setEnabled(false);
    	this.lightControlTrackball.setInverted(true);
    	
    	this.model = new TrackballLightModel(lightCount);
    	
    	for (int i = 0; i < lightCount; i++)
    	{
    		TrackballController newTrackball = TrackballController.getBuilder()
		    		.setSensitivity(1.0f)
					.setPrimaryButtonIndex(0)
					.setSecondaryButtonIndex(1)
					.setModel(this.model.getTrackballModel(i))
					.create();
    		trackballs.add(newTrackball);
    		
    		if (i != 0)
    		{
    			newTrackball.setEnabled(false);
    		}
    	}
	}
	
	@Override
	public void addAsWindowListener(Window window)
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
	public ReadonlyLightModel getModel()
	{
		return this.model;
	}

	@Override
	public void keyPressed(Window window, int keycode, ModifierKeys mods) 
	{
		if (keycode >= KeyCodes.ONE && keycode <= KeyCodes.FOUR)
		{
			int selection = keycode - KeyCodes.ONE;

			if (mods.getAltModifier())
			{
				if (selection < trackballs.size() && window.getModifierKeys().getAltModifier())
				{
					this.trackballs.get(this.model.getSelectedLightIndex()).setEnabled(false);
					this.model.setSelectedLightIndex(selection);
					this.trackballs.get(this.model.getSelectedLightIndex()).setEnabled(true);
				}
			}
			else if (selection < model.getLightCount() && selection != model.getSelectedLightIndex())
			{
				model.enableLightTrackball(selection);
				lightControlTrackball.setEnabled(true);
				trackballs.get(model.getSelectedLightIndex()).setEnabled(false);
			}
		}
	}

	@Override
	public void keyReleased(Window window, int keycode, ModifierKeys mods) 
	{
		if (keycode >= KeyCodes.ONE && keycode <= KeyCodes.FOUR)
		{
			int selection = keycode - KeyCodes.ONE;
			
			if (selection < trackballs.size())
			{
				model.disableLightTrackball(selection);
				
				if (model.getTrackballLightCount() == 0)
				{
					trackballs.get(this.model.getSelectedLightIndex()).setEnabled(true);
					lightControlTrackball.setEnabled(false);
				}
			}
		}
	}
}
