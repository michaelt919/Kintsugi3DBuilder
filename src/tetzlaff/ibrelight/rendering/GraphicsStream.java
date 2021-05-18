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

import tetzlaff.util.ColorList;

/**
 * An interface that provides a small subset of the functionality of the standard Java Stream API.
 * Intended for when graphics operations will be part of the stream and parallelism is required so that the standard API is insufficient.
 * @param <T> The type of objects produced by this stream.
 */
public interface GraphicsStream<T>
{
    GraphicsStream<T> sequential();
    GraphicsStream<T> parallel();
    GraphicsStream<T> parallel(int maxRunningThreads);
    int count();
    void forEach(Consumer<? super T> action);
    <R> GraphicsStream<R> map(Function<T, ? extends R> mapper);
    <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator);
}
