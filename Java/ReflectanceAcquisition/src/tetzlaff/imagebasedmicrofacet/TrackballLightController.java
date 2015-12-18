package tetzlaff.imagebasedmicrofacet;

import java.util.ArrayList;
import java.util.List;

import tetzlaff.gl.helpers.CameraController;
import tetzlaff.gl.helpers.LightController;
import tetzlaff.gl.helpers.Matrix4;
import tetzlaff.gl.helpers.Trackball;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.window.Window;
import tetzlaff.window.listeners.CharacterListener;

public class TrackballLightController implements LightController, CharacterListener
{
	private int activeTrackball;
	private List<Vector3> lightColors;
	private List<Trackball> trackballs;
	
	public TrackballLightController()
	{
		this.activeTrackball = 0;
		
		this.lightColors = new ArrayList<Vector3>(8);
		
    	this.trackballs = new ArrayList<Trackball>(8);
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
    		}
    	}
	}
	
	private int getActiveTrackball()
	{
		return activeTrackball;
	}
	
	public CameraController asCameraController()
	{
		return () -> trackballs.get(this.getActiveTrackball()).getViewMatrix();
	}
	
	public void addAsWindowListener(Window window)
	{
		window.addCharacterListener(this);
		
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
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
			int selection = c - '0';
			if (selection < trackballs.size())
			{
				this.trackballs.get(this.activeTrackball).setEnabled(false);
				this.activeTrackball = selection;
				this.trackballs.get(this.activeTrackball).setEnabled(true);
			}
			break;
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
		return this.trackballs.get(i).getViewMatrix();
	}
}
