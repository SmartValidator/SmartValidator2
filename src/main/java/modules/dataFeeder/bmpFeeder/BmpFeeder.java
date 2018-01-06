package modules.dataFeeder.bmpFeeder;

import modules.dataFeeder.rpkiFeeder.RpkiFeeder;
import modules.dataFeeder.rpkiFeeder.RpkiRepoDownloader;
import modules.dataFeeder.rpkiFeeder.RpkiValidatorControlThread;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BmpFeeder  implements Runnable {


        private static BmpFeeder instance = null;
        private BmpFeederControlThread bmpFeederControlThread = null;
        private ScheduledExecutorService scheduledThreadPool = null;
        private Lock aLock = new ReentrantLock();
        private Condition condVar = aLock.newCondition();
        private Thread thread_handle = null;

        private BmpFeeder(){

            scheduledThreadPool = Executors.newScheduledThreadPool(1);
        }

        public void start() {
            URL url = getClass().getResource("/libs/Bmp/bin/yabmpd");
            bmpFeederControlThread = new BmpFeederControlThread(url.getPath());
            //TODO: check that this started succesfuly
            scheduledThreadPool.scheduleAtFixedRate(bmpFeederControlThread, 500, 60, TimeUnit.MINUTES);
            System.out.println("Starting ");
            if (thread_handle == null) {
                thread_handle = new Thread(this);
                thread_handle.start();
            }
        }

        public void close() {
            scheduledThreadPool.shutdown();
            aLock.lock();
            condVar.signal();
            aLock.unlock();
        }

        public static BmpFeeder getInstance() {
            if (instance == null) {
                instance = new BmpFeeder();
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


