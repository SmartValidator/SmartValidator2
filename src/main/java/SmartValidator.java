import modules.archiver.ConflictArchiver;
import modules.archiver.ResolverArchiver;
import modules.conflictHandler.ConflictHandler;
import modules.conflictSeeker.ConflictSeeker;
import modules.dataFeeder.Feeder;
import modules.dataFeeder.risFeeder.BgpRisFeederControlThread;
import modules.dataFeeder.rpkiFeeder.RpkiFeeder;
import modules.helper.DbHandler;

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


    public static void main(String args[])  {
        ScheduledExecutorService scheduler = null;
        try {
            //Init feeder and startRpkiValidator raw data information flowing
            Feeder.getInstance().startRawDataFeed();
            ScheduledFuture<?> scheduledFuture;
            scheduler = new ScheduledThreadPoolExecutor(1);
            Thread main = new Thread(() -> {
                try {
                    regularUpdate();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            main.setUncaughtExceptionHandler((t, e) -> {
                throw new RuntimeException(e);
            });
            scheduledFuture = scheduler.scheduleAtFixedRate(main, 0, 180, TimeUnit.SECONDS);
            scheduledFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            if(scheduler != null){
                scheduler.shutdown();
                Feeder.getInstance().stopRawDataFeed();
            }
        }
    }

    private static void regularUpdate() throws ExecutionException, InterruptedException {
        Future<Void> conflictArchivationTask = null;
        Future<Void> resolvingArchivationTask = null;
        Future<?> bgpRisDownloadTask = null;
        try {
//            bgpRisDownloadTask = executor.submit(new BgpRisFeederControlThread());
            RpkiFeeder.getInstance().startRpkiRepoDownload();
            conflictArchivationTask = executor.submit(new ConflictArchiver()); //TODO make sure base tables arent empty
            resolvingArchivationTask = executor.submit(new ResolverArchiver());
//            bgpRisDownloadTask.get();

            ConflictSeeker conflictSeeker = new ConflictSeeker();
            ConflictHandler conflictHandler = new ConflictHandler();
//                    Integer a = Integer.parseInt("XTX");
            conflictSeeker.run();
            conflictHandler.run();


            if (isSimulatorMode()) {

            } else {

            }


        } finally {
            if (conflictArchivationTask != null) {
                conflictArchivationTask.get();
            }
            if (resolvingArchivationTask != null) {
                resolvingArchivationTask.get();
            }
            if (bgpRisDownloadTask != null) {
                bgpRisDownloadTask.get();
            }
        }


    }

    private static boolean isSimulatorMode() throws ExecutionException {
        boolean settings = DEFAULT_OPERATION_MODE;

        try {
            Connection connection = DbHandler.produceConnection();
            ResultSet rs = connection.createStatement().executeQuery("SELECT value FROM settings " +
                    "WHERE key IN ('simulator_mode')");
            rs.next();
            settings = Boolean.parseBoolean(rs.getString("value"));
        } catch (SQLException | NumberFormatException e) {
            throw new ExecutionException(e);
        }
        return settings;
    }

}
