package modules.dataFeeder.rpkiFeeder;

import modules.dataFeeder.Feeder;

import java.net.URL;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RpkiFeeder implements Runnable {
    private static RpkiFeeder instance = null;
    private RpkiValidatorControlThread rpkiValidatorControlThread = null;
    private Lock aLock = new ReentrantLock();
    private Condition condVar = aLock.newCondition();
    private Thread thread_handle = null;

    private RpkiFeeder(){
    }

    public static RpkiFeeder getInstance() {
        if (instance == null) {
            instance = new RpkiFeeder();
        }
        return instance;
    }

    public void startRpkiValidator() {
        URL url = getClass().getResource("/libs/rpki-validator-3/rpki-validator-3.0.0-SNAPSHOT.jar");
        rpkiValidatorControlThread = new RpkiValidatorControlThread(url.getPath());
        if (thread_handle == null) {
            thread_handle = new Thread(this);
            thread_handle.start();
        }

    }

    public void startRpkiRepoDownload(){
        RpkiRepoDownloader rpkiRepoDownloader = new RpkiRepoDownloader();
        rpkiRepoDownloader.run();
    }

    public void close() {
        rpkiValidatorControlThread.stop();
//        condVar.signal();
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
