/*
 * LF Viewer - A tool to render Agisoft PhotoScan models as light fields.
 *
 * Copyright (c) 2016
 * The Regents of the University of Minnesota
 *     and
 * Cultural Heritage Imaging
 * All rights reserved
 *
 * This file is part of LF Viewer.
 *
 *     LF Viewer is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LF Viewer is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with LF Viewer.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package tetzlaff.window;
import static org.lwjgl.glfw.GLFW.*;

/**
 * A listing of possible key codes that could be used in key events.
 * @author Michael Tetzlaff
 *
 */
public class KeyCodes
{
	public static final int UNKNOWN = GLFW_KEY_UNKNOWN;
	public static final int SPACE = GLFW_KEY_SPACE;
	public static final int APOSTROPHE = GLFW_KEY_APOSTROPHE;
	public static final int COMMA = GLFW_KEY_COMMA;
	public static final int MINUS = GLFW_KEY_MINUS;
	public static final int PERIOD = GLFW_KEY_PERIOD;
	public static final int SLASH = GLFW_KEY_SLASH;
	public static final int ZERO = GLFW_KEY_0;
	public static final int ONE = GLFW_KEY_1;
	public static final int TWO = GLFW_KEY_2;
	public static final int THREE = GLFW_KEY_3;
	public static final int FOUR = GLFW_KEY_4;
	public static final int FIVE = GLFW_KEY_5;
	public static final int SIX = GLFW_KEY_6;
	public static final int SEVEN = GLFW_KEY_7;
	public static final int EIGHT = GLFW_KEY_8;
	public static final int NINE = GLFW_KEY_9;
	public static final int SEMICOLON = GLFW_KEY_SEMICOLON;
	public static final int EQUAL = GLFW_KEY_EQUAL;
	public static final int A = GLFW_KEY_A;
	public static final int B = GLFW_KEY_B;
	public static final int C = GLFW_KEY_C;
	public static final int D = GLFW_KEY_D;
	public static final int E = GLFW_KEY_E;
	public static final int F = GLFW_KEY_F;
	public static final int G = GLFW_KEY_G;
	public static final int H = GLFW_KEY_H;
	public static final int I = GLFW_KEY_I;
	public static final int J = GLFW_KEY_J;
	public static final int K = GLFW_KEY_K;
	public static final int L = GLFW_KEY_L;
	public static final int M = GLFW_KEY_M;
	public static final int N = GLFW_KEY_N;
	public static final int O = GLFW_KEY_O;
	public static final int P = GLFW_KEY_P;
	public static final int Q = GLFW_KEY_Q;
	public static final int R = GLFW_KEY_R;
	public static final int S = GLFW_KEY_S;
	public static final int T = GLFW_KEY_T;
	public static final int U = GLFW_KEY_U;
	public static final int V = GLFW_KEY_V;
	public static final int W = GLFW_KEY_W;
	public static final int X = GLFW_KEY_X;
	public static final int Y = GLFW_KEY_Y;
	public static final int Z = GLFW_KEY_Z;
	public static final int LEFT_BRACKET = GLFW_KEY_LEFT_BRACKET;
	public static final int BACKSLASH = GLFW_KEY_BACKSLASH;
	public static final int RIGHT_BRACKET = GLFW_KEY_RIGHT_BRACKET;
	public static final int GRAVE_ACCENT = GLFW_KEY_GRAVE_ACCENT;
	public static final int WORLD_1 = GLFW_KEY_WORLD_1;
	public static final int WORLD_2 = GLFW_KEY_WORLD_2;
	public static final int ESCAPE = GLFW_KEY_ESCAPE;
	public static final int ENTER = GLFW_KEY_ENTER;
	public static final int TAB = GLFW_KEY_TAB;
	public static final int BACKSPACE = GLFW_KEY_BACKSPACE;
	public static final int INSERT = GLFW_KEY_INSERT;
	public static final int DELETE = GLFW_KEY_DELETE;
	public static final int RIGHT = GLFW_KEY_RIGHT;
	public static final int LEFT = GLFW_KEY_LEFT;
	public static final int DOWN = GLFW_KEY_DOWN;
	public static final int UP = GLFW_KEY_UP;
	public static final int PAGE_UP = GLFW_KEY_PAGE_UP;
	public static final int PAGE_DOWN = GLFW_KEY_PAGE_DOWN;
	public static final int HOME = GLFW_KEY_HOME;
	public static final int END = GLFW_KEY_END;
	public static final int CAPS_LOCK = GLFW_KEY_CAPS_LOCK;
	public static final int SCROLL_LOCK = GLFW_KEY_SCROLL_LOCK;
	public static final int NUM_LOCK = GLFW_KEY_NUM_LOCK;
	public static final int PRINT_SCREEN = GLFW_KEY_PRINT_SCREEN;
	public static final int PAUSE = GLFW_KEY_PAUSE;
	public static final int F1 = GLFW_KEY_F1;
	public static final int F2 = GLFW_KEY_F2;
	public static final int F3 = GLFW_KEY_F3;
	public static final int F4 = GLFW_KEY_F4;
	public static final int F5 = GLFW_KEY_F5;
	public static final int F6 = GLFW_KEY_F6;
	public static final int F7 = GLFW_KEY_F7;
	public static final int F8 = GLFW_KEY_F8;
	public static final int F9 = GLFW_KEY_F9;
	public static final int F10 = GLFW_KEY_F10;
	public static final int F11 = GLFW_KEY_F11;
	public static final int F12 = GLFW_KEY_F12;
	public static final int F13 = GLFW_KEY_F13;
	public static final int F14 = GLFW_KEY_F14;
	public static final int F15 = GLFW_KEY_F15;
	public static final int F16 = GLFW_KEY_F16;
	public static final int F17 = GLFW_KEY_F17;
	public static final int F18 = GLFW_KEY_F18;
	public static final int F19 = GLFW_KEY_F19;
	public static final int F20 = GLFW_KEY_F20;
	public static final int F21 = GLFW_KEY_F21;
	public static final int F22 = GLFW_KEY_F22;
	public static final int F23 = GLFW_KEY_F23;
	public static final int F24 = GLFW_KEY_F24;
	public static final int F25 = GLFW_KEY_F25;
	public static final int KEYPAD_0 = GLFW_KEY_KP_0;
	public static final int KEYPAD_1 = GLFW_KEY_KP_1;
	public static final int KEYPAD_2 = GLFW_KEY_KP_2;
	public static final int KEYPAD_3 = GLFW_KEY_KP_3;
	public static final int KEYPAD_4 = GLFW_KEY_KP_4;
	public static final int KEYPAD_5 = GLFW_KEY_KP_5;
	public static final int KEYPAD_6 = GLFW_KEY_KP_6;
	public static final int KEYPAD_7 = GLFW_KEY_KP_7;
	public static final int KEYPAD_8 = GLFW_KEY_KP_8;
	public static final int KEYPAD_9 = GLFW_KEY_KP_9;
	public static final int KEYPAD_DECIMAL = GLFW_KEY_KP_DECIMAL;
	public static final int KEYPAD_DIVIDE = GLFW_KEY_KP_DIVIDE;
	public static final int KEYPAD_MULTIPLY = GLFW_KEY_KP_MULTIPLY;
	public static final int KEYPAD_SUBTRACT = GLFW_KEY_KP_SUBTRACT;
	public static final int KEYPAD_ADD = GLFW_KEY_KP_ADD;
	public static final int KEYPAD_ENTER = GLFW_KEY_KP_ENTER;
	public static final int KEYPAD_EQUAL = GLFW_KEY_KP_EQUAL;
	public static final int LEFT_SHIFT = GLFW_KEY_LEFT_SHIFT;
	public static final int LEFT_CONTROL = GLFW_KEY_LEFT_CONTROL;
	public static final int LEFT_ALT = GLFW_KEY_LEFT_ALT;
	public static final int LEFT_SUPER = GLFW_KEY_LEFT_SUPER;
	public static final int RIGHT_SHIFT = GLFW_KEY_RIGHT_SHIFT;
	public static final int RIGHT_CONTROL = GLFW_KEY_RIGHT_CONTROL;
	public static final int RIGHT_ALT = GLFW_KEY_RIGHT_ALT;
	public static final int RIGHT_SUPER = GLFW_KEY_RIGHT_SUPER;
	public static final int MENU = GLFW_KEY_MENU;
	public static final int LAST = GLFW_KEY_LAST;
}
