package kintsugi3d.builder.javafx.controllers.paged;

import javafx.beans.binding.ObjectExpression;
import javafx.fxml.FXMLLoader;

import java.util.Map;

public interface Page<InType, OutType>
{
    PageController<? super InType> getController();

    void initController();

    FXMLLoader getLoader();

    String getFXMLFilePath();

    Page<?, ? extends InType> getPrevPage();
    boolean hasPrevPage();
    void setPrevPage(Page<?, ? extends InType> page);

    ObjectExpression<? extends Page<? super OutType, ?>> getNextPageObservable();
    Page<? super OutType, ?> getNextPage();
    boolean hasNextPage();
    void setNextPage(Page<? super OutType, ?> page);

    Map<String, Page<? super OutType, ?>> getFallbackPages();
    boolean hasFallbackPage();
    void addFallbackPage(String fallbackName, Page<? super OutType, ?> page);

    void linkBackFromNextPage();

    void receiveData(InType data);
    OutType getOutData();

    void sendOutData();
}
