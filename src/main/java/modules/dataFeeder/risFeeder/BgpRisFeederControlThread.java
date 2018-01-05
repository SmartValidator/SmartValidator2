package modules.dataFeeder.risFeeder;

import modules.helper.DbHandler;
import org.postgresql.util.PGobject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.*;
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

    public BgpRisFeederControlThread() {}

    @Override
    public void run() {
        URL url = null;
        HttpURLConnection risIpv4Connection = null;
        RandomAccessFile randomAccessFile = null;
        InputStream inputStream = null;
        try(Connection dbConnection = DbHandler.produceConnection()) {
            url = new URL("http://www.ris.ripe.net/dumps/riswhoisdump.IPv4.gz");
            risIpv4Connection = (HttpURLConnection) url.openConnection();
            risIpv4Connection.setRequestMethod("GET");
            risIpv4Connection.setUseCaches(false);
            risIpv4Connection.setAllowUserInteraction(false);
            risIpv4Connection.connect();
            int status = risIpv4Connection.getResponseCode();
            size = risIpv4Connection.getContentLength();
            inputStream = risIpv4Connection.getInputStream();

            GZIPInputStream gzip = new GZIPInputStream(inputStream);
            BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

            assert dbConnection != null;
            Statement stmt = dbConnection.createStatement();
//                        ResultSet rs = stmt.executeQuery("CREATE TABLE validated_roas_rv_test ( id SERIAL (10) DEFAULT nextval('validated_roas_rv_test_id_seq':: REGCLASS ) NOT NULL\n" + "  CONSTRAINT validated_roas_rv_test_pkey\n" + "  PRIMARY KEY,\n" + "  asn          CIDR(max)    NOT NULL, max_length   INT4(10)     NOT NULL,  trust_anchor VARCHAR(255) NOT NULL\n" + "); COMMENT ON TABLE validated_roas_rv_test IS 'test table to be filled from the rpki validator run'");
            stmt.execute("DROP TABLE IF EXISTS global_announcements");
            stmt.execute("CREATE TABLE public.global_announcements\n" +
                    "(\n" +
                    "  id INT DEFAULT nextval('validated_roas_id_seq'::REGCLASS) PRIMARY KEY NOT NULL,\n" +
                    "  asn BIGINT NOT NULL,\n" +
                    "  prefix CIDR NOT NULL,\n" +
                    "  ris_peers BIGINT NOT NULL,\n" +
                    "  created_at TIMESTAMP DEFAULT now() NOT NULL\n" +
                    ");\n");

            String insertStmt = "INSERT INTO global_announcements(asn,prefix,ris_peers) VALUES (?, ?, ?)";
            PreparedStatement ps = dbConnection.prepareStatement(insertStmt);

            String line;
            int i = 0;
            while ((line = br.readLine()) != null && i < 100){
                if(line.matches(BGP_ENTRY_REGEX)){
                    String[] split = line.split("\\s+");
                    long asn = Long.parseLong(split[0]);
                    String prefix = split[1];
                    long risPeers = Long.parseLong(split[2]);
                    PGobject dummyObject = new PGobject();
                    dummyObject.setType("cidr");
                    dummyObject.setValue(prefix);
                    ps.setObject(2, dummyObject, Types.OTHER);
                    ps.setLong(1, asn);
                    ps.setLong(3, risPeers);
                    ps.addBatch();
                    System.out.println(asn + prefix + risPeers);
                } else {
                    System.out.println("FALSE : " + line);
                }

                i++;
            }
            ps.executeBatch();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
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


