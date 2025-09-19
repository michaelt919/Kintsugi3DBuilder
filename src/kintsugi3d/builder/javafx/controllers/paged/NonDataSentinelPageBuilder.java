package kintsugi3d.builder.javafx.controllers.paged;

import java.util.function.Supplier;

public class NonDataSentinelPageBuilder extends NonDataPageBuilder<PageFrameController>
{
    NonDataSentinelPageBuilder(PageFrameController frameController, Supplier<PageFrameController> finisher)
    {
        super(new SentinelPage<>(), frameController, finisher);

        // Whenever the next page (i.e. true first page) is assigned, make it the current page of the frame controller.
        getPage().getNextPageObservable().addListener(
            obs -> frameController.setCurrentPage(getPage().getNextPage()));
    }
}
