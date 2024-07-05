/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.util;

import java.util.Arrays;

public class Stopwatch {
    private static final int ARR_SIZE = 30;
    private Long[] rollingAverageStorage = new Long[ARR_SIZE];
    private int trackingIdx = 0;

    private long initTime;
    private boolean running = false;


    public Stopwatch(){
    }

    public long start(){
        running = true;

        initTime = System.nanoTime();
        Arrays.fill(rollingAverageStorage, initTime);

        click();
        return initTime;
    }

    public long click() {
        rollingAverageStorage[trackingIdx] = System.nanoTime();

        int prevIndex = trackingIdx - 1;

        if (prevIndex <= -1){
            prevIndex = ARR_SIZE - 1;
        }

        long difference = rollingAverageStorage[trackingIdx] - rollingAverageStorage[prevIndex];
        trackingIdx = (trackingIdx + 1) % ARR_SIZE;
        return difference;
    }

    public long getInitTime() {return initTime;}

    public boolean isRunning(){return running;}

    public long getTotalElapsedTime() {return System.nanoTime() - initTime;}

    public void stop() {
        running = false;
    }
}
