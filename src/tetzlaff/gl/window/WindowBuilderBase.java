package tetzlaff.gl.window;

public abstract class WindowBuilderBase<WindowType extends Window<?>> implements WindowBuilder<WindowType>
{
    private String title;
    private int width;
    private int height;
    private int x;
    private int y;
    private boolean resizable = false;
    private int multisamples = 0;

    protected WindowBuilderBase(String title, int width, int height, int x, int y)
    {
        this.title = title;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    protected String getTitle()
    {
        return title;
    }

    protected int getWidth()
    {
        return width;
    }

    protected int getHeight()
    {
        return height;
    }

    protected int getX()
    {
        return x;
    }

    protected int getY()
    {
        return y;
    }

    protected boolean isResizable()
    {
        return resizable;
    }

    protected int getMultisamples()
    {
        return multisamples;
    }

    @Override
    public WindowBuilderBase<WindowType> setX(int x)
    {
        this.x = x;
        return this;
    }

    @Override
    public WindowBuilderBase<WindowType> setY(int y)
    {
        this.y = y;
        return this;
    }

    @Override
    public WindowBuilderBase<WindowType> setResizable(boolean resizable)
    {
        this.resizable = resizable;
        return this;
    }

    @Override
    public WindowBuilderBase<WindowType> setMultisamples(int multisamples)
    {
        this.multisamples = multisamples;
        return this;
    }
}
