package modules.conflictSeeker;

import modules.helper.DbHandler;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConflictSeeker {

    private Connection connection;
    private List<Roa> validated_roas;

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

    public ConflictSeeker(){
        connection = DbHandler.produceConnection();
        if (connection != null) {
            System.out.println("Connection established");
            System.out.println("Running conflict seeker - start\n");
            long startTime = System.currentTimeMillis();
            this.runConflictSeeker();
            long stopTime = System.currentTimeMillis();
            long elapsedTime = stopTime - startTime;
            System.out.println("Finish conflict seeker - end\n");
            System.out.format("Elapsed time: %d milliseconds, %f seconds\n",elapsedTime, (float) (elapsedTime / 1000));


        } else {
            System.out.println("Failed to make connection!");
        }
    }

    private boolean checkAsn(Roa curRoa,ResultSet rs) throws Exception{
        if (curRoa.asn.equals(rs.getString(2))) return true;
        else return false;

    }
    private boolean checkLength(Roa curRoa,ResultSet rs) throws Exception{
        int checkRoa = Math.max(Integer.parseInt(curRoa.prefix.split("\\/")[1]),Integer.parseInt(curRoa.maxLength));
        int annPrefix = Integer.parseInt(rs.getString(3).split("\\/")[1]);
        if(annPrefix <= checkRoa){
            return true;
        }
        return false;
    }

    private boolean isConflictExist(long roa_id, long ann_id,int routeValidity) throws Exception{
        ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM validated_roas_verified_announcements" +
                " WHERE verified_announcement_id =  '" + ann_id+ "'" +
                "AND validated_roa_id =  '" + roa_id+ "'" +
                "AND route_validity = '"+routeValidity+"'");
        return rs.next();
    }

    private PreparedStatement detectOverlap(){
        try{

            ResultSet rs;
            int roas_counter = 0;
            String papo = "INSERT INTO validated_roas_verified_announcements(verified_announcement_id, validated_roa_id, route_validity)" +
                    " VALUES" + "(?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(papo);
            for(Roa roa : validated_roas){
                if(roas_counter % 1000 == 0 && roas_counter > 0){
                    System.out.format("Elapsed through %d Roas .\n", roas_counter);
                }
                rs = connection.createStatement().executeQuery("SELECT * FROM announcements WHERE prefix <<= inet '" + roa.prefix+ "'");
                while (rs.next() ) {
                    int key = rs.getInt(1);
                    ps.setInt(1, key);
                    ps.setInt(2, roa.roa_id);
                    if(!checkAsn(roa,rs)){
                        ps.setInt(3, 1);
                    }else if(!checkLength(roa,rs)){
                        ps.setInt(3, 2);
                    }else{
                        ps.setInt(3, 0);
                    }
                    ps.addBatch();
                }
                roas_counter += 1;
            }
            return ps;
        }catch(Exception e){
            System.out.println(e.getMessage());

        }

        return null;
    }


    private void getRoas( ) {
        try {
            // Get the database metadata

            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM validated_roas");
            while (rs.next()){
                validated_roas.add(new Roa(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4)));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());

        }
    }
    private void dropAndCreateTable() throws Exception{
        connection.createStatement().execute("DROP TABLE IF EXISTS validated_roas_verified_announcements ");
        connection.createStatement().execute("CREATE TABLE validated_roas_verified_announcements (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    verified_announcement_id integer REFERENCES verified_announcements,\n" +
                "    validated_roa_id integer REFERENCES validated_roas,\n" +
                "    route_validity int,\n" +
                "    created_at TIMESTAMP NOT NULL DEFAULT NOW(),\n" +
                "    updated_at TIMESTAMP NOT NULL DEFAULT NOW()\n" +
                ")");
        connection.createStatement().execute("CREATE TRIGGER set_timestamp BEFORE UPDATE ON validated_roas_verified_announcements FOR EACH ROW EXECUTE PROCEDURE update_timestamp()");

    }
    private void runConflictSeeker(){
        try {
            validated_roas = new ArrayList<>();
            this.getRoas();
            this.dropAndCreateTable();
            PreparedStatement ps = this.detectOverlap();
            ps.executeBatch();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }


    }


}
