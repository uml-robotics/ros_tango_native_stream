package edu.uml;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by emarcoux on 1/13/15.
 */
public class CancelableDelayedTask {
    private Timer timer;
    private TimerTaskInternal timerTask;
    private Runnable runnable;

    public CancelableDelayedTask(Runnable runnable) {
        this.timer = new Timer();
        this.runnable = runnable;
    }

    public boolean schedule(long delay) {
        synchronized (this) {
            if(timerTask != null) return false;

            timerTask = new TimerTaskInternal(runnable);
            timer.schedule(timerTask, delay);
            return true;
        }
    }

    public boolean cancel() {
        if(timerTask == null) return false;

        timerTask.cancel();
        timerTask = null;

        return true;
    }




    private class TimerTaskInternal extends TimerTask {
        private Runnable runnable;

        private TimerTaskInternal(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }

}
