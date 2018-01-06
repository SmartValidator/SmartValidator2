import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import modules.conflictHandler.ConflictHandler;
import modules.conflictSeeker.ConflictSeeker;
import modules.dataFeeder.Feeder;
import modules.dataFeeder.risFeeder.BgpRisFeederControlThread;
import modules.dataFeeder.rpkiFeeder.RpkiFeeder;
import modules.helper.DbHandler;
import modules.helper.options.OptionsHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SmartValidator {
    private static final boolean SIMULATOR_OPERATION_MODE = true;
    private static final boolean DEFAULT_OPERATION_MODE = SIMULATOR_OPERATION_MODE;
    private static final int NTHREADS = 3;
    private static final ExecutorService executor
            = Executors.newFixedThreadPool(NTHREADS);
    private static ReentrantLock onGoingValidationRun = new ReentrantLock();
    private static Condition onGoingValidationRunCondition = onGoingValidationRun.newCondition();
    private static HttpServer server = null;
    private static ScheduledExecutorService scheduler = null;
    public static void main(String args[])  {
        scheduler = new ScheduledThreadPoolExecutor(2);

        try {
            //Init feeder and startRpkiValidator raw data information flowing
            Feeder.getInstance().startRawDataFeed();
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
            ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(main, 0, 300, TimeUnit.MINUTES);
            backendServer();
            scheduledFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private static void regularUpdate() throws ExecutionException, InterruptedException {

        Future<Void> conflictArchivationTask = null;
        Future<Void> resolvingArchivationTask = null;
        Future<?> bgpRisDownloadTask = null;
        try {
            onGoingValidationRun.lock();
            bgpRisDownloadTask = executor.submit(new BgpRisFeederControlThread());
            RpkiFeeder.getInstance().startRpkiRepoDownload();
//            conflictArchivationTask = executor.submit(new ConflictArchiver()); //TODO make sure base tables arent empty
//            resolvingArchivationTask = executor.submit(new ResolverArchiver());
            bgpRisDownloadTask.get();

            ConflictSeeker conflictSeeker = new ConflictSeeker();
            ConflictHandler conflictHandler = new ConflictHandler();
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
            onGoingValidationRun.unlock();
        }


    }

    private static boolean isSimulatorMode() throws ExecutionException {
        boolean settings = DEFAULT_OPERATION_MODE;

        try {
            Connection connection = DbHandler.produceConnection();
            ResultSet rs = connection.createStatement().executeQuery("SELECT value FROM settings " +
                    "WHERE key IN ('simulator.mode')");
            rs.next();
            int result = Integer.parseInt(rs.getString("value"));
            if(result == 1){
                settings = true;
            } else if (result == 0){
                settings = false;
            } else {
                throw new NumberFormatException("the mode setting doesn't match any option");
            }
        } catch (SQLException | NumberFormatException e) {
            throw new ExecutionException(e);
        }
        return settings;
    }

    private static void backendServer() throws IOException {

        Thread handle = new Thread(() -> {
            try {
                server = HttpServer.create(new InetSocketAddress(OptionsHandler.getInstance().getOptions().getGeneral().getBackendPort()), 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            MainHandler mainHandler  = new MainHandler();
            server.createContext("/modeChange", mainHandler);
            server.createContext("/shutdown", mainHandler);
            server.setExecutor(executor); // creates a default executor
            server.start();
        });
        handle.start();
    }

    private static void shutdown(){
        server.stop(5);
        if(scheduler != null){
            scheduler.shutdown();
        }
        Feeder.getInstance().stopRawDataFeed();

    }

    static class MainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            switch (httpExchange.getHttpContext().getPath()) {
                case "/modeChange":
                    onGoingValidationRun.lock();
                    try {
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
                    } finally {
                        sendResponse(httpExchange, "ack");
                        onGoingValidationRun.unlock();
                    }
                    break;
                case "/shutdown":

                    sendResponse(httpExchange, "ack");
                    break;
                default:
                    sendResponse(httpExchange, "unknown command");

                    break;
            }
        }

        void sendResponse(HttpExchange httpExchange, String response) throws IOException {
            response = "{ message : " + response + " }";
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}
