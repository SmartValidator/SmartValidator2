package modules.dataFeeder;

import modules.dataFeeder.bmpFeeder.BmpFeeder;
import modules.dataFeeder.rpkiFeeder.RpkiFeeder;
import modules.dataFeeder.rpkiFeeder.RpkiValidatorControlThread;

import java.net.URL;
import java.util.concurrent.ScheduledFuture;

public class Feeder {
    private static Feeder instance = null;

    private Feeder() {
    }

    public void start(){
        ScheduledFuture<?> signal =  RpkiFeeder.getInstance().start();
        BmpFeeder.getInstance().start();

    }

    public static Feeder getInstance() {
        if (instance == null) {
            instance = new Feeder();
        }
        return instance;
    }
}
