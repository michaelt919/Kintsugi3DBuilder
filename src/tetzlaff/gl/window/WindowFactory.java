package tetzlaff.gl.window;

public interface WindowFactory<WindowType extends Window<?>>
{
	WindowBuilder<? extends WindowType> getWindowBuilder(int width, int height, String title);
}
