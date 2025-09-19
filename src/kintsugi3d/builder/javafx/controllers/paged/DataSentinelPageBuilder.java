package kintsugi3d.builder.javafx.controllers.paged;

import java.util.function.Supplier;

public class DataSentinelPageBuilder<T> extends DataPageBuilder<Object, T, PageFrameController>
{
    DataSentinelPageBuilder(PageFrameController frameController, Supplier<PageFrameController> finisher, T data)
    {
        super(new SentinelPage<>(), frameController, finisher);

        // Whenever the next page (i.e. true first page) is assigned, make it the current page of the frame controller.
        getPage().getNextPageObservable().addListener(
            obs ->
            {
                getPage().getNextPage().receiveData(data);
                frameController.setCurrentPage(getPage().getNextPage());
            });
    }
}
