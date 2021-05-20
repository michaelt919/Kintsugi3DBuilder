/*
 *  Copyright (c) Michael Tetzlaff 2021
 *  Copyright (c) The Regents of the University of Minnesota 2019
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.optimization;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import tetzlaff.gl.builders.ProgramBuilder;
import tetzlaff.gl.builders.framebuffer.FramebufferObjectBuilder;
import tetzlaff.gl.core.*;

/**
 * A class for optimization where the entirety of the solving happens on the graphics card (typically useful for optimizing 4 parameters or less).
 * This class is primarily designed for the iterative portion of the algorithm, i.e. when using Levenberg-Marquardt or some other iterative method.
 * @param <ContextType> The type of the graphics context.
 */
public class ShaderBasedOptimization<ContextType extends Context<ContextType>> implements AutoCloseable
{
    private final Program<ContextType> estimationProgram;
    private final FramebufferObject<ContextType> framebuffer1;
    private final FramebufferObject<ContextType> framebuffer2;

    private final Drawable<ContextType> estimationDrawable;

    // References to the two framebuffers that can swap (double-buffering)
    private FramebufferObject<ContextType> frontFramebuffer;
    private FramebufferObject<ContextType> backFramebuffer;

    // Set finished to true to indicate that the back framebuffer has been destroyed.
    private boolean finished = false;

    private final Collection<BiConsumer<Program<ContextType>, FramebufferObject<ContextType>>> setupCallbacks = new LinkedList<>();
    private final Collection<Consumer<FramebufferObject<ContextType>>> postUpdateCallbacks = new LinkedList<>();

    public ShaderBasedOptimization(
        ProgramBuilder<ContextType> estimationProgramBuilder,
        FramebufferObjectBuilder<ContextType> framebufferObjectBuilder,
        Function<Program<ContextType>, Drawable<ContextType>> drawableFactory)
        throws FileNotFoundException
    {
        // Normal estimation program
        estimationProgram = estimationProgramBuilder.createProgram();

        // Framebuffers (double-buffered) for estimating normals on the GPU
        framebuffer1 = framebufferObjectBuilder.createFramebufferObject();
        framebuffer2 = framebufferObjectBuilder.createFramebufferObject();

        framebuffer1.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);
        framebuffer2.clearColorBuffer(0, 0.5f, 0.5f, 1.0f, 1.0f);

        // Double buffering since we need the previous normal estimate to generate the next normal estimate.
        frontFramebuffer = framebuffer1;
        backFramebuffer = framebuffer2;

        estimationDrawable = drawableFactory.apply(estimationProgram);
    }

    /**
     * Indicate that optimization is complete; this allows some GPU resources (the back framebuffer & shader program) to be de-allocated.
     */
    public void finish()
    {
        if (!finished)
        {
            // We're done with the estimation program.
            estimationProgram.close();

            // Close only the back framebuffer as we are done fitting but may still use the front framebuffer as a texture.
            backFramebuffer.close();
            finished = true;
        }
    }

    @Override
    public void close()
    {
        if (!finished)
        {
            estimationProgram.close();
        }

        // Make sure that if the object is "finished", the framebuffer to close is not the back framebuffer.
        if (!finished || !Objects.equals(framebuffer1, backFramebuffer))
        {
            framebuffer1.close();
        }
        else if (!finished || !Objects.equals(framebuffer2, backFramebuffer))
        {
            framebuffer2.close();
        }
    }

    public void addSetupCallback(BiConsumer<Program<ContextType>, FramebufferObject<ContextType>> callback)
    {
        setupCallbacks.add(callback);
    }

    public void addPostUpdateCallback(Consumer<FramebufferObject<ContextType>> callback)
    {
        postUpdateCallbacks.add(callback);
    }

    public FramebufferObject<ContextType> getFrontFramebuffer()
    {
        return frontFramebuffer;
    }

    public ReadonlyErrorReport runOnce(Function<Texture<ContextType>, ReadonlyErrorReport> errorCalculator)
    {
        if (finished)
        {
            throw new IllegalStateException("Attempt to run optimization after finish() has been called.");
        }

        // Opportunity to set up shader parameters and uniform inputs and clear the framebuffer if necessary.
        for (BiConsumer<Program<ContextType>, FramebufferObject<ContextType>> callback : setupCallbacks)
        {
            callback.accept(estimationProgram, backFramebuffer);
        }

        // Estimate a new solution.
        // Run shader program to fill framebuffer with per-pixel information.
        estimationDrawable.draw(PrimitiveMode.TRIANGLES, backFramebuffer);

        // New estimate becomes the new front framebuffer
        swapFramebuffers();

        // Notify callbacks that the estimate has been updated.
        for (Consumer<FramebufferObject<ContextType>> callback : postUpdateCallbacks)
        {
            callback.accept(frontFramebuffer);
        }

        // Check error
        ReadonlyErrorReport report = errorCalculator.apply(frontFramebuffer.getColorAttachmentTexture(0));

        if (report.getError() > report.getPreviousError())
        {
            // Error is worse; reject new estimate.
            // Swap framebuffers back to use the old estimate, if the new one isn't better.
            swapFramebuffers();
        }

        return report;
    }

    public void runUntilConvergence(Function<Texture<ContextType>, ReadonlyErrorReport> errorCalculator,
        double convergenceTolerance, int unsuccessfulIterationsAllowed)
    {
        int unsuccessfulIterations = 0;

        ReadonlyErrorReport report;

        do
        {
            report = runOnce(errorCalculator);

            if (report.getPreviousError() - report.getError() <= convergenceTolerance)
            {
                unsuccessfulIterations++;
            }
            else
            {
                unsuccessfulIterations = 0;
            }
        }
        while (report.getError() <= report.getPreviousError() && unsuccessfulIterations < unsuccessfulIterationsAllowed);
    }

    private void swapFramebuffers()
    {
        FramebufferObject<ContextType> tmp = frontFramebuffer;
        frontFramebuffer = backFramebuffer;
        backFramebuffer = tmp;
    }
}
