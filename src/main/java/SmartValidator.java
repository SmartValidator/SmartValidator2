import modules.conflictHandler.ConflictHandler;
import modules.conflictSeeker.ConflictSeeker;
import modules.dataFeeder.Feeder;
import modules.helper.options.OptionsHandler;
import modules.simulator.SimulatorHook;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SmartValidator {

    public static void main(String args[]){

        try {
            //Init option handler
            OptionsHandler optionsHandler = OptionsHandler.getInstance();
            //Init feeder and start raw data information flowing
            Feeder.getInstance().start();
            regularUpdate();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        //Init thread worker pool
//        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);
//        scheduledThreadPool.scheduleAtFixedRate(new SimulatorHook(), 15, 180, TimeUnit.MINUTES);


    }

    private static void regularUpdate() throws ExecutionException {
        ConflictSeeker conflictSeeker = new ConflictSeeker();
        ConflictHandler conflictHandler = new ConflictHandler();
        conflictSeeker.start();
        try {
            conflictSeeker.run();
            conflictHandler.run();
        } catch (ExecutionException e) {
            throw new ExecutionException(e);
        }


    }
}
