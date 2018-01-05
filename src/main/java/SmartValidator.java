import modules.conflictHandler.ConflictHandler;
import modules.conflictSeeker.ConflictSeeker;
import modules.dataFeeder.Feeder;
import modules.dataFeeder.rpkiFeeder.RpkiFeeder;
import modules.helper.options.OptionsHandler;

import java.util.concurrent.ExecutionException;

public class SmartValidator {
    public static void main(String args[]) {

        try {
            //Init option handler
            OptionsHandler optionsHandler = OptionsHandler.getInstance();
            //Init feeder and startRpkiValidator raw data information flowing
            Feeder.getInstance().startRawDataFeed();

            regularUpdate();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        //Init thread worker pool
//        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);
//        scheduledThreadPool.scheduleAtFixedRate(new SimulatorHook(), 15, 180, TimeUnit.MINUTES);


    }

    private static void regularUpdate() throws ExecutionException {
        try {
            RpkiFeeder.getInstance().startRpkiRepoDownload();
            ConflictSeeker conflictSeeker = new ConflictSeeker();
            ConflictHandler conflictHandler = new ConflictHandler();
            conflictSeeker.start();
                conflictSeeker.run();
                conflictHandler.run();
        } finally {
        }



    }
}
