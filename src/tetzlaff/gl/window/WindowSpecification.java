package tetzlaff.gl.window;

public interface WindowSpecification
{
    int getWidth();
    int getHeight();
    String getTitle();
    int getX();
    int getY();
    boolean isResizable();
    int getMultisamples();
}
