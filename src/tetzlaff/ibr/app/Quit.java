package tetzlaff.ibr.app;//Created by alexk on 8/11/2017.

import java.util.LinkedList;
import java.util.List;

import javafx.event.EventHandler;
import javafx.stage.WindowEvent;
import tetzlaff.ibr.util.Trigger;


public class Quit implements Trigger, EventHandler<WindowEvent> {
    private List<Trigger> quitTriggers = new LinkedList<>();
    public void addCloseTrigger(Trigger trigger){
        quitTriggers.add(trigger);
    }
    @Override
    public void handle(WindowEvent event) {
        this.trigger();
    }
    @Override
    public void trigger() {
        for(Trigger trigger : quitTriggers){
            trigger.trigger();
        }
    }

    private Quit() {
    }
    private static Quit instance = new Quit();
    public static Quit getInstance() {
        return instance;
    }
}
