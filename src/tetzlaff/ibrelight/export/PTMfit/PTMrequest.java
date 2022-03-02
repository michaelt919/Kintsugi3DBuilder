package tetzlaff.ibrelight.export.PTMfit;

import tetzlaff.gl.core.Context;
import tetzlaff.ibrelight.core.IBRRenderable;
import tetzlaff.ibrelight.core.IBRRequest;
import tetzlaff.ibrelight.core.LoadingMonitor;
import tetzlaff.ibrelight.core.TextureFitSettings;

import java.io.IOException;

public class PTMrequest<ContextType extends Context<ContextType>> implements IBRRequest<ContextType> {

    private TextureFitSettings setting;
    public PTMrequest(TextureFitSettings settings){
        setting = settings;
    }

    @Override
    public void executeRequest(IBRRenderable<ContextType> renderable, LoadingMonitor callback) throws Exception
    {
        try
        {
            new PTMOptimization<ContextType>(setting).createFit(renderable.getResources());
        }
        catch(IOException e) // thrown by createReflectanceProgram
        {
            e.printStackTrace();
        }
    }
}
