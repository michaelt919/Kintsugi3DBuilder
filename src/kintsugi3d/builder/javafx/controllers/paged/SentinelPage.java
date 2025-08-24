package kintsugi3d.builder.javafx.controllers.paged;

public class SentinelPage<T> extends PageBase<Object, T, PageController<? super Object>>
{
    public SentinelPage()
    {
        super(null, null);
    }

    @Override
    public void initController()
    {
    }

    @Override
    public T getOutData()
    {
        return null;
    }

    @Override
    public void setNextPage(Page<? super T, ?> page)
    {
        super.setNextPage(page);

        if (page.getPrevPage() == this)
        {
            // don't link back to this page
            page.setPrevPage(null);
        }
    }
}
