package tetzlaff.mvc.old.controllers.impl;

import tetzlaff.gl.window.Key;
import tetzlaff.gl.window.ModifierKeys;
import tetzlaff.gl.window.Window;
import tetzlaff.gl.window.listeners.KeyPressListener;
import tetzlaff.gl.window.listeners.KeyReleaseListener;
import tetzlaff.models.ReadonlyCameraModel;
import tetzlaff.mvc.old.controllers.LightController;
import tetzlaff.mvc.old.models.TrackballLightingModel;

public class TrackballLightController implements LightController, KeyPressListener, KeyReleaseListener
{
    private final TrackballLightingModel model;
    private final TrackballController lightControlTrackball;
    private final TrackballController[] trackballs;

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
    public void keyPressed(Window<?> window, Key key, ModifierKeys mods)
    {
        if (key == Key.ONE || key == Key.TWO || key == Key.THREE || key == Key.FOUR)
        {
            int selection;

            switch(key)
            {
                case ONE: selection = 0; break;
                case TWO: selection = 1; break;
                case THREE: selection = 2; break;
                case FOUR: selection = 3; break;
                default: return; // shouldn't happen
            }

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
    public void keyReleased(Window<?> window, Key key, ModifierKeys mods)
    {
        if (key == Key.ONE || key == Key.TWO || key == Key.THREE || key == Key.FOUR)
        {
            int selection;

            switch(key)
            {
                case ONE: selection = 0; break;
                case TWO: selection = 1; break;
                case THREE: selection = 2; break;
                case FOUR: selection = 3; break;
                default: return; // shouldn't happen
            }

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
