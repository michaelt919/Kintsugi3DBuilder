package tetzlaff.mvc.models;//Created by alexk on 7/24/2017.

import java.io.File;
import java.io.IOException;

import tetzlaff.ibr.IBRLoadOptions;
import tetzlaff.ibr.IBRRenderable;
import tetzlaff.ibr.rendering.ImageBasedRendererList;
import tetzlaff.ibr.rendering2.to_sort.IBRLoadOptions2;
import tetzlaff.ibr.rendering2.tools2.ToolBox;

public abstract class ControllableToolModel {
    private IBRLoadOptions2 loadOptions = new IBRLoadOptions()
            .setColorImagesRequested(true)
            .setCompressionRequested(true)
            .setMipmapsRequested(true)
            .setDepthImagesRequested(false)
            .setDepthImageHeight(1024)
            .setDepthImageWidth(1024);

    public void setLoadOptions(IBRLoadOptions2 loadOptions) {
        this.loadOptions = loadOptions;
    }

    private ImageBasedRendererList<?> model;
    public final void setModel(ImageBasedRendererList<?> model) {
        this.model = model;
    }

    public final void loadFiles(File cameraFile, File objFile, File photoDir) throws IOException{


        IBRRenderable<?> ibrRenderable = model.addFromAgisoftXMLFile(cameraFile.getPath(), cameraFile, objFile, photoDir, loadOptions);

        //TODO remove temp-def.
        ibrRenderable.setHalfResolution(true);
    }

    final void loadEV(File ev){
        model.getSelectedItem().setEnvironment(ev);
    }

    final void unloadEV(){
        model.getSelectedItem().setEnvironment(null);
    }

    public abstract ToolBox.TOOL getTool();
}
