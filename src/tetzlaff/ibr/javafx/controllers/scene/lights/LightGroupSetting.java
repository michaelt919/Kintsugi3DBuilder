package tetzlaff.ibr.javafx.controllers.scene.lights;//Created by alexk on 7/16/2017.

import java.util.ArrayList;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

public class LightGroupSetting
{
    public final static int LIGHT_LIMIT = 4;

    private final ListProperty<LightInstanceSetting> lightList = new SimpleListProperty<>(
        new ObservableListWrapper<LightInstanceSetting>(
            new ArrayList<>(LIGHT_LIMIT)
        )
    );

    private final BooleanProperty locked = new SimpleBooleanProperty(false);

    private final StringProperty name = new SimpleStringProperty();

    public LightGroupSetting(String name)
    {
        this.name.setValue(name);
    }

    public void addLight()
    {
        if (lightList.size() < LIGHT_LIMIT)
        {
            lightList.add(
                new LightInstanceSetting(
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    1.0,
                    false,
                    "X",
                    LightType.PointLight,
                    Color.WHITE,
                    locked
                )
            );
        }
    }

    public void addLight(int index)
    {
        if (lightList.size() < LIGHT_LIMIT)
        {
            if (index >= 0 && index < lightList.size())
            {
                lightList.add(lightList.get(index).duplicate());
            }
            else
            {
                addLight();
            }
        }
    }

    public void removeLight()
    {
        if (lightList.size() > 0)
        {
            lightList.remove(lightList.size() - 1);
        }
    }

    public void removeLight(int index)
    {
        if (lightList.size() > 0)
        {
            if (index >= 0 && index < lightList.size())
            {
                lightList.remove(index);
            }
            else
            {
                removeLight();
            }
        }
    }

    public int getNLights()
    {
        return lightList.size();
    }

    public ObservableList<LightInstanceSetting> getLightList()
    {
        return lightList.get();
    }

    public ListProperty<LightInstanceSetting> lightListProperty()
    {
        return lightList;
    }

    public boolean isLocked()
    {
        return locked.get();
    }

    public BooleanProperty lockedProperty()
    {
        return locked;
    }

    public void setLocked(boolean locked)
    {
        this.locked.set(locked);
    }

    public String getName()
    {
        return name.get();
    }

    public StringProperty nameProperty()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name.set(name);
    }
}
