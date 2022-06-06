/*
 *  Copyright (c) Zhangchi (Josh) Lyu, Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package tetzlaff.ibrelight.export.PTMfit;


import tetzlaff.util.ColorList;

public class LuminanceData
{
    private int size;
    private ColorList lumin;
    private ColorList lightdir;

    public LuminanceData(ColorList lumin, ColorList lightdir){
        this.lumin=lumin;
        this.lightdir=lightdir;
        this.size= lumin.size();
    }

    public ColorList getLumin() {
        return lumin;
    }
    public ColorList getLightdir() {
        return lightdir;
    }

    public int getsize(){
        return this.size;
    }

}
