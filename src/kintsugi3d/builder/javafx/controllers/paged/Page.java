package kintsugi3d.builder.javafx.controllers.paged;

import javafx.fxml.FXMLLoader;

public interface Page<ControllerType extends PageController<?>>
{
    ControllerType getController();

    void initController();

    FXMLLoader getLoader();

    String getFXMLFilePath();

    Page<?> getPrevPage();

    Page<?> getNextPage();

    boolean hasNextPage();

    boolean hasPrevPage();

    void submit();
}
