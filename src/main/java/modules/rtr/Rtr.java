package modules.rtr;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Rtr implements Runnable {


        private static Rtr instance = null;
        private RtrControlThread bmpFeederControlThread = null;
        private ScheduledExecutorService scheduledThreadPool = null;
        private Lock aLock = new ReentrantLock();
        private Condition condVar = aLock.newCondition();
        private Thread thread_handle = null;

        private Rtr(){

            scheduledThreadPool = Executors.newScheduledThreadPool(1);
        }

        public void start() {
            URL url = getClass().getResource("/libs/rtr/rtrd");
            bmpFeederControlThread = new RtrControlThread(url.getPath());
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

        public static Rtr getInstance() {
            if (instance == null) {
                instance = new Rtr();
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


