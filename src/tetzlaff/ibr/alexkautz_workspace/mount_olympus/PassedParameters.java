package tetzlaff.ibr.alexkautz_workspace.mount_olympus;

public class  PassedParameters {
    //local passedParameters

    private String name;

    private String filepath;

    private RenderPerams renderPerams;



    public RenderPerams getRenderPerams() {
        if(renderPerams == null) throw new IllegalStateException("RenderPerams got befor set!"); //KILLME
        return renderPerams;
    }

    public void setRenderPerams(RenderPerams renderPerams) {
        this.renderPerams = renderPerams;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    private PassedParameters(String filepath, String name) {
        this.filepath = filepath;
        this.name = name;
    }

    //static bridge

    private static PassedParameters passedParameters;

    public static void init(String filepath, String name){
        passedParameters = new PassedParameters(filepath, name);
    }

    public static PassedParameters get() {
        return passedParameters;
    }
}
