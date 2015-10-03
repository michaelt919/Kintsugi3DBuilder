package tetzlaff.imagebasedmicrofacet;

import tetzlaff.gl.Context;
import tetzlaff.gl.helpers.Vector3;
import tetzlaff.window.Window;
import tetzlaff.window.listeners.CharacterListener;

public class LightController<ContextType extends Context<ContextType>> implements CharacterListener
{
	private TrackballLightModel renderer;
	
	public LightController(TrackballLightModel renderer)
	{
		this.renderer = renderer;
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
			renderer.setActiveTrackball(c - '0');
			break;
		case 't':
			renderer.setActiveLightColor(renderer.getActiveLightColor().plus(new Vector3(1.0f / 32.0f, 0.0f, 0.0f)));
			break;
		case 'r':
			renderer.setActiveLightColor(renderer.getActiveLightColor().plus(new Vector3(-1.0f / 32.0f, 0.0f, 0.0f)));
			if (renderer.getActiveLightColor().x < 0.0f)
			{
				renderer.setActiveLightColor(new Vector3(0.0f, renderer.getActiveLightColor().y, renderer.getActiveLightColor().z));
			}
			break;
		case 'g':
			renderer.setActiveLightColor(renderer.getActiveLightColor().plus(new Vector3(0.0f, 1.0f / 32.0f, 0.0f)));
			break;
		case 'f':
			renderer.setActiveLightColor(renderer.getActiveLightColor().plus(new Vector3(0.0f, -1.0f / 32.0f, 0.0f)));
			if (renderer.getActiveLightColor().y < 0.0f)
			{
				renderer.setActiveLightColor(new Vector3(renderer.getActiveLightColor().x, 0.0f, renderer.getActiveLightColor().z));
			}
			break;
		case 'b':
			renderer.setActiveLightColor(renderer.getActiveLightColor().plus(new Vector3(0.0f, 0.0f, 1.0f / 32.0f)));
			break;
		case 'v':
			renderer.setActiveLightColor(renderer.getActiveLightColor().plus(new Vector3(0.0f, 0.0f, -1.0f / 32.0f)));
			if (renderer.getActiveLightColor().z < 0.0f)
			{
				renderer.setActiveLightColor(new Vector3(renderer.getActiveLightColor().x, renderer.getActiveLightColor().y, 0.0f));
			}
			break;
		}
	}
}
