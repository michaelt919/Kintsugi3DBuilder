package tetzlaff.gl.window;

public interface WindowBuilder<WindowType extends Window<?>>
{
	WindowBuilder<WindowType> setX(int x);
	WindowBuilder<WindowType> setY(int y);
	WindowBuilder<WindowType> setResizable(boolean resizable);
	WindowBuilder<WindowType> setMultisamples(int multisamples);
	
	WindowType create();
}
