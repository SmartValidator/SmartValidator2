import modules.conflictHandler.ConflictHandler;
import modules.conflictSeeker.ConflictSeeker;
import modules.dataFeeder.Feeder;
import modules.dataFeeder.rpkiFeeder.RpkiFeeder;
import modules.helper.DbHandler;
import modules.helper.options.OptionsHandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class SmartValidator {
    private static final boolean SIMULATOR_OPERATION_MODE = true;
    private static final boolean DEFAULT_OPERATION_MODE = SIMULATOR_OPERATION_MODE;

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
            if(isSimulatorMode()){

            } else {

            }
        } finally {
        }



    }

    private static boolean isSimulatorMode() throws ExecutionException {
        boolean settings = DEFAULT_OPERATION_MODE;

        try {
            Connection connection = DbHandler.produceConnection();
            ResultSet rs = connection.createStatement().executeQuery("SELECT value FROM settings " +
                    "WHERE key IN ('conflictHandler.heuristic')");
            rs.next();
            settings = Boolean.parseBoolean(rs.getString("value"));
        } catch (SQLException | NumberFormatException e) {
            throw new ExecutionException(e);
        }
        return settings;
    }
}
