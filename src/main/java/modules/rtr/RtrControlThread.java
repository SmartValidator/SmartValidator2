package modules.rtr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RtrControlThread extends Thread {

    private final String rtrPath;

    RtrControlThread(String rtrPath) {

        this.rtrPath = rtrPath;
    }

    @Override
    public void run() {
        String[] cmd = new String[2];
        cmd[0] = "python";
        cmd[1] = rtrPath;

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
