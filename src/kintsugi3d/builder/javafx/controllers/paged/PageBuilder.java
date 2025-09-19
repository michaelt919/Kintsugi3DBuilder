package kintsugi3d.builder.javafx.controllers.paged;

import java.util.function.Supplier;

public abstract class PageBuilder<FinishType>
{
    protected static final String SELECTION_PAGE_FXML = "/fxml/SelectionPage.fxml";

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

    public abstract Page<?,?> getPage();
}
