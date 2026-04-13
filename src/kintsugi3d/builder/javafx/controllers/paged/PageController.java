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

package kintsugi3d.builder.javafx.controllers.paged;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.scene.layout.Region;

public interface PageController<T>
{
    Page<?, ?> getPage();

    PageFrameController getPageFrameController();
    void setPageFrameController(PageFrameController scroller);

    /**
     * Returns the outer AnchorPane, VBox, GridPane, etc. for the controller's fxml
     *
     * @return
     */
    Region getRootNode();

    BooleanExpression getCanAdvanceObservable();
    boolean canAdvance();

    StringExpression getAdvanceLabelOverrideObservable();
    String getAdvanceLabelOverride();

    BooleanExpression getCanConfirmObservable();
    boolean canConfirm();

    /**
     * Called when the page is created, after the page hsa been assigned to the controller.
     * In contrast with initialize() (which is called after JavaFX initialization, before any page object has been initialized)
     * when initPage() is called, a valid page will have been assigned to this controller.
     */
    void initPage();

    /**
     * Called when the previous page has finished if the previous page has data to share with this page
     * and this page is set up to receive data to forward to the controller.
     * @param data Data received from the previous page.
     */
    void receiveData(T data);

    /**
     * Called when the page is displayed, either through forward or backwards navigation.
     */
    void refresh();

    /**
     * Called when advancing from this page.
     * @return false if navigation was cancelled by the controller; true otherwise.
     */
    boolean advance();

    /**
     * Called after advancing if there are no additional pages and the paged experience is thus complete.
     * @return false if confirmation was cancelled by the controller; true otherwise.
     */
    boolean confirm();

    /**
     * Called when the window should close.
     * @return false if closing was cancelled by the controller; true otherwise.
     */
    boolean cancel();
}
