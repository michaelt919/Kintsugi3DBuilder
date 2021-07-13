package tetzlaff.ibrelight.export.PTMfit;

import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.TextureFitSettings;
import tetzlaff.ibrelight.export.PTMfit.PTMOptimiztion;

import java.io.IOException;

public class PTMrequest implements IBRRequest {

    private TextureFitSettings setting;
    public PTMrequest(TextureFitSettings settings){
        setting = settings;
    }

    @Override
    public <ContextType extends Context<ContextType>> void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback) throws Exception {
        try
        {
            new PTMOptimiztion<ContextType>(setting).createFit(renderable.getResources());
        }
        catch(IOException e) // thrown by createReflectanceProgram
        {
            e.printStackTrace();
        }
    }
}
