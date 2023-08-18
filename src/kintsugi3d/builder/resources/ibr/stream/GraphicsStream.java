/*
 * Copyright (c) 2019 - 2023 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 */

package kintsugi3d.builder.resources.ibr.stream;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An interface that provides a small subset of the functionality of the standard Java Stream API.
 * Intended for when graphics operations will be part of the stream and parallelism is required so that the standard API is insufficient.
 * @param <T> The type of objects produced by this stream.
 */
public interface GraphicsStream<T>
{
    /**
     * Returns an equivalent stream that is sequential. May return itself, either because the stream was already
     * sequential, or because the underlying stream state was modified to be sequential.
     * @return a sequential stream
     */
    GraphicsStream<T> sequential();

    /**
     * Returns an equivalent stream that is parallel. May return itself, either because the stream was already parallel,
     * or because the underlying stream state was modified to be parallel.
     * This is an intermediate operation.
     * @return a parallel stream
     */
    GraphicsStream<T> parallel();

    /**
     * Returns an equivalent stream that is parallel. May return itself, either because the stream was already parallel,
     * or because the underlying stream state was modified to be parallel.
     * This is an intermediate operation.
     * @return a parallel stream
     */
    GraphicsStream<T> parallel(int maxRunningThreads);

    /**
     * Gets the count of elements in this stream.  Unlike count() in the Java 8 Stream API, this is not technically a
     * terminal operation in that it does not consume the stream pipeline.
     * @return the count of elements in this stream
     */
    int getCount();

    /**
     * Performs an action for each element of this stream.
     * This is a terminal operation.
     * @param action a non-interfering action to perform on the elements
     */
    void forEach(Consumer<? super T> action);

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation.
     * @param mapper a non-interfering, stateless function to apply to each element
     * @param <R> The element type of the new stream
     * @return the new stream
     */
    <R> GraphicsStream<R> map(Function<T, ? extends R> mapper);

    /**
     * Performs a mutable reduction operation on the elements of this stream.
     * For more information, please refer to the documentation for Stream.collect in the Java 8 API.
     * Unlike the Java 8 API, no combiner is required for this function.
     * This is a terminal operation.
     * @param supplier a function that creates a new result container. For a parallel execution, this function may be
     *                 called multiple times and must return a fresh value each time.
     * @param accumulator an associative, non-interfering, stateless function for incorporating an additional element
     *                    into a result
     * @param <R> type of the result
     * @return the result of the reduction
     */
    <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator);
}
