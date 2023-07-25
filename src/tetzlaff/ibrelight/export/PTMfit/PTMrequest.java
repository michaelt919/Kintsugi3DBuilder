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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.IBRInstance;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.TextureFitSettings;

import java.io.File;
import java.io.IOException;

public class PTMrequest<ContextType extends Context<ContextType>> implements IBRRequest<ContextType> {

    private static final Logger log = LoggerFactory.getLogger(PTMrequest.class);

    private final TextureFitSettings setting;
    private final File outputDirectory;
    public PTMrequest(TextureFitSettings settings, File outputDirectory){
        setting = settings;

        if (outputDirectory == null)
        {
            throw new IllegalArgumentException("Output directory cannot be null.");
        }
        else
        {
            this.outputDirectory = outputDirectory;
        }
    }

    @Override
    public void executeRequest(IBRInstance<ContextType> renderable, LoadingMonitor callback) throws Exception
    {
        try
        {
            new PTMOptimization<ContextType>(setting).createFit(renderable.getIBRResources(), outputDirectory);
        }
        catch(IOException e) // thrown by createReflectanceProgram
        {
            log.error("Error occurred executing request:", e);
        }
    }
}
