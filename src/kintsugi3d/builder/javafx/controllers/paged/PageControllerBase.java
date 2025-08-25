/*
 * Copyright (c) 2019 - 2025 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius, Atlas Collins
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.javafx.controllers.paged;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import kintsugi3d.builder.javafx.experience.Modal;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal base class that is extended for the public-facing classes
 * @param <T>
 * @param <PageType>
 */
abstract class PageControllerBase<T, PageType extends Page<?, ?>> implements PageController<T>
{
    private PageFrameController pageFrameController;
    private PageType page;
    private final BooleanProperty canAdvanceProperty = new SimpleBooleanProperty(false);
    private final StringProperty advanceLabelOverrideProperty = new SimpleStringProperty(null);
    private final BooleanProperty canConfirmProperty = new SimpleBooleanProperty(false);

    private volatile boolean alertShown = false;

    @Override
    public PageFrameController getPageFrameController()
    {
        return pageFrameController;
    }

    @Override
    public final void setPageFrameController(PageFrameController frameController)
    {
        this.pageFrameController = frameController;
    }

    @Override
    public final PageType getPage()
    {
        return this.page;
    }

    public final void setPage(PageType page)
    {
        this.page = page;
    }

    @Override
    public final boolean canAdvance()
    {
        return getCanAdvanceObservable().get();
    }

    @Override
    public final BooleanProperty getCanAdvanceObservable()
    {
        return canAdvanceProperty;
    }

    protected final void setCanAdvance(boolean canAdvance)
    {
        canAdvanceProperty.set(canAdvance);
    }

    @Override
    public final StringProperty getAdvanceLabelOverrideObservable()
    {
        return advanceLabelOverrideProperty;
    }

    @Override
    public final String getAdvanceLabelOverride()
    {
        return getAdvanceLabelOverrideObservable().get();
    }

    protected final void setAdvanceLabelOverride(String advanceLabelOverride)
    {
        advanceLabelOverrideProperty.set(advanceLabelOverride);
    }

    @Override
    public final BooleanProperty getCanConfirmObservable()
    {
        return canConfirmProperty;
    }

    @Override
    public final boolean canConfirm()
    {
        return canConfirmProperty.get();
    }

    protected final void setCanConfirm(boolean canConfirm)
    {
        canConfirmProperty.set(canConfirm);
    }

    @Override
    public boolean advance()
    {
        // By default, advance without any additional logic other than what page has been assigned as nextPage.
        return true;
    }

    @Override
    public boolean confirm()
    {
        // Allow silent exit by default if no confirmation is implemented.
        return true;
    }

    @Override
    public boolean close()
    {
        // Close without any additional logic by default.
        return true;
    }

    private void alert(String title, String message, boolean allowContinue)
    {
        if (alertShown) // Prevent multiple alerts from showing at once
        {
            return;
        }

        alertShown = true;
        Platform.runLater(() ->
        {
            if (getPage().hasFallbackPage())
            {
                // "Cancel" closes the modal entirely
                ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.LEFT);

                // "Fallback" buttons navigate to a different page.
                List<ButtonType> fallbackButtons = new ArrayList<>(getPage().getFallbackPages().size());
                for (var fallback : getPage().getFallbackPages().entrySet())
                {
                    String fallbackName = fallback.getKey();
                    fallbackButtons.add(new ButtonType(fallbackName));
                }

                List<ButtonType> allButtons = new ArrayList<>(fallbackButtons.size() + 2);
                allButtons.add(cancel);
                allButtons.addAll(fallbackButtons);

                // If allowed, "Continue" just continues as if nothing happened (only allowed for warnings)
                ButtonType continueButton = null;
                if (allowContinue)
                {
                    continueButton = new ButtonType("Continue", ButtonBar.ButtonData.RIGHT);
                    allButtons.add(continueButton);
                }

                Alert alert = new Alert(Alert.AlertType.NONE, message, allButtons.toArray(ButtonType[]::new));
                alert.setWidth(480);

                ((ButtonBase) alert.getDialogPane().lookupButton(cancel)).setOnAction(event ->
                {
                    Modal.requestClose(getPageFrameController().getWindow());
                    alertShown = false;
                });

                for (ButtonType button : fallbackButtons)
                {
                    ((ButtonBase) alert.getDialogPane().lookupButton(button)).setOnAction(event ->
                    {
                        getPageFrameController().fallbackPage(button.getText());
                        alertShown = false;
                    });
                }

                if (allowContinue)
                {
                    ((ButtonBase) alert.getDialogPane().lookupButton(continueButton)).setOnAction(event ->
                    {
                        alertShown = false;
                    });
                }

                alert.setTitle(title);
                alert.show();
            }
            else
            {
                ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE);
                Alert alert = new Alert(Alert.AlertType.NONE, message, ok);
                ((ButtonBase) alert.getDialogPane().lookupButton(ok)).setOnAction(event -> alertShown = false);
                alert.setTitle(title);
                alert.show();
            }
        });
    }

    protected void error(String title, String message)
    {
        alert(title, message, false);
    }

    protected void warning(String title, String message)
    {
        alert(title, message, true);
    }
}
