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
    private static final int ARR_SIZE = 15;
    private final Long[] rollingAverageStorage = new Long[ARR_SIZE];
    private int trackingIdx = 0;

    private long initTime;
    private boolean running = false;

    //used to manually set elapsed time for a stopwatch which is stopped and
    //   has its elapsed time retrieved later
    private long manualElapsedTime = -1;


    public Stopwatch(){
    }

    public long start(){
        if(running){
            throw new IllegalStateException("Stopwatch object cannot be started multiple times.");
        }

        running = true;
        trackingIdx = 0;

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

    public long getElapsedTime() {
        if (running){
            //TODO: need to redo this if we want to support starting and stopping stopwatches
            return System.nanoTime() - initTime;
        }

        return manualElapsedTime;
    }

    public long stop() {
        running = false;
        manualElapsedTime = System.nanoTime() - initTime;

        return manualElapsedTime;
    }

    public long getAvgDifference(){
        long recordedTimes = 0;
        int numToDivide = 0;
        for(int i = 1; i < ARR_SIZE; ++i){
            if (rollingAverageStorage[i] == initTime){break;}

            long difference = rollingAverageStorage[i] - rollingAverageStorage[i-1];

            if (difference > 0){
                recordedTimes += difference;
                numToDivide++;
            }
        }

        return numToDivide == 0 ? 0 : recordedTimes / numToDivide;
    }
}