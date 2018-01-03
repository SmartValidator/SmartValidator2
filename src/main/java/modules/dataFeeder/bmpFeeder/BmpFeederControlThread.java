package modules.dataFeeder.bmpFeeder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BmpFeederControlThread extends Thread {

    private final String bmpLibPath;

    BmpFeederControlThread(String bmpLibPath) {

        this.bmpLibPath = bmpLibPath;
    }

    @Override
    public void run() {
        String[] cmd = new String[2];
        cmd[0] = "python";
        cmd[1] = bmpLibPath;

        Runtime rt = Runtime.getRuntime();
        Process pr = null;
        try {
            pr = rt.exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // retrieve output from python script
        assert pr != null;
        BufferedReader bfr = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        // display each output line form python script
        bfr.lines().forEachOrdered(System.out::println);
    }
}
