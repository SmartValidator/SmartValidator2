package modules.simulator;

import modules.conflictSeeker.ConflictSeeker;
import modules.helper.DbHandler;
import org.postgresql.util.PGobject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class LocalConflictSeeker {

    private Connection connection;
    private List<Roa> payload_roas;

    public class Roa{
        private int roa_id;
        private String asn;
        private String prefix;
        private String maxLength;;

        public Roa(int roa_id, String asn, String prefix, String maxLength){
            this.roa_id = roa_id;
            this.asn = asn;
            this.prefix = prefix;
            this.maxLength = maxLength;
        }
    }

    public LocalConflictSeeker(){

    }

    private boolean checkAsn(Roa curRoa, ResultSet rs) throws Exception{
        if (curRoa.asn.equals(rs.getString(2))) return true;
        else return false;

    }
    private boolean checkLength(Roa curRoa, ResultSet rs) throws Exception{
        int checkRoa = Math.max(Integer.parseInt(curRoa.prefix.split("\\/")[1]),Integer.parseInt(curRoa.maxLength));
        int annPrefix = Integer.parseInt(rs.getString(3).split("\\/")[1]);
        if(annPrefix <= checkRoa){
            return true;
        }
        return false;
    }

    private void setBlockingStatus() {
        try {
            String papo = "UPDATE local_announcements SET  blocking_status = ? WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(papo);
            ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT local_announcement_id, MAX (blocking_status) FROM local_conflicts GROUP BY local_announcement_id");
            while (rs.next()){
                ps.setInt(2,rs.getInt(1));
                ps.setInt(1, rs.getInt(2));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private PreparedStatement detectOverlap() throws ExecutionException {
        try{

            ResultSet rs;
            int roas_counter = 0;
            String papo = "INSERT INTO local_conflicts(local_announcement_id, blocking_status)" +
                    " VALUES" + "(?, ?)";
            PreparedStatement ps = connection.prepareStatement(papo);
            for(Roa roa : payload_roas){
                if(roas_counter % 1000 == 0 && roas_counter > 0){
                    System.out.format("Elapsed through %d Roas .\n", roas_counter);
                }
                rs = connection.createStatement().executeQuery("SELECT * FROM local_announcements " +
                        "WHERE prefix <<= inet '" + roa.prefix+ "'");
                while (rs.next() ) {
                    System.out.println("YEAH! Match!");
                    int key = rs.getInt(1);
                    ps.setInt(1, key);
                    if(!checkAsn(roa,rs)){
                        ps.setInt(2, 1);
                    }else if(!checkLength(roa,rs)){
                        ps.setInt(2, 1);
                    }else{
                        ps.setInt(2, 2);
                    }
                    ps.addBatch();
                }
                roas_counter += 1;
            }
            return ps;
        }catch(Exception e){
            throw new ExecutionException(e);

        }
    }

    private void getRoas( ) throws ExecutionException {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM payload_roas");
            while (rs.next()){
                payload_roas.add(new Roa(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4)));
            }
        } catch (Exception e) {
            throw new ExecutionException(e);

        }
    }

    private void getLocalAnnouncements() {
        try {
            List<String> userPrefixes = new ArrayList<>();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT prefix FROM user_prefixes");
            while (rs.next()) {
                userPrefixes.add(rs.getString(1));
            }
            String insertStmt = "INSERT INTO local_announcements(asn,prefix) VALUES (?, ?)";
            PreparedStatement ps = connection.prepareStatement(insertStmt);
            for(int i = 0; i < userPrefixes.size(); i++){
                rs = stmt.executeQuery(
                        "SELECT * FROM announcements WHERE prefix <<= inet '" + userPrefixes.get(i) + "'");
                while (rs.next()) {
                    long asn = rs.getLong(2);
                    PGobject dummyObject = new PGobject();
                    dummyObject.setType("cidr");
                    dummyObject.setValue(rs.getString(3));
                    ps.setObject(2, dummyObject, Types.OTHER);
                    ps.setLong(1, asn);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void dropAndCreateTables() throws Exception{
        connection.createStatement().execute("DROP TABLE IF EXISTS local_conflicts ");
        connection.createStatement().execute("DROP TABLE IF EXISTS local_announcements ");

        connection.createStatement().execute("CREATE TABLE public.local_announcements\n" +
                "(\n" +
                "  id INT DEFAULT nextval('validated_roas_id_seq'::REGCLASS) PRIMARY KEY NOT NULL,\n" +
                "  asn BIGINT NOT NULL,\n" +
                "  prefix CIDR NOT NULL,\n" +
                "  blocking_status INT DEFAULT 0 NOT NULL,\n" +
//                                "  ris_peers BIGINT NOT NULL,\n" +
                "  created_at TIMESTAMP DEFAULT now() NOT NULL,\n" +
                "  updated_at TIMESTAMP DEFAULT now() NOT NULL\n" +
                ");\n");
        connection.createStatement().execute("CREATE TRIGGER set_timestamp BEFORE UPDATE ON local_announcements FOR EACH ROW EXECUTE PROCEDURE update_timestamp()");

        connection.createStatement().execute("CREATE TABLE local_conflicts (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    local_announcement_id INT REFERENCES local_announcements,\n" +
                "    blocking_status INT DEFAULT 0 NOT NULL,\n" +
                "    created_at TIMESTAMP NOT NULL DEFAULT NOW(),\n" +
                "    updated_at TIMESTAMP NOT NULL DEFAULT NOW()\n" +
                ")");
        connection.createStatement().execute("CREATE TRIGGER set_timestamp BEFORE UPDATE ON local_conflicts FOR EACH ROW EXECUTE PROCEDURE update_timestamp()");
    }

    private void connectToDB() throws Exception {
        this.connection = DbHandler.produceConnection();
        if(this.connection == null)
            throw new Exception("Failed to connect DB");
    }

    public void run() throws ExecutionException{
        try {
            payload_roas = new ArrayList<>();
            this.connectToDB();
            this.dropAndCreateTables();
            this.getRoas();
            this.getLocalAnnouncements();
            PreparedStatement ps = this.detectOverlap();
            ps.executeBatch();
            this.setBlockingStatus();
        }catch(Exception e){
            throw new ExecutionException(e);
        }
    }
}
