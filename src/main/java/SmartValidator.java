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
                //push
            } else {

            }
        } finally {
        }



    }

    private static boolean isSimulatorMode() throws ExecutionException {
        int settings = -1;

//        try {
//            Connection connection = DbHandler.produceConnection();
//            if(connection)
//            ResultSet rs = connection.createStatement().executeQuery("SELECT value FROM settings " +
//                    "WHERE key IN ('conflictHandler.heuristic')");
//            rs.next();
//            settings[0] = Integer.parseInt(rs.getString("value"));
//            rs.next();
//            settings[1] = Integer.parseInt(rs.getString("value"));
//        } catch (SQLException | NumberFormatException e) {
//            throw new ExecutionException(e);
//        } finally {
//            if(settings[0] < 0 || settings[0] > 3) {
//                settings[0] = 0;
//            }
//            if(settings[1] < 0) {
//                settings[1] = 0;
//            } else if(settings[1] > 32) {
//                settings[1] = 32;
//            }
//        }
        return false;
    }
}
