package modules.dataFeeder.risFeeder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


public class BgpRisFeederControlThread implements Runnable {
    private static final int MAX_BUFFER_SIZE = 1024;
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;
    private URL url;
    private int size = 0;
    private int downloaded = 0;
    private int status;


    public BgpRisFeederControlThread() {}

    @Override
    public void run() {
        URL url = null;
        HttpURLConnection risIpv4Connection = null;
        RandomAccessFile randomAccessFile = null;
        InputStream inputStream = null;
        try {
            url = new URL("http://www.ris.ripe.net/dumps/riswhoisdump.IPv4.gz");
            risIpv4Connection = (HttpURLConnection) url.openConnection();
            risIpv4Connection.setRequestMethod("GET");
            risIpv4Connection.setUseCaches(false);
            risIpv4Connection.setAllowUserInteraction(false);
            risIpv4Connection.connect();
            int status = risIpv4Connection.getResponseCode();
            size = risIpv4Connection.getContentLength();
            inputStream = risIpv4Connection.getInputStream();
            while(downloaded < size){
                byte buffer[];
                int finalSize=size - downloaded;
                if ( finalSize > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[size - downloaded];
                }
                int read = inputStream.read(buffer);
                if(read == -1)
                    break;

                downloaded += read;


            }



    } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
                if(inputStream!=null){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }
        }


        public static void main(String args[]){
            (new BgpRisFeederControlThread()).run();
        }
    }


