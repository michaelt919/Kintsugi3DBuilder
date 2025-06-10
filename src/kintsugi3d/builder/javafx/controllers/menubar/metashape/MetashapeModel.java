package kintsugi3d.builder.javafx.controllers.menubar.metashape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Objects;
import java.util.Optional;

public class MetashapeModel {
    private static final Logger log = LoggerFactory.getLogger(MetashapeModel.class);
    private MetashapeChunk chunk;
    private Optional<Integer> id;
    private String label;
    private String path;

    private MetashapeModel(MetashapeChunk chunk, Optional<Integer> id, String label, String path){
        this.id = id;
        this.label = label;
        this.path = path;
        this.chunk = chunk;
    }

    public static MetashapeModel parseFromElement(MetashapeChunk chunk, Element elem) {
        Optional<Integer> tempModelID = Optional.empty();
        String tempLabel = null;
        try{
            tempModelID = Optional.of(Integer.parseInt(elem.getAttribute("id")));
        }
        catch(NumberFormatException nfe){
            log.warn("Model has no id", nfe);
        }

        try{
            tempLabel = elem.getAttribute("label");
        }
        catch(NumberFormatException nfe){
            log.warn("Model has no label", nfe);
        }

        String path = "";
        //  <model id="0" path="model.1/model.zip"/> --> returns "model.1/model.zip"

        try{
            NodeList elems = ((Element) chunk.getFrameXML().getElementsByTagName("frame").item(0))
                    .getElementsByTagName("model");

            //this if statement triggers if chunk has one model and that model has no id
            if (elems.getLength() == 1 &&
                    ((Element) elems.item(0)).getAttribute("id").isEmpty()) {
                path = ((Element) elems.item(0)).getAttribute("path");
            } else {
                for (int i = 0; i < elems.getLength(); i++) {
                    Element element = (Element) elems.item(i);

                    if (Objects.equals(element.getAttribute("id"), String.valueOf(tempModelID.get()))) {
                        path = element.getAttribute("path");
                        break;
                    }
                }
            }

        }
        catch(NullPointerException e){
            //ignore, no path was found
        }

        //if no model info yet, might be a single model in chunk which isn't labeled in chunk xml
        //need to look in frame xml instead
        //default to looking in frame xml first?
        //ex. mia arrowhead
        //TODO: look into making arrowhead work another way
//        if (modelInfo.isEmpty()){
//            //TODO: needs more work, this is a quick hack to get arrowhead to work
//            String path = getModelPathFromXML(null);
//            if (!path.isBlank()) {
//                modelInfo.add(new Triplet<>(null, "", path));
//            }
//        }

        return new MetashapeModel(chunk, tempModelID, tempLabel, path);
    }

    public Optional<Integer> getId(){
        return id;
    }

    public String getPath(){
        return path;
    }

    public String getLabel(){
        return label;
    }
}
