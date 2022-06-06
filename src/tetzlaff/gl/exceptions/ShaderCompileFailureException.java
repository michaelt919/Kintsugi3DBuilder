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

package tetzlaff.gl.exceptions;

public class ShaderCompileFailureException extends RuntimeException 
{
    private static final long serialVersionUID = 7556469381337373536L;

    public ShaderCompileFailureException()
    {
    }

    public ShaderCompileFailureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ShaderCompileFailureException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ShaderCompileFailureException(String message)
    {
        super(message);
    }

    public ShaderCompileFailureException(Throwable cause)
    {
        super(cause);
    }
}
