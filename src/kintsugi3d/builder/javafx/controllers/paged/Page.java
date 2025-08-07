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
    void setPrevPage(Page<?, ? extends InType> page);

    ObjectExpression<? extends Page<? super OutType, ?>> getNextPageObservable();
    Page<? super OutType, ?> getNextPage();
    boolean hasNextPage();
    void setNextPage(Page<? super OutType, ?> page);

    void receiveData(InType data);
    OutType getOutData();

    void submit();
}
