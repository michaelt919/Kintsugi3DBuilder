package tetzlaff.gl.javafx;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

import tetzlaff.gl.window.WindowListenerManager;
import tetzlaff.gl.window.WindowListenerManagerInstance;
import tetzlaff.gl.window.listeners.*;

class EventCollector
{
    private final Queue<Consumer<WindowPositionListener>> windowPos = new LinkedList<>();
    private final Queue<Consumer<WindowSizeListener>> windowSize = new LinkedList<>();
    private final Queue<Consumer<WindowCloseListener>> windowClose = new LinkedList<>();
    private final Queue<Consumer<WindowRefreshListener>> windowRefresh = new LinkedList<>();
    private final Queue<Consumer<WindowFocusLostListener>> windowFocusLost = new LinkedList<>();
    private final Queue<Consumer<WindowFocusGainedListener>> windowFocusGained = new LinkedList<>();
    private final Queue<Consumer<WindowIconifiedListener>> windowIconified = new LinkedList<>();
    private final Queue<Consumer<WindowRestoredListener>> windowRestored = new LinkedList<>();
    private final Queue<Consumer<FramebufferSizeListener>> framebufferSize = new LinkedList<>();
    private final Queue<Consumer<KeyPressListener>> keyPress = new LinkedList<>();
    private final Queue<Consumer<KeyReleaseListener>> keyRelease = new LinkedList<>();
    private final Queue<Consumer<KeyTypeListener>> keyType = new LinkedList<>();
    private final Queue<Consumer<CharacterListener>> character = new LinkedList<>();
    private final Queue<Consumer<CharacterModifiersListener>> charMods = new LinkedList<>();
    private final Queue<Consumer<MouseButtonPressListener>> mouseButtonPress = new LinkedList<>();
    private final Queue<Consumer<MouseButtonReleaseListener>> mouseButtonRelease = new LinkedList<>();
    private final Queue<Consumer<CursorPositionListener>> cursorPos = new LinkedList<>();
    private final Queue<Consumer<CursorEnteredListener>> cursorEnter = new LinkedList<>();
    private final Queue<Consumer<CursorExitedListener>> cursorExit = new LinkedList<>();
    private final Queue<Consumer<ScrollListener>> scroll = new LinkedList<>();

    private final WindowListenerManagerInstance listenerManager = new WindowListenerManagerInstance();

    WindowListenerManager getListenerManager()
    {
        return listenerManager;
    }

    void windowPos(Consumer<WindowPositionListener> event)
    {
        this.windowPos.add(event);
    }

    void windowSize(Consumer<WindowSizeListener> event)
    {
        this.windowSize.add(event);
    }

    void windowClose(Consumer<WindowCloseListener> event)
    {
        this.windowClose.add(event);
    }

    void windowRefresh(Consumer<WindowRefreshListener> event)
    {
        this.windowRefresh.add(event);
    }

    void windowFocusLost(Consumer<WindowFocusLostListener> event)
    {
        this.windowFocusLost.add(event);
    }

    void windowFocusGained(Consumer<WindowFocusGainedListener> event)
    {
        this.windowFocusGained.add(event);
    }

    void windowIconified(Consumer<WindowIconifiedListener> event)
    {
        this.windowIconified.add(event);
    }

    void windowRestored(Consumer<WindowRestoredListener> event)
    {
        this.windowRestored.add(event);
    }

    void framebufferSize(Consumer<FramebufferSizeListener> event)
    {
        this.framebufferSize.add(event);
    }

    void keyPress(Consumer<KeyPressListener> event)
    {
        this.keyPress.add(event);
    }

    void keyRelease(Consumer<KeyReleaseListener> event)
    {
        this.keyRelease.add(event);
    }

    void keyType(Consumer<KeyTypeListener> event)
    {
        this.keyType.add(event);
    }

    void character(Consumer<CharacterListener> event)
    {
        this.character.add(event);
    }

    void charMods(Consumer<CharacterModifiersListener> event)
    {
        this.charMods.add(event);
    }

    void mouseButtonPress(Consumer<MouseButtonPressListener> event)
    {
        this.mouseButtonPress.add(event);
    }

    void mouseButtonRelease(Consumer<MouseButtonReleaseListener> event)
    {
        this.mouseButtonRelease.add(event);
    }

    void cursorPos(Consumer<CursorPositionListener> event)
    {
        this.cursorPos.add(event);
    }

    void cursorEnter(Consumer<CursorEnteredListener> event)
    {
        this.cursorEnter.add(event);
    }

    void cursorExit(Consumer<CursorExitedListener> event)
    {
        this.cursorExit.add(event);
    }

    void scroll(Consumer<ScrollListener> event)
    {
        this.scroll.add(event);
    }

    private <L> void pollEvents(Queue<Consumer<L>> eventQueue, Iterable<L> listeners)
    {
        while(!eventQueue.isEmpty())
        {
            Consumer<L> event = eventQueue.poll();
            for (L l : listeners)
            {
                event.accept(l);
            }
        }
    }

    void pollEvents()
    {
        pollEvents(windowPos, listenerManager.getWindowPosListeners());
        pollEvents(windowSize, listenerManager.getWindowSizeListeners());
        pollEvents(windowClose, listenerManager.getWindowCloseListeners());
        pollEvents(windowRefresh, listenerManager.getWindowRefreshListeners());
        pollEvents(windowFocusLost, listenerManager.getWindowFocusLostListeners());
        pollEvents(windowFocusGained, listenerManager.getWindowFocusGainedListeners());
        pollEvents(windowIconified, listenerManager.getWindowIconifiedListeners());
        pollEvents(windowRestored, listenerManager.getWindowRestoredListeners());
        pollEvents(framebufferSize, listenerManager.getFramebufferSizeListeners());
        pollEvents(keyPress, listenerManager.getKeyPressListeners());
        pollEvents(keyRelease, listenerManager.getKeyReleaseListeners());
        pollEvents(keyType, listenerManager.getKeyTypeListeners());
        pollEvents(character, listenerManager.getCharacterListeners());
        pollEvents(charMods, listenerManager.getCharModsListeners());
        pollEvents(mouseButtonPress, listenerManager.getMouseButtonPressListeners());
        pollEvents(mouseButtonRelease, listenerManager.getMouseButtonReleaseListeners());
        pollEvents(cursorPos, listenerManager.getCursorPosListeners());
        pollEvents(cursorEnter, listenerManager.getCursorEnterListeners());
        pollEvents(cursorExit, listenerManager.getCursorExitListeners());
        pollEvents(scroll, listenerManager.getScrollListeners());
    }
}
