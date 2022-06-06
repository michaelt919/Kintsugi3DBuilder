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

package tetzlaff.gl.glfw;

import java.util.*;
import java.util.Map.Entry;

import tetzlaff.gl.window.Key;

import static org.lwjgl.glfw.GLFW.*;

final class KeyCodeMaps
{
    private static final Map<Integer, Key> CODE_TO_KEY;
    private static final Map<Key, List<Integer>> KEY_TO_CODES;

    static
    {
        CODE_TO_KEY = new HashMap<>(Key.values().length);
        CODE_TO_KEY.put(GLFW_KEY_SPACE, Key.SPACE);
        CODE_TO_KEY.put(GLFW_KEY_APOSTROPHE, Key.APOSTROPHE);
        CODE_TO_KEY.put(GLFW_KEY_COMMA, Key.COMMA);
        CODE_TO_KEY.put(GLFW_KEY_MINUS, Key.MINUS);
        CODE_TO_KEY.put(GLFW_KEY_PERIOD, Key.PERIOD);
        CODE_TO_KEY.put(GLFW_KEY_SLASH, Key.SLASH);
        CODE_TO_KEY.put(GLFW_KEY_0, Key.ZERO);
        CODE_TO_KEY.put(GLFW_KEY_1, Key.ONE);
        CODE_TO_KEY.put(GLFW_KEY_2, Key.TWO);
        CODE_TO_KEY.put(GLFW_KEY_3, Key.THREE);
        CODE_TO_KEY.put(GLFW_KEY_4, Key.FOUR);
        CODE_TO_KEY.put(GLFW_KEY_5, Key.FIVE);
        CODE_TO_KEY.put(GLFW_KEY_6, Key.SIX);
        CODE_TO_KEY.put(GLFW_KEY_7, Key.SEVEN);
        CODE_TO_KEY.put(GLFW_KEY_8, Key.EIGHT);
        CODE_TO_KEY.put(GLFW_KEY_9, Key.NINE);
        CODE_TO_KEY.put(GLFW_KEY_SEMICOLON, Key.SEMICOLON);
        CODE_TO_KEY.put(GLFW_KEY_EQUAL, Key.EQUAL);
        CODE_TO_KEY.put(GLFW_KEY_A, Key.A);
        CODE_TO_KEY.put(GLFW_KEY_B, Key.B);
        CODE_TO_KEY.put(GLFW_KEY_C, Key.C);
        CODE_TO_KEY.put(GLFW_KEY_D, Key.D);
        CODE_TO_KEY.put(GLFW_KEY_E, Key.E);
        CODE_TO_KEY.put(GLFW_KEY_F, Key.F);
        CODE_TO_KEY.put(GLFW_KEY_G, Key.G);
        CODE_TO_KEY.put(GLFW_KEY_H, Key.H);
        CODE_TO_KEY.put(GLFW_KEY_I, Key.I);
        CODE_TO_KEY.put(GLFW_KEY_J, Key.J);
        CODE_TO_KEY.put(GLFW_KEY_K, Key.K);
        CODE_TO_KEY.put(GLFW_KEY_L, Key.L);
        CODE_TO_KEY.put(GLFW_KEY_M, Key.M);
        CODE_TO_KEY.put(GLFW_KEY_N, Key.N);
        CODE_TO_KEY.put(GLFW_KEY_O, Key.O);
        CODE_TO_KEY.put(GLFW_KEY_P, Key.P);
        CODE_TO_KEY.put(GLFW_KEY_Q, Key.Q);
        CODE_TO_KEY.put(GLFW_KEY_R, Key.R);
        CODE_TO_KEY.put(GLFW_KEY_S, Key.S);
        CODE_TO_KEY.put(GLFW_KEY_T, Key.T);
        CODE_TO_KEY.put(GLFW_KEY_U, Key.U);
        CODE_TO_KEY.put(GLFW_KEY_V, Key.V);
        CODE_TO_KEY.put(GLFW_KEY_W, Key.W);
        CODE_TO_KEY.put(GLFW_KEY_X, Key.X);
        CODE_TO_KEY.put(GLFW_KEY_Y, Key.Y);
        CODE_TO_KEY.put(GLFW_KEY_Z, Key.Z);
        CODE_TO_KEY.put(GLFW_KEY_LEFT_BRACKET, Key.LEFT_BRACKET);
        CODE_TO_KEY.put(GLFW_KEY_BACKSLASH, Key.BACKSLASH);
        CODE_TO_KEY.put(GLFW_KEY_RIGHT_BRACKET, Key.RIGHT_BRACKET);
        CODE_TO_KEY.put(GLFW_KEY_GRAVE_ACCENT, Key.GRAVE_ACCENT);
        CODE_TO_KEY.put(GLFW_KEY_ESCAPE, Key.ESCAPE);
        CODE_TO_KEY.put(GLFW_KEY_ENTER, Key.ENTER);
        CODE_TO_KEY.put(GLFW_KEY_TAB, Key.TAB);
        CODE_TO_KEY.put(GLFW_KEY_BACKSPACE, Key.BACKSPACE);
        CODE_TO_KEY.put(GLFW_KEY_INSERT, Key.INSERT);
        CODE_TO_KEY.put(GLFW_KEY_DELETE, Key.DELETE);
        CODE_TO_KEY.put(GLFW_KEY_RIGHT, Key.RIGHT);
        CODE_TO_KEY.put(GLFW_KEY_LEFT, Key.LEFT);
        CODE_TO_KEY.put(GLFW_KEY_DOWN, Key.DOWN);
        CODE_TO_KEY.put(GLFW_KEY_UP, Key.UP);
        CODE_TO_KEY.put(GLFW_KEY_PAGE_UP, Key.PAGE_UP);
        CODE_TO_KEY.put(GLFW_KEY_PAGE_DOWN, Key.PAGE_DOWN);
        CODE_TO_KEY.put(GLFW_KEY_HOME, Key.HOME);
        CODE_TO_KEY.put(GLFW_KEY_END, Key.END);
        CODE_TO_KEY.put(GLFW_KEY_CAPS_LOCK, Key.CAPS_LOCK);
        CODE_TO_KEY.put(GLFW_KEY_SCROLL_LOCK, Key.SCROLL_LOCK);
        CODE_TO_KEY.put(GLFW_KEY_NUM_LOCK, Key.NUM_LOCK);
        CODE_TO_KEY.put(GLFW_KEY_PRINT_SCREEN, Key.PRINT_SCREEN);
        CODE_TO_KEY.put(GLFW_KEY_PAUSE, Key.PAUSE);
        CODE_TO_KEY.put(GLFW_KEY_F1, Key.F1);
        CODE_TO_KEY.put(GLFW_KEY_F2, Key.F2);
        CODE_TO_KEY.put(GLFW_KEY_F3, Key.F3);
        CODE_TO_KEY.put(GLFW_KEY_F4, Key.F4);
        CODE_TO_KEY.put(GLFW_KEY_F5, Key.F5);
        CODE_TO_KEY.put(GLFW_KEY_F6, Key.F6);
        CODE_TO_KEY.put(GLFW_KEY_F7, Key.F7);
        CODE_TO_KEY.put(GLFW_KEY_F8, Key.F8);
        CODE_TO_KEY.put(GLFW_KEY_F9, Key.F9);
        CODE_TO_KEY.put(GLFW_KEY_F10, Key.F10);
        CODE_TO_KEY.put(GLFW_KEY_F11, Key.F11);
        CODE_TO_KEY.put(GLFW_KEY_F12, Key.F12);
        CODE_TO_KEY.put(GLFW_KEY_F13, Key.F13);
        CODE_TO_KEY.put(GLFW_KEY_F14, Key.F14);
        CODE_TO_KEY.put(GLFW_KEY_F15, Key.F15);
        CODE_TO_KEY.put(GLFW_KEY_F16, Key.F16);
        CODE_TO_KEY.put(GLFW_KEY_F17, Key.F17);
        CODE_TO_KEY.put(GLFW_KEY_F18, Key.F18);
        CODE_TO_KEY.put(GLFW_KEY_F19, Key.F19);
        CODE_TO_KEY.put(GLFW_KEY_F20, Key.F20);
        CODE_TO_KEY.put(GLFW_KEY_F21, Key.F21);
        CODE_TO_KEY.put(GLFW_KEY_F22, Key.F22);
        CODE_TO_KEY.put(GLFW_KEY_F23, Key.F23);
        CODE_TO_KEY.put(GLFW_KEY_F24, Key.F24);
        CODE_TO_KEY.put(GLFW_KEY_KP_0, Key.KEYPAD_0);
        CODE_TO_KEY.put(GLFW_KEY_KP_1, Key.KEYPAD_1);
        CODE_TO_KEY.put(GLFW_KEY_KP_2, Key.KEYPAD_2);
        CODE_TO_KEY.put(GLFW_KEY_KP_3, Key.KEYPAD_3);
        CODE_TO_KEY.put(GLFW_KEY_KP_4, Key.KEYPAD_4);
        CODE_TO_KEY.put(GLFW_KEY_KP_5, Key.KEYPAD_5);
        CODE_TO_KEY.put(GLFW_KEY_KP_6, Key.KEYPAD_6);
        CODE_TO_KEY.put(GLFW_KEY_KP_7, Key.KEYPAD_7);
        CODE_TO_KEY.put(GLFW_KEY_KP_8, Key.KEYPAD_8);
        CODE_TO_KEY.put(GLFW_KEY_KP_9, Key.KEYPAD_9);
        CODE_TO_KEY.put(GLFW_KEY_KP_DECIMAL, Key.KEYPAD_DECIMAL);
        CODE_TO_KEY.put(GLFW_KEY_KP_DIVIDE, Key.KEYPAD_DIVIDE);
        CODE_TO_KEY.put(GLFW_KEY_KP_MULTIPLY, Key.KEYPAD_MULTIPLY);
        CODE_TO_KEY.put(GLFW_KEY_KP_SUBTRACT, Key.KEYPAD_SUBTRACT);
        CODE_TO_KEY.put(GLFW_KEY_KP_ADD, Key.KEYPAD_ADD);
        CODE_TO_KEY.put(GLFW_KEY_KP_ENTER, Key.ENTER);
        CODE_TO_KEY.put(GLFW_KEY_KP_EQUAL, Key.EQUAL);
        CODE_TO_KEY.put(GLFW_KEY_LEFT_SHIFT, Key.SHIFT);
        CODE_TO_KEY.put(GLFW_KEY_LEFT_CONTROL, Key.CONTROL);
        CODE_TO_KEY.put(GLFW_KEY_LEFT_ALT, Key.ALT);
        CODE_TO_KEY.put(GLFW_KEY_LEFT_SUPER, Key.SUPER);
        CODE_TO_KEY.put(GLFW_KEY_RIGHT_SHIFT, Key.SHIFT);
        CODE_TO_KEY.put(GLFW_KEY_RIGHT_CONTROL, Key.CONTROL);
        CODE_TO_KEY.put(GLFW_KEY_RIGHT_ALT, Key.ALT);
        CODE_TO_KEY.put(GLFW_KEY_RIGHT_SUPER, Key.SUPER);
        CODE_TO_KEY.put(GLFW_KEY_MENU, Key.MENU);

        KEY_TO_CODES = new EnumMap<>(Key.class);
        for (Entry<Integer, Key> entry : CODE_TO_KEY.entrySet())
        {
            KEY_TO_CODES.computeIfAbsent(entry.getValue(), key -> new ArrayList<>(1))
                .add(entry.getKey());
        }
    }

    private KeyCodeMaps()
    {
    }

    public static Key codeToKey(int code)
    {
        return CODE_TO_KEY.getOrDefault(code, Key.UNKNOWN);
    }

    public static Iterable<Integer> keyToCodes(Key key)
    {
        return KEY_TO_CODES.getOrDefault(key, Collections.emptyList());
    }
}
