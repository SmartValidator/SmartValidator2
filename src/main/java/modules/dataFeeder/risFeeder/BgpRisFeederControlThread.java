package modules.dataFeeder.risFeeder;

import modules.helper.DbHandler;
import org.postgresql.util.PGobject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.*;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;


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
    private static final String BGP_ENTRY_REGEX = "^\\s*([0-9]+)\\s+([0-9a-fA-F.:/]+)\\s+([0-9]+)\\s*$";
    private static final String BGP_ENTRY_REGEX_2 = "^\\s*\\{([0-9]+)\\}\\s+([0-9a-fA-F.:/]+)\\s+([0-9]+)\\s*$";

    public BgpRisFeederControlThread() {}

    @Override
    public void run() {
        try (Connection dbConnection = DbHandler.produceConnection()) {
            assert dbConnection != null;
            Statement stmt = dbConnection.createStatement();
            stmt.execute("DROP TABLE IF EXISTS announcements");
            stmt.execute("CREATE TABLE public.announcements\n" +
                    "(\n" +
                    "  id INT DEFAULT nextval('validated_roas_id_seq'::REGCLASS) PRIMARY KEY NOT NULL,\n" +
                    "  asn BIGINT NOT NULL,\n" +
                    "  prefix CIDR NOT NULL,\n" +
//                                "  ris_peers BIGINT NOT NULL,\n" +
                    "  created_at TIMESTAMP DEFAULT now() NOT NULL,\n" +
                    "  updated_at TIMESTAMP DEFAULT now() NOT NULL\n" +
                    ");\n");

            connectAndDownload("http://www.ris.ripe.net/dumps/riswhoisdump.IPv4.gz", dbConnection);
            connectAndDownload("http://www.ris.ripe.net/dumps/riswhoisdump.IPv6.gz", dbConnection);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void connectAndDownload(String urlString, Connection dbConnection) {
        URL url = null;
        HttpURLConnection bgpRisConnection = null;
        try {
            url = new URL(urlString);

            bgpRisConnection = (HttpURLConnection) url.openConnection();
            bgpRisConnection.setRequestMethod("GET");
            bgpRisConnection.setUseCaches(false);
            bgpRisConnection.setAllowUserInteraction(false);

            bgpRisConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:57.0) Gecko/20100101 Firefox/57.0");
            bgpRisConnection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            int status = bgpRisConnection.getResponseCode();

            switch(status){
                case 200:
                case 201:
                    GZIPInputStream gzip = new GZIPInputStream(bgpRisConnection.getInputStream());
                    BufferedReader in = new BufferedReader(new InputStreamReader(gzip));
                    String inputLine;

                    try {
                        assert dbConnection != null;
                        String insertStmt = "INSERT INTO global_announcements(asn,prefix) VALUES (?, ?)";
                        PreparedStatement ps = dbConnection.prepareStatement(insertStmt);

                        int i = 0;
                        while ((inputLine = in.readLine()) != null){
                            if(inputLine.matches(BGP_ENTRY_REGEX)) {
                                String[] split = inputLine.split("\\s+");
                                long asn = Long.parseLong(split[0]);
                                String prefix = split[1];
                                long risPeers = Long.parseLong(split[2]);
                                PGobject dummyObject = new PGobject();
                                dummyObject.setType("cidr");
                                dummyObject.setValue(prefix);
                                ps.setObject(2, dummyObject, Types.OTHER);
                                ps.setLong(1, asn);
//                                ps.setLong(3, risPeers);
                                ps.addBatch();
                            } else if(inputLine.matches(BGP_ENTRY_REGEX_2)) {
                                String[] split = inputLine.split("\\s+");
                                split[0] = split[0].replaceAll("[{}]", "");
                                long asn = Long.parseLong(split[0]);
                                String prefix = split[1];
                                long risPeers = Long.parseLong(split[2]);
                                PGobject dummyObject = new PGobject();
                                dummyObject.setType("cidr");
                                dummyObject.setValue(prefix);
                                ps.setObject(2, dummyObject, Types.OTHER);
                                ps.setLong(1, asn);
//                                ps.setLong(3, risPeers);
                                ps.addBatch();
                            } else {
//                                System.out.println("Non-parseable line : " + inputLine);
                                i++;
                            }
                        }
                        System.out.println("Non-parseable: " + i + " lines.");
                        ps.executeBatch();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    in.close();
                default:
            }
        } catch (ProtocolException e1) {
            e1.printStackTrace();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally{
            assert bgpRisConnection != null;
            bgpRisConnection.disconnect();
        }
    }

}


