package tetzlaff.models;//Created by alexk on 7/31/2017.

public interface SettingsModel extends ReadonlySettingsModel
{
    <T> void set(String name, T value);
}
