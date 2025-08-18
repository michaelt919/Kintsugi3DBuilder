package kintsugi3d.builder.javafx.controllers.paged;

import javafx.beans.binding.ObjectExpression;
import javafx.fxml.FXMLLoader;

public interface Page<InType, OutType>
{
    PageController<? super InType> getController();

    void initController();

    FXMLLoader getLoader();

    String getFXMLFilePath();

    Page<?, ? extends InType> getPrevPage();
    boolean hasPrevPage();
    Page<?, ? extends InType> setPrevPage(Page<?, ? extends InType> page);

    ObjectExpression<? extends Page<? super OutType, ?>> getNextPageObservable();
    Page<? super OutType, ?> getNextPage();
    boolean hasNextPage();
    Page<? super OutType, ?> setNextPage(Page<? super OutType, ?> page);

    Page<InType, OutType> receiveData(InType data);
    OutType getOutData();

    void sendOutData();
}
