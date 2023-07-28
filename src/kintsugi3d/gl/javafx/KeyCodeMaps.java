/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.gl.javafx;

import java.util.*;
import java.util.Map.Entry;

import javafx.scene.input.KeyCode;
import kintsugi3d.gl.window.Key;

final class KeyCodeMaps
{
    private static final Map<KeyCode, Key> CODE_TO_KEY;
    private static final Map<Key, List<KeyCode>> KEY_TO_CODES;

    static
    {
        CODE_TO_KEY = new EnumMap<>(KeyCode.class);
        CODE_TO_KEY.put(KeyCode.SPACE, Key.SPACE);
        CODE_TO_KEY.put(KeyCode.QUOTE, Key.APOSTROPHE);
        CODE_TO_KEY.put(KeyCode.COMMA, Key.COMMA);
        CODE_TO_KEY.put(KeyCode.MINUS, Key.MINUS);
        CODE_TO_KEY.put(KeyCode.PERIOD, Key.PERIOD);
        CODE_TO_KEY.put(KeyCode.SLASH, Key.SLASH);
        CODE_TO_KEY.put(KeyCode.DIGIT0, Key.ZERO);
        CODE_TO_KEY.put(KeyCode.DIGIT1, Key.ONE);
        CODE_TO_KEY.put(KeyCode.DIGIT2, Key.TWO);
        CODE_TO_KEY.put(KeyCode.DIGIT3, Key.THREE);
        CODE_TO_KEY.put(KeyCode.DIGIT4, Key.FOUR);
        CODE_TO_KEY.put(KeyCode.DIGIT5, Key.FIVE);
        CODE_TO_KEY.put(KeyCode.DIGIT6, Key.SIX);
        CODE_TO_KEY.put(KeyCode.DIGIT7, Key.SEVEN);
        CODE_TO_KEY.put(KeyCode.DIGIT8, Key.EIGHT);
        CODE_TO_KEY.put(KeyCode.DIGIT9, Key.NINE);
        CODE_TO_KEY.put(KeyCode.SEMICOLON, Key.SEMICOLON);
        CODE_TO_KEY.put(KeyCode.EQUALS, Key.EQUAL);
        CODE_TO_KEY.put(KeyCode.A, Key.A);
        CODE_TO_KEY.put(KeyCode.B, Key.B);
        CODE_TO_KEY.put(KeyCode.C, Key.C);
        CODE_TO_KEY.put(KeyCode.D, Key.D);
        CODE_TO_KEY.put(KeyCode.E, Key.E);
        CODE_TO_KEY.put(KeyCode.F, Key.F);
        CODE_TO_KEY.put(KeyCode.G, Key.G);
        CODE_TO_KEY.put(KeyCode.H, Key.H);
        CODE_TO_KEY.put(KeyCode.I, Key.I);
        CODE_TO_KEY.put(KeyCode.J, Key.J);
        CODE_TO_KEY.put(KeyCode.K, Key.K);
        CODE_TO_KEY.put(KeyCode.L, Key.L);
        CODE_TO_KEY.put(KeyCode.M, Key.M);
        CODE_TO_KEY.put(KeyCode.N, Key.N);
        CODE_TO_KEY.put(KeyCode.O, Key.O);
        CODE_TO_KEY.put(KeyCode.P, Key.P);
        CODE_TO_KEY.put(KeyCode.Q, Key.Q);
        CODE_TO_KEY.put(KeyCode.R, Key.R);
        CODE_TO_KEY.put(KeyCode.S, Key.S);
        CODE_TO_KEY.put(KeyCode.T, Key.T);
        CODE_TO_KEY.put(KeyCode.U, Key.U);
        CODE_TO_KEY.put(KeyCode.V, Key.V);
        CODE_TO_KEY.put(KeyCode.W, Key.W);
        CODE_TO_KEY.put(KeyCode.X, Key.X);
        CODE_TO_KEY.put(KeyCode.Y, Key.Y);
        CODE_TO_KEY.put(KeyCode.Z, Key.Z);
        CODE_TO_KEY.put(KeyCode.OPEN_BRACKET, Key.LEFT_BRACKET);
        CODE_TO_KEY.put(KeyCode.BACK_SLASH, Key.BACKSLASH);
        CODE_TO_KEY.put(KeyCode.CLOSE_BRACKET, Key.RIGHT_BRACKET);
        CODE_TO_KEY.put(KeyCode.BACK_QUOTE, Key.GRAVE_ACCENT);
        CODE_TO_KEY.put(KeyCode.ESCAPE, Key.ESCAPE);
        CODE_TO_KEY.put(KeyCode.ENTER, Key.ENTER);
        CODE_TO_KEY.put(KeyCode.TAB, Key.TAB);
        CODE_TO_KEY.put(KeyCode.BACK_SPACE, Key.BACKSPACE);
        CODE_TO_KEY.put(KeyCode.INSERT, Key.INSERT);
        CODE_TO_KEY.put(KeyCode.DELETE, Key.DELETE);
        CODE_TO_KEY.put(KeyCode.RIGHT, Key.RIGHT);
        CODE_TO_KEY.put(KeyCode.LEFT, Key.LEFT);
        CODE_TO_KEY.put(KeyCode.DOWN, Key.DOWN);
        CODE_TO_KEY.put(KeyCode.UP, Key.UP);
        CODE_TO_KEY.put(KeyCode.PAGE_UP, Key.PAGE_UP);
        CODE_TO_KEY.put(KeyCode.PAGE_DOWN, Key.PAGE_DOWN);
        CODE_TO_KEY.put(KeyCode.HOME, Key.HOME);
        CODE_TO_KEY.put(KeyCode.END, Key.END);
        CODE_TO_KEY.put(KeyCode.CAPS, Key.CAPS_LOCK);
        CODE_TO_KEY.put(KeyCode.SCROLL_LOCK, Key.SCROLL_LOCK);
        CODE_TO_KEY.put(KeyCode.NUM_LOCK, Key.NUM_LOCK);
        CODE_TO_KEY.put(KeyCode.PRINTSCREEN, Key.PRINT_SCREEN);
        CODE_TO_KEY.put(KeyCode.PAUSE, Key.PAUSE);
        CODE_TO_KEY.put(KeyCode.F1, Key.F1);
        CODE_TO_KEY.put(KeyCode.F2, Key.F2);
        CODE_TO_KEY.put(KeyCode.F3, Key.F3);
        CODE_TO_KEY.put(KeyCode.F4, Key.F4);
        CODE_TO_KEY.put(KeyCode.F5, Key.F5);
        CODE_TO_KEY.put(KeyCode.F6, Key.F6);
        CODE_TO_KEY.put(KeyCode.F7, Key.F7);
        CODE_TO_KEY.put(KeyCode.F8, Key.F8);
        CODE_TO_KEY.put(KeyCode.F9, Key.F9);
        CODE_TO_KEY.put(KeyCode.F10, Key.F10);
        CODE_TO_KEY.put(KeyCode.F11, Key.F11);
        CODE_TO_KEY.put(KeyCode.F12, Key.F12);
        CODE_TO_KEY.put(KeyCode.F13, Key.F13);
        CODE_TO_KEY.put(KeyCode.F14, Key.F14);
        CODE_TO_KEY.put(KeyCode.F15, Key.F15);
        CODE_TO_KEY.put(KeyCode.F16, Key.F16);
        CODE_TO_KEY.put(KeyCode.F17, Key.F17);
        CODE_TO_KEY.put(KeyCode.F18, Key.F18);
        CODE_TO_KEY.put(KeyCode.F19, Key.F19);
        CODE_TO_KEY.put(KeyCode.F20, Key.F20);
        CODE_TO_KEY.put(KeyCode.F21, Key.F21);
        CODE_TO_KEY.put(KeyCode.F22, Key.F22);
        CODE_TO_KEY.put(KeyCode.F23, Key.F23);
        CODE_TO_KEY.put(KeyCode.F24, Key.F24);
        CODE_TO_KEY.put(KeyCode.NUMPAD0, Key.KEYPAD_0);
        CODE_TO_KEY.put(KeyCode.NUMPAD1, Key.KEYPAD_1);
        CODE_TO_KEY.put(KeyCode.NUMPAD2, Key.KEYPAD_2);
        CODE_TO_KEY.put(KeyCode.NUMPAD3, Key.KEYPAD_3);
        CODE_TO_KEY.put(KeyCode.NUMPAD4, Key.KEYPAD_4);
        CODE_TO_KEY.put(KeyCode.NUMPAD5, Key.KEYPAD_5);
        CODE_TO_KEY.put(KeyCode.NUMPAD6, Key.KEYPAD_6);
        CODE_TO_KEY.put(KeyCode.NUMPAD7, Key.KEYPAD_7);
        CODE_TO_KEY.put(KeyCode.NUMPAD8, Key.KEYPAD_8);
        CODE_TO_KEY.put(KeyCode.NUMPAD9, Key.KEYPAD_9);
        CODE_TO_KEY.put(KeyCode.DECIMAL, Key.KEYPAD_DECIMAL);
        CODE_TO_KEY.put(KeyCode.DIVIDE, Key.KEYPAD_DIVIDE);
        CODE_TO_KEY.put(KeyCode.MULTIPLY, Key.KEYPAD_MULTIPLY);
        CODE_TO_KEY.put(KeyCode.SUBTRACT, Key.KEYPAD_SUBTRACT);
        CODE_TO_KEY.put(KeyCode.ADD, Key.KEYPAD_ADD);
        CODE_TO_KEY.put(KeyCode.SHIFT, Key.SHIFT);
        CODE_TO_KEY.put(KeyCode.CONTROL, Key.CONTROL);
        CODE_TO_KEY.put(KeyCode.ALT, Key.ALT);
        CODE_TO_KEY.put(KeyCode.META, Key.SUPER);
        CODE_TO_KEY.put(KeyCode.CONTEXT_MENU, Key.MENU);


        KEY_TO_CODES = new EnumMap<>(Key.class);
        for (Entry<KeyCode, Key> entry : CODE_TO_KEY.entrySet())
        {
            KEY_TO_CODES.computeIfAbsent(entry.getValue(), key -> new ArrayList<>(1))
                .add(entry.getKey());
        }
    }

    private KeyCodeMaps()
    {
    }

    static Key codeToKey(KeyCode code)
    {
        return CODE_TO_KEY.getOrDefault(code, Key.UNKNOWN);
    }

    static Iterable<KeyCode> keyToCode(Key key)
    {
        return KEY_TO_CODES.getOrDefault(key, Collections.emptyList());
    }
}
