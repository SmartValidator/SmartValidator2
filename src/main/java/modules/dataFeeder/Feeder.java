package modules.dataFeeder;

import modules.dataFeeder.rpkiFeeder.RpkiFeeder;
import modules.dataFeeder.rpkiFeeder.RpkiValidatorControlThread;

import java.net.URL;

public class Feeder {
    private static Feeder instance = null;

    private Feeder() {
    }

    private void start(){
        RpkiFeeder.getInstance().start();

    }

    public static Feeder getInstance() {
        if (instance == null) {
            instance = new Feeder();
        }
        return instance;
    }
}
