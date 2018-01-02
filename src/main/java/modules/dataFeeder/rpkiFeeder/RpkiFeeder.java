package modules.dataFeeder.rpkiFeeder;

import modules.dataFeeder.Feeder;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RpkiFeeder implements Runnable {
    private static RpkiFeeder instance = null;
    private RpkiValidatorControlThread rpkiValidatorControlThread = null;
    private ScheduledExecutorService scheduledThreadPool = null;
    private Lock aLock = new ReentrantLock();
    private Condition condVar = aLock.newCondition();
    private Thread thread_handle = null;

    private RpkiFeeder(){

        scheduledThreadPool = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        URL url = getClass().getResource("/libs/rpki-validator-3/rpki-validator-3.0.0-SNAPSHOT.jar");
//        rpkiValidatorControlThread = new RpkiValidatorControlThread(url.getPath());
//        rpkiValidatorControlThread.start();
        //TODO: check that this started succesfuly
        scheduledThreadPool.scheduleAtFixedRate(new RpkiRepoDownloader(), 0, 5, TimeUnit.MINUTES);
        System.out.println("Starting rpki feeder scheduled ");
        if (thread_handle == null) {
            thread_handle = new Thread(this);
            thread_handle.start();
        }
    }

    public void close() {
        scheduledThreadPool.shutdown();
        rpkiValidatorControlThread.stop();
        condVar.signal();
    }

    public static RpkiFeeder getInstance() {
        if (instance == null) {
            instance = new RpkiFeeder();
        }
        return instance;
    }

    @Override
    public void run() {
        aLock.lock();
        try {
            condVar.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            aLock.unlock();
        }
    }
}
