package tetzlaff.gl.javafx;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tetzlaff.gl.core.Context;
import tetzlaff.gl.window.*;

public class WindowImpl<ContextType extends Context<ContextType>>
    extends WindowBase<ContextType> implements PollableWindow<ContextType>
{
    private final ContextType baseContext;
    private final ContextType wrapperContext;

    private final Stage stage;
    private final Scene scene;
    private final Pane root;

    private boolean windowClosing = false;
    private boolean resourceClosed = false;

    private final Map<Key, KeyState> keyStates = new EnumMap<>(Key.class);
    private ModifierKeys modifierKeys = ModifierKeys.NONE;
    private MouseButtonState lmb = MouseButtonState.RELEASED;
    private MouseButtonState mmb = MouseButtonState.RELEASED;
    private MouseButtonState rmb = MouseButtonState.RELEASED;

    private CursorPosition cursorPosition = new CursorPosition(0, 0);

    private final EventCollector eventCollector = new EventCollector();

    private static class ModifierKeysInstance extends ModifierKeysBase
    {
        private final boolean shiftModifier;
        private final boolean ctrlModifier;
        private final boolean altModifier;
        private final boolean superModifier;

        ModifierKeysInstance(boolean shiftModifier, boolean ctrlModifier, boolean altModifier, boolean superModifier)
        {
            this.shiftModifier = shiftModifier;
            this.ctrlModifier = ctrlModifier;
            this.altModifier = altModifier;
            this.superModifier = superModifier;
        }

        @Override
        public boolean getShiftModifier()
        {
            return shiftModifier;
        }

        @Override
        public boolean getControlModifier()
        {
            return ctrlModifier;
        }

        @Override
        public boolean getAltModifier()
        {
            return altModifier;
        }

        @Override
        public boolean getSuperModifier()
        {
            return superModifier;
        }
    }

    WindowImpl(Stage primaryStage, ContextType baseContext, WindowSpecification windowSpec)
    {
        this.baseContext = baseContext;
        this.wrapperContext = new OffscreenContextWrapper(baseContext);

        this.stage = new Stage(StageStyle.UNIFIED);
        this.stage.initOwner(primaryStage);
        this.stage.setTitle(windowSpec.getTitle());
        this.stage.setResizable(windowSpec.isResizable());

        if (windowSpec.getX() >= 0)
        {
            this.stage.setX(windowSpec.getX());
        }

        if (windowSpec.getY() >= 0)
        {
            this.stage.setY(windowSpec.getY());
        }

        this.root = new AnchorPane();
        this.root.setPrefWidth(windowSpec.getWidth());
        this.root.setPrefHeight(windowSpec.getHeight());

        root.setOnKeyPressed(event ->
        {
            modifierKeys = new ModifierKeysInstance(
                event.isShiftDown(),
                event.isControlDown(),
                event.isAltDown(),
                event.isMetaDown());

            keyStates.put(KeyCodeMaps.codeToKey(event.getCode()), KeyState.PRESSED);

            eventCollector.keyPress(l -> l.keyPressed(this, KeyCodeMaps.codeToKey(event.getCode()), modifierKeys));
        });

        root.setOnKeyReleased(event ->
        {
            modifierKeys = new ModifierKeysInstance(
                event.isShiftDown(),
                event.isControlDown(),
                event.isAltDown(),
                event.isMetaDown());

            keyStates.put(KeyCodeMaps.codeToKey(event.getCode()), KeyState.RELEASED);

            eventCollector.keyRelease(l -> l.keyReleased(this, KeyCodeMaps.codeToKey(event.getCode()), modifierKeys));
        });

        this.root.setOnMousePressed(event ->
        {
            handleMouseEvent(event);
            getButtonIndex(event.getButton()).ifPresent(
                index -> eventCollector.mouseButtonPress(l -> l.mouseButtonPressed(this, index, modifierKeys)));
        });

        this.root.setOnMouseReleased(event ->
        {
            handleMouseEvent(event);
            getButtonIndex(event.getButton()).ifPresent(
                index -> eventCollector.mouseButtonRelease(l -> l.mouseButtonReleased(this, index, modifierKeys)));
        });

        EventHandler<? super MouseEvent> mouseCursor = event ->
        {
            handleMouseEvent(event);
            getButtonIndex(event.getButton()).ifPresent(
                index -> eventCollector.cursorPos(l -> l.cursorMoved(this, event.getSceneX(), event.getSceneY())));
        };

        ChangeListener<? super Number> windowSize = (event, oldValue, newValue) -> eventCollector.windowSize(
            l -> l.windowResized(this, (int) Math.round(stage.getWidth()), (int) Math.round(stage.getHeight())));

        this.stage.widthProperty().addListener(windowSize);
        this.stage.heightProperty().addListener(windowSize);

        this.stage.setOnCloseRequest(event -> eventCollector.windowClose(l -> l.windowClosing(this)));

        this.root.setOnMouseMoved(mouseCursor);
        this.root.setOnMouseDragged(mouseCursor);

        this.scene = new Scene(this.root);
        this.stage.setScene(this.scene);
        this.stage.sizeToScene();
    }

    private OptionalInt getButtonIndex(MouseButton button)
    {
        switch(button)
        {
            case PRIMARY: return OptionalInt.of(0);
            case SECONDARY: return OptionalInt.of(1);
            case MIDDLE: return OptionalInt.of(2);
            default: return OptionalInt.empty();
        }
    }

    private void handleMouseEvent(MouseEvent event)
    {
        modifierKeys = new ModifierKeysInstance(
            event.isShiftDown(),
            event.isControlDown(),
            event.isAltDown(),
            event.isMetaDown());

        this.lmb = event.isPrimaryButtonDown() ? MouseButtonState.PRESSED : MouseButtonState.RELEASED;
        this.mmb = event.isMiddleButtonDown() ? MouseButtonState.PRESSED : MouseButtonState.RELEASED;
        this.rmb = event.isSecondaryButtonDown() ? MouseButtonState.PRESSED : MouseButtonState.RELEASED;

        this.cursorPosition = new CursorPosition((int)Math.round(event.getSceneX()), (int)Math.round(event.getSceneY()));
    }

    @Override
    protected WindowListenerManager getListenerManager()
    {
        return eventCollector.getListenerManager();
    }

    @Override
    public void pollEvents()
    {
        eventCollector.pollEvents();
    }

    @Override
    public boolean shouldTerminate()
    {
        return windowClosing;
    }

    @Override
    public ContextType getBaseContext()
    {
        return baseContext;
    }

    @Override
    public void show()
    {
        stage.show();
    }

    @Override
    public void hide()
    {
        stage.hide();
    }

    @Override
    public void focus()
    {
        stage.requestFocus();
    }

    @Override
    public boolean isHighDPI()
    {
        return false;
    }

    @Override
    public boolean isWindowClosing()
    {
        return windowClosing;
    }

    @Override
    public void requestWindowClose()
    {
        windowClosing = true;
    }

    @Override
    public void cancelWindowClose()
    {
        windowClosing = false;
    }

    @Override
    public boolean isResourceClosed()
    {
        return resourceClosed;
    }

    @Override
    public void close()
    {
        stage.close();
        resourceClosed = true;
    }

    @Override
    public WindowSize getWindowSize()
    {
        return new WindowSize((int)Math.round(root.getWidth()), (int)Math.round(root.getHeight()));
    }

    @Override
    public WindowPosition getWindowPosition()
    {
        return new WindowPosition((int)Math.round(stage.getX()), (int)Math.round(stage.getY()));
    }

    @Override
    public void setWindowTitle(String title)
    {
        stage.setTitle(title);
    }

    @Override
    public void setWindowSize(int width, int height)
    {
        root.setPrefWidth(width);
        root.setPrefHeight(height);
        stage.sizeToScene();
    }

    @Override
    public void setWindowPosition(int x, int y)
    {
        stage.setX(x);
        stage.setY(y);
    }

    @Override
    public boolean isFocused()
    {
        return stage.isFocused();
    }

    @Override
    public MouseButtonState getMouseButtonState(int buttonIndex)
    {
        switch(buttonIndex)
        {
            case 0: return lmb;
            case 1: return rmb;
            case 2: return mmb;
            default: return MouseButtonState.RELEASED;
        }
    }

    @Override
    public KeyState getKeyState(Key key)
    {
        return keyStates.getOrDefault(key, KeyState.RELEASED);
    }

    @Override
    public CursorPosition getCursorPosition()
    {
        return cursorPosition;
    }

    @Override
    public ModifierKeys getModifierKeys()
    {
        return modifierKeys;
    }
}
