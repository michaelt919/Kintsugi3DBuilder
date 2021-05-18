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

package tetzlaff.ibrelight.rendering;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An abstract base class to facilitate writing stream classes that contain graphics operations.
 * The only functions that the base class need provide are forEach() and count().
 * It is expected that forEach() may execute in parallel using multiple threads.
 * @param <T> The type of objects produced by this stream.
 */
public abstract class GraphicsStreamBase<T> implements GraphicsStream<T>
{
    @Override
    public <R> GraphicsStream<R> map(Function<T, ? extends R> mapper)
    {
        return new Mapped<>(this, mapper);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator)
    {
        R result = supplier.get();

        this.forEach(contribution ->
        {
            // Add the contribution into the main result.
            synchronized (result)
            {
                accumulator.accept(result, contribution);
            }
        });

        return result;
    }

    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    private static class Mapped<S, T> extends GraphicsStreamBase<T>
    {
        private final GraphicsStream<S> base;
        private final Function<S, ? extends T> mapper;

        Mapped(GraphicsStream<S> base, Function<S, ? extends T> mapper)
        {
            this.base = base;
            this.mapper = mapper;
        }

        @Override
        public <R> GraphicsStream<R> map(Function<T, ? extends R> mapper)
        {
            return new Mapped<>(this, mapper);
        }

        @Override
        public int count()
        {
            return base.count();
        }

        @Override
        public void forEach(Consumer<? super T> action)
        {
            // Apply the current mapping before executing the action.
            base.forEach(source -> action.accept(mapper.apply(source)));
        }
    }
}
