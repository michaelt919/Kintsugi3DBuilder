package tetzlaff.imagebasedmicrofacet;

import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.LightController;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.gl.helpers.Vector4;
import tetzlaff.window.KeyCodes;
import tetzlaff.window.ModifierKeys;
import tetzlaff.window.Window;
import tetzlaff.window.listeners.CharacterListener;
import tetzlaff.window.listeners.KeyPressListener;
import tetzlaff.window.listeners.KeyReleaseListener;

public class TrackballLightController implements LightController, CharacterListener, KeyPressListener, KeyReleaseListener
{
	private int activeTrackball;
	private List<Vector3> lightColors;
	private List<Trackball> trackballs;
	private List<Boolean> lightControls;
	private int lightsControlled = 0;
	private Trackball lightControlTrackball;
	
	public TrackballLightController()
	{
		this.activeTrackball = 0;
		
		this.lightColors = new ArrayList<Vector3>(8);
    	this.trackballs = new ArrayList<Trackball>(8);
    	this.lightControls = new ArrayList<Boolean>(8);
    	
    	this.lightControlTrackball = new Trackball(1.0f, 0, 1, true);
    	this.lightControlTrackball.setEnabled(false);
    	this.lightControlTrackball.setInverted(true);
    	
    	for (int i = 0; i < 4; i++)
    	{
    		Trackball newTrackball = new Trackball(1.0f, 0, 1, true);
    		trackballs.add(newTrackball);
    		
    		if (i == 0)
    		{
    			lightColors.add(new Vector3(1.0f, 1.0f, 1.0f));
    		}
    		else
    		{
    			lightColors.add(new Vector3(0.0f, 0.0f, 0.0f));
    			newTrackball.setEnabled(false);
    		}
    		
    		lightControls.add(false);
    	}
	}
	
	public CameraController asCameraController()
	{
		return () -> trackballs.get(this.activeTrackball).getViewMatrix();
	}
	
	public void addAsWindowListener(Window window)
	{
		window.addCharacterListener(this);
		window.addKeyPressListener(this);
		window.addKeyReleaseListener(this);
		
		lightControlTrackball.addAsWindowListener(window);
		
		for (Trackball t : trackballs)
		{
			t.addAsWindowListener(window);
		}
	}

	@Override
	public void characterTyped(Window window, char c) 
	{
		switch(c)
		{
		case 't':
			this.lightColors.set(activeTrackball, this.lightColors.get(activeTrackball).plus(new Vector3(1.0f / 32.0f, 0.0f, 0.0f)));
			break;
		case 'r':
			this.lightColors.set(activeTrackball, this.lightColors.get(activeTrackball).plus(new Vector3(-1.0f / 32.0f, 0.0f, 0.0f)));
			if (this.lightColors.get(activeTrackball).x < 0.0f)
			{
				this.lightColors.set(activeTrackball, new Vector3(0.0f, this.lightColors.get(activeTrackball).y, this.lightColors.get(activeTrackball).z));
			}
			break;
		case 'g':
			this.lightColors.set(activeTrackball, this.lightColors.get(activeTrackball).plus(new Vector3(0.0f, 1.0f / 32.0f, 0.0f)));
			break;
		case 'f':
			this.lightColors.set(activeTrackball, this.lightColors.get(activeTrackball).plus(new Vector3(0.0f, -1.0f / 32.0f, 0.0f)));
			if (this.lightColors.get(activeTrackball).y < 0.0f)
			{
				this.lightColors.set(activeTrackball, new Vector3(this.lightColors.get(activeTrackball).x, 0.0f, this.lightColors.get(activeTrackball).z));
			}
			break;
		case 'b':
			this.lightColors.set(activeTrackball, this.lightColors.get(activeTrackball).plus(new Vector3(0.0f, 0.0f, 1.0f / 32.0f)));
			break;
		case 'v':
			this.lightColors.set(activeTrackball, this.lightColors.get(activeTrackball).plus(new Vector3(0.0f, 0.0f, -1.0f / 32.0f)));
			if (this.lightColors.get(activeTrackball).z < 0.0f)
			{
				this.lightColors.set(activeTrackball, new Vector3(this.lightColors.get(activeTrackball).x, this.lightColors.get(activeTrackball).y, 0.0f));
			}
			break;
		}
	}

	@Override
	public void keyPressed(Window window, int keycode, ModifierKeys mods) 
	{
		if (keycode >= KeyCodes.ZERO && keycode <= KeyCodes.SEVEN)
		{
			int selection = keycode - KeyCodes.ZERO;

			if (mods.getAltModifier())
			{
				if (selection < trackballs.size() && window.getModifierKeys().getAltModifier())
				{
					this.trackballs.get(this.activeTrackball).setEnabled(false);
					this.activeTrackball = selection;
					this.trackballs.get(this.activeTrackball).setEnabled(true);
				}
			}
			else if (selection != activeTrackball && !lightControls.get(selection))
			{
				lightControls.set(selection, true);
				lightsControlled++;
				
				trackballs.get(activeTrackball).setEnabled(false);
				
				if (lightsControlled == 1)
				{
					// no lights controlled -> one light controlled
					lightControlTrackball.setEnabled(true);
					lightControlTrackball.setTrackballMatrix(Matrix4.identity());
					lightControlTrackball.setScale(1.0f);
				}
			}
		}
	}

	@Override
	public void keyReleased(Window window, int keycode, ModifierKeys mods) 
	{
		if (keycode >= KeyCodes.ZERO && keycode <= KeyCodes.SEVEN)
		{
			int selection = keycode - KeyCodes.ZERO;
			
			if (selection < trackballs.size() && lightControls.get(selection))
			{
				lightControls.set(selection, false);
				lightsControlled--;
				
//				trackballs.get(selection).setTrackballMatrix(
//					trackballs.get(activeTrackball).getTrackballMatrix().quickInverse(0.001f)
//						.times(lightControlTrackball.getTrackballMatrix())
//						.times(trackballs.get(activeTrackball).getTrackballMatrix())
//						.times(trackballs.get(selection).getTrackballMatrix()));
				
				trackballs.get(selection).setTrackballMatrix(
					trackballs.get(selection).getTrackballMatrix()
						.times(trackballs.get(activeTrackball).getTrackballMatrix().quickInverse(0.001f))
						.times(lightControlTrackball.getTrackballMatrix())
						.times(trackballs.get(activeTrackball).getTrackballMatrix()));
				
				if (lightsControlled == 0)
				{
					trackballs.get(activeTrackball).setEnabled(true);
					lightControlTrackball.setEnabled(false);
				}
			}
		}
	}

	@Override
	public int getLightCount() 
	{
		return this.trackballs.size();
	}

	@Override
	public Vector3 getLightColor(int i) 
	{
		return this.lightColors.get(i);
	}

	@Override
	public Matrix4 getLightMatrix(int i) 
	{
		if (lightControls.get(i))
		{
			return trackballs.get(i).getViewMatrix()
					//.times(trackballs.get(i).getTrackballMatrix().quickInverse(0.001f))
					.times(trackballs.get(activeTrackball).getTrackballMatrix().quickInverse(0.001f))
					.times(lightControlTrackball.getTrackballMatrix())
					.times(trackballs.get(activeTrackball).getTrackballMatrix())
					;//.times(trackballs.get(i).getTrackballMatrix());
		}
		else
		{
			return this.trackballs.get(i).getViewMatrix();
		}
	}
}
