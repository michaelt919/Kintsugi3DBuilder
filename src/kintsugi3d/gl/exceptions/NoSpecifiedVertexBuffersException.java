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

package kintsugi3d.gl.exceptions;

public class NoSpecifiedVertexBuffersException extends RuntimeException {

    private static final long serialVersionUID = 6824841077784662947L;

    public NoSpecifiedVertexBuffersException()
    {
    }

    public NoSpecifiedVertexBuffersException(String message)
    {
        super(message);
    }

    public NoSpecifiedVertexBuffersException(Throwable cause)
    {
        super(cause);
    }

    public NoSpecifiedVertexBuffersException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public NoSpecifiedVertexBuffersException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
