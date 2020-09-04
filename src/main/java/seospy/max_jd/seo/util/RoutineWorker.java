package seospy.max_jd.seo.util;

import javax.swing.SwingWorker;

public class RoutineWorker extends SwingWorker<Void, Void> {
    private Routine routine;

    public RoutineWorker(Routine routine) {
        this.routine = routine;
    }

    @Override
    protected Void doInBackground() throws Exception {
        routine.doWork();
        return null;
    }
}