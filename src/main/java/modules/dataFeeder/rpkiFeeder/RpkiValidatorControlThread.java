package modules.dataFeeder.rpkiFeeder;

import java.io.IOException;

    public class RpkiValidatorControlThread implements Runnable {
        public Thread thread_handle = null;
        private String rpki_validator_jar_path;
        private Process p = null;

        public RpkiValidatorControlThread(String rpki_validator_jar_path){

            this.rpki_validator_jar_path = rpki_validator_jar_path;
        }

        @Override
        public void run(){
            ProcessBuilder pb = new ProcessBuilder("java", "-jar",rpki_validator_jar_path);
            try {
                p = pb.start();
                p.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }


        public void start () {
            System.out.println("Starting ");
            if (thread_handle == null) {
                thread_handle = new Thread (this);
                thread_handle.start ();
            }
        }

        public void stop (){
            System.out.println("Stopping ");
            if (thread_handle != null) {
                thread_handle.interrupt();
                p.destroy();

            }
        }
    }

