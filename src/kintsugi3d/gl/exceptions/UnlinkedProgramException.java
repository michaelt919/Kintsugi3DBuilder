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

package kintsugi3d.gl.exceptions;

public class UnlinkedProgramException extends IllegalStateException 
{
    private static final long serialVersionUID = 7045695222299534322L;

    public UnlinkedProgramException()
    {
    }

    public UnlinkedProgramException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UnlinkedProgramException(String message)
    {
        super(message);
    }

    public UnlinkedProgramException(Throwable cause)
    {
        super(cause);
    }
}
