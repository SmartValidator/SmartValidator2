package modules.dataFeeder;

import modules.dataFeeder.bmpFeeder.BmpFeeder;
import modules.dataFeeder.rpkiFeeder.RpkiFeeder;

import java.util.concurrent.ExecutionException;

public class Feeder {
    private static Feeder instance = null;

    private Feeder() {
    }

    public void startRawDataFeed() throws ExecutionException, InterruptedException {
        RpkiFeeder.getInstance().startRpkiValidator();
        BmpFeeder.getInstance().start();

    }

    public void stopRawDataFeed() {
        RpkiFeeder.getInstance().close();
        BmpFeeder.getInstance().close();
    }

    public static Feeder getInstance() {
        if (instance == null) {
            instance = new Feeder();
        }
        return instance;
    }
}
