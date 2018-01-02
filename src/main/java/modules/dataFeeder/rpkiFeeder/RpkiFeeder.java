package modules.dataFeeder.rpkiFeeder;

import modules.dataFeeder.Feeder;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RpkiFeeder {
    private static RpkiFeeder instance = null;
    private RpkiValidatorControlThread rpkiValidatorControlThread = null;
    private ScheduledExecutorService scheduledThreadPool;

    private RpkiFeeder(){

        scheduledThreadPool = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        URL url = getClass().getResource("libs/rpki-validator-3/rpki-validator-3.0.0-SNAPSHOT.jar");
        rpkiValidatorControlThread = new RpkiValidatorControlThread(url.getPath());
        rpkiValidatorControlThread.start();
        //TODO: check that this started succesfuly
        scheduledThreadPool.scheduleAtFixedRate(new RpkiRepoDownloader(), 500, 60, TimeUnit.MINUTES);
    }

    public void close() {
        scheduledThreadPool.shutdown();
        rpkiValidatorControlThread.stop();
    }

    public static RpkiFeeder getInstance() {
        if (instance == null) {
            instance = new RpkiFeeder();
        }
        return instance;
    }
}
