package kintsugi3d.builder.javafx.internal;

import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import kintsugi3d.builder.state.settings.ReadonlyGeneralSettingsModel;

public interface ReadonlyObservableGeneralSettingsModel extends ReadonlyGeneralSettingsModel
{
    Observable getObservable(String name);
    ObservableValue<Boolean> getBooleanProperty(String name);
    ObservableValue<Number> getNumericProperty(String name);
    <T> ObservableValue<T> getObjectProperty(String name, Class<T> settingType);
}
