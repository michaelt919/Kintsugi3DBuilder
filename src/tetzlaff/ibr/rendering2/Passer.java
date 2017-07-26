package tetzlaff.ibr.rendering2;//Created by alexk on 7/20/2017.

public class Passer<T>{
    private T object;
    private boolean passed = false;

    public Passer(T object) {
        this.object = object;
    }

    public T getObject() {
        if(!passed){
            passed = true;
            return object;
        }
        else return null;
    }
}
