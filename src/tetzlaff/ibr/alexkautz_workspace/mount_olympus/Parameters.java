package tetzlaff.ibr.alexkautz_workspace.mount_olympus;

public class Parameters {
    //local parameters

    private String name;

    private String filepath;

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

    private Parameters(String filepath, String name) {
        this.filepath = filepath;
        this.name = name;
    }

    //static bridge

    private static Parameters parameters;

    public static void init(String filepath, String name){
        parameters = new Parameters(filepath, name);
    }

    public static Parameters get() {
        return parameters;
    }
}
