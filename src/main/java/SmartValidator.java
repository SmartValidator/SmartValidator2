import modules.archiver.ConflictArchiver;
import modules.archiver.ResolverArchiver;
import modules.conflictHandler.ConflictHandler;
import modules.conflictSeeker.ConflictSeeker;
import modules.dataFeeder.Feeder;
import modules.dataFeeder.rpkiFeeder.RpkiFeeder;
import modules.helper.DbHandler;
import modules.helper.options.OptionsHandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.*;

public class SmartValidator {
    private static final boolean SIMULATOR_OPERATION_MODE = true;
    private static final boolean DEFAULT_OPERATION_MODE = SIMULATOR_OPERATION_MODE;
    private static final int NTHREADS = 5;
    private static final ExecutorService executor
            = Executors.newFixedThreadPool(NTHREADS);


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
    }

    private static void regularUpdate() throws ExecutionException, InterruptedException {
        Future<Void> conflictArchivationTask = null;
        Future<Void> resolvingArchivationTask = null;

        try {
            RpkiFeeder.getInstance().startRpkiRepoDownload();
            conflictArchivationTask = executor.submit(new ConflictArchiver()); //TODO make sure base tables arent empty
            resolvingArchivationTask = executor.submit(new ResolverArchiver());
            ConflictSeeker conflictSeeker = new ConflictSeeker();
            ConflictHandler conflictHandler = new ConflictHandler();
            conflictSeeker.run();
            conflictHandler.run();



            if(isSimulatorMode()){

            } else {

            }


        } finally {
            if (conflictArchivationTask != null) {
                conflictArchivationTask.get();
            }
            if (resolvingArchivationTask != null) {
                resolvingArchivationTask.get();
            }
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
