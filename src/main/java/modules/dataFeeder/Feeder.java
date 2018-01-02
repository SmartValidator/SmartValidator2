package modules.dataFeeder;

import java.net.URL;

public class Feeder {
    private static Feeder instance = null;
    private RpkiFeederControlThread rpkiFeederControlThread = null;

    private Feeder() {
    }

    public void start() {
        URL url = getClass().getResource("libs/rpki-validator-3/rpki-validator-3.0.0-SNAPSHOT.jar");
        rpkiFeederControlThread = new RpkiFeederControlThread(url.getPath());
        rpkiFeederControlThread.start();
    }

    public void close() {
        rpkiFeederControlThread.stop();
    }

    public static Feeder getInstance() {
        if (instance == null) {
            instance = new Feeder();
        }
        return instance;
    }
}
