package modules.dataFeeder.rpkiFeeder;

import modules.dataFeeder.Feeder;

import java.net.URL;

public class RpkiFeeder {
    private static RpkiFeeder instance = null;
    private RpkiValidatorControlThread rpkiValidatorControlThread = null;

    private RpkiFeeder(){

    }

    public void start() {
        URL url = getClass().getResource("libs/rpki-validator-3/rpki-validator-3.0.0-SNAPSHOT.jar");
        rpkiValidatorControlThread = new RpkiValidatorControlThread(url.getPath());
        rpkiValidatorControlThread.start();
    }

    public void close() {
        rpkiValidatorControlThread.stop();
    }

    public static RpkiFeeder getInstance() {
        if (instance == null) {
            instance = new RpkiFeeder();
        }
        return instance;
    }
}
