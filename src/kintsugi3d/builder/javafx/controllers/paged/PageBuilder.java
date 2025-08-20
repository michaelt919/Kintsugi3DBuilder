package kintsugi3d.builder.javafx.controllers.paged;

import java.util.function.Supplier;

public class PageBuilder<FinishType>
{
    protected final PageFrameController frameController;
    protected final Supplier<FinishType> finisher;

    public PageBuilder(PageFrameController frameController, Supplier<FinishType> finisher)
    {
        this.frameController = frameController;
        this.finisher = finisher;
    }

    public FinishType finish()
    {
        return finisher.get();
    }
}
