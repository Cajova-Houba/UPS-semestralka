package org.valesz.ups.common;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.valesz.ups.controller.GameController;

/**
 * @author Zdenek Vales
 */
public class TimerThread extends Task<Void>{

    /**
     * Second in ms.
     */
    public static final int SECOND = 1000;

    /**
     * In s.
     */
    private final int maxTime;

    private StringProperty passedTime = new SimpleStringProperty();

    public final String getPassedTime() {return passedTime.get();}

    public final void setPassedTime(int passedSeconds) {
        int time = GameController.MAX_TURN_TIME - passedSeconds;
        if(time < 0) {
            time = 0;
        }
        int minutes = time / 60;
        time = time - minutes*60;
        passedTime.setValue(String.format("%02d : %02d",minutes, time));
    }

    public TimerThread(int maxTime) {
        this.maxTime = maxTime;
    }

    @Override
    protected Void call() throws Exception {
        int cntr = 0;

        while(cntr <= maxTime) {
            try {
                Thread.sleep(SECOND);
            } catch (InterruptedException ex) {
                break;
            }

            if(Thread.currentThread().isInterrupted()) {
                break;
            }

            setPassedTime(cntr);

            cntr += 1;
        }

        return null;
    }
}
