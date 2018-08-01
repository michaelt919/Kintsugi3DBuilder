package tetzlaff.gl.window;

import tetzlaff.gl.core.Context;
import tetzlaff.interactive.EventPollable;

public interface PollableWindow<ContextType extends Context<ContextType>> extends Window<ContextType>, EventPollable
{
}
