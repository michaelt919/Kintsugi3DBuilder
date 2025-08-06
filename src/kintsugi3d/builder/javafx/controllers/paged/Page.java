package kintsugi3d.builder.javafx.controllers.paged;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXMLLoader;

public interface Page<ControllerType extends PageController<?>>
{
    ControllerType getController();

    void initController();

    FXMLLoader getLoader();

    String getFXMLFilePath();

    Page<?> getPrevPage();

    ObjectProperty<? extends Page<?>> getNextPageProperty();

    Page<?> getNextPage();

    boolean hasNextPage();

    boolean hasPrevPage();

    void submit();
}
