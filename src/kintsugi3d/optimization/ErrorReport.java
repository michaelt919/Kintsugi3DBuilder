/*
 *  Copyright (c) Michael Tetzlaff 2022
 *
 *  Licensed under GPLv3
 *  ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 *  This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.optimization;

public class ErrorReport extends ReadonlyErrorReport
{
    public ErrorReport(int sampleCount)
    {
        super(sampleCount);
    }

    @Override
    public void setError(double newError)
    {
        super.setError(newError);
    }

    @Override
    public void reject()
    {
        super.reject();
    }
}
