package tetzlaff.mvc.models;//Created by alexk on 7/24/2017.

import java.io.File;
import java.io.IOException;

import tetzlaff.ibr.IBRLoadOptions;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.ibr.rendering2.to_sort.IBRLoadOptions2;
import tetzlaff.ibr.rendering2.to_sort.IBRSettings2;
import tetzlaff.ibr.rendering2.tools2.ToolBox;

public abstract class ControllableToolModel {
    private IBRRenderable<?> ibrRenderable = null;
    protected abstract IBRSettings2 getSettings();
    protected abstract IBRLoadOptions2 getLoadOptions();



    private ImageBasedRendererList<?> model;
    public final void setModel(ImageBasedRendererList<?> model) {
        this.model = model;
    }

    public final void loadFiles(File cameraFile, File objFile, File photoDir) throws IOException{



        ibrRenderable = model.addFromAgisoftXMLFile(cameraFile.getPath(), cameraFile, objFile, photoDir, getLoadOptions());

        //TODO remove temp-def.
        ibrRenderable.setHalfResolution(true);

        ibrRenderable.setSettings(getSettings());

    }

    public final IBRRenderable<?> getIBRRenderable(){
        return ibrRenderable;
    }


    final void loadEV(File ev){
        model.getSelectedItem().setEnvironment(ev);
    }

    final void unloadEV(){
        model.getSelectedItem().setEnvironment(null);
    }

    public abstract ToolBox.TOOL getTool();
}
