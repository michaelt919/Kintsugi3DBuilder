/*
 * Copyright (c) 2019 - 2026 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins, Simon Cao
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.experience;

import javafx.stage.Window;
import kintsugi3d.builder.javafx.core.JavaFXState;

/**
 * An experience encapsulates, typically, a modal window and its controller.
 */
public interface Experience
{
    /**
     * Gets the name that will be displayed in the modal window's title bar.
     * @return The name of the experience.
     */
    String getName();

    /**
     * Initializes the experience.  This should be called once and will typically be handled by ExperienceManager
     * for top-level experiences that are managed by that object.
     * @param parentWindow The parent window of the experience's modal window.
     * @param state All state-related data that the experience has access to.
     */
    void initialize(Window parentWindow, JavaFXState state);

    /**
     * Gets whether the experience is currently open.
     * @return true if the experience's modal window is open; false otherwise.
     */
    boolean isOpen();

    /**
     * Tries to open the experience, typically in a modal window, catching and logging any exceptions that occur
     * (i.e. if the FXML could not be loaded).
     * This method will ensure that the same modal is not opened more than once at the same time.
     */
    void tryOpen();

    /**
     * Gets whether the experience is initialized with a parent window and state access.
     * @return True if initialized; false otherwise.
     */
    boolean isInitialized();

    /**
     * Gets the parent window of the experience.
     * @return A reference to the parent window.
     */
    Window getParentWindow();

    /**
     * Gets the modal housing this experience.
     * @return The modal object for this experience.
     */
    Modal getModal();
}
