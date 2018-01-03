package modules.conflictSeeker;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConflictSeeker {

    private Connection connection;
    private List<Roa> validated_roas;
    private Map<Integer, List<RoaEntry>> validatedAnnouncements;
    public class RoaEntry{
        private int roa_id;
        private int validity;

        public RoaEntry(int roa_id, int validity){
            this.roa_id = roa_id;
            this.validity = validity;

        }
    }

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
        System.out.println("-------- PostgreSQL "
                + "JDBC Connection ------------");

        try {

            Class.forName("org.postgresql.Driver");

        } catch (ClassNotFoundException e) {

            e.printStackTrace();
            return;

        }

        System.out.println("PostgreSQL JDBC Driver Registered!");

        connection = null;

        try {

            connection = DriverManager.getConnection(
                    "jdbc:postgresql://smart-validator.net/smart_validator_test_5", System.getProperty("validator.db.user.name"),
                    System.getProperty("validator.db.password"));

        } catch (SQLException e) {

            System.out.println("Connection Failed! Check output console");
            e.printStackTrace();
            return;

        }

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
    private void updateValidationMap(RoaEntry entry, int key){
        if(validatedAnnouncements.get(key) == null){
            List<RoaEntry> newList = new ArrayList<>();
            newList.add(entry);
            validatedAnnouncements.put(key,newList);
        }else{
            List<RoaEntry> curList = validatedAnnouncements.get(key);
            curList.add(entry);
            validatedAnnouncements.put(key, curList);
        }

    }

    private boolean checkAsn(Roa curRoa,ResultSet rs) throws Exception{
        if(curRoa.asn.equals(rs.getString(2)))
            return true;
        return false;

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
            String insertTableSQL = "INSERT INTO validated_roas_verified_announcements"
                    + "(verified_announcement_id, validated_roa_id, route_validity) VALUES"
                    + "(?,?,?)";
            String updatedTableSQL = "UPDATE INTO validated_roas_verified_announcements"
                    + "(verified_announcement_id, validated_roa_id, route_validity, updated_at) VALUES"
                    + "(?,?,?,?)";
            String papo = "INSERT INTO validated_roas_verified_announcements(verified_announcement_id, validated_roa_id, route_validity)" +
                    " VALUES" + "(?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(papo);
            for(Roa roa : validated_roas){
                if(roas_counter % 1000 == 0 && roas_counter > 0){
                    System.out.format("Elapsed through %d Roas .\n", roas_counter);
                }
//                rs = connection.createStatement().executeQuery("SELECT * FROM invalid_announcements('" + roa.prefix+ "')");
//                rs = connection.createStatement().executeQuery("SELECT * FROM invalid_length_announcements('" + roa.prefix+ "')");
//                rs = connection.createStatement().executeQuery("SELECT * FROM valid_announcements('" + roa.prefix+ "')");
                rs = connection.createStatement().executeQuery("SELECT * FROM announcements WHERE prefix <<= inet '" + roa.prefix+ "'");
                while (rs.next() ) {
//                    String key = rs.getString(2)+":"+rs.getString(3);
                    int key = rs.getInt(1);
                    if(!checkAsn(roa,rs)){
//                        updateValidationMap(new RoaEntry(roa.roa_id,2),key);
//                        isConflictExist(roa.roa_id, key,1);
                        ps.setInt(1, key);
                        ps.setInt(2, roa.roa_id);
                        ps.setInt(3, 1);
                        ps.addBatch();
//                        ps.clearParameters();
                    }else if(!checkLength(roa,rs)){
//                        updateValidationMap(new RoaEntry(roa.roa_id,1),key);
//                        isConflictExist(roa.roa_id, key,2);
                        ps.setInt(1, key);
                        ps.setInt(2, roa.roa_id);
                        ps.setInt(3, 2);
                        ps.addBatch();
//                        ps.clearParameters();
                    }else{
//                        updateValidationMap(new RoaEntry(roa.roa_id,0),key);
//                        isConflictExist(roa.roa_id, key,0);
                        ps.setInt(1, key);
                        ps.setInt(2, roa.roa_id);
                        ps.setInt(3, 0);
                        ps.addBatch();
//                        ps.clearParameters();
                    }
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
//            ResultSetMetaData rsmd = rs.getMetaData();
//            DatabaseMetaData metadata = connection.getMetaData();
//
//            // Specify the type of object; in this case we want tables
//
//            String[] types = {"TABLE"};
//
//            ResultSet resultSet = metadata.getTables(null, null, "%", types);
//
//
//            while (resultSet.next()) {
//
//                String tableName = resultSet.getString(3);
//
//                String tableCatalog = resultSet.getString(1);
//
//                String tableSchema = resultSet.getString(2);
//
//
//                System.out.println("Table : " + tableName + "nCatalog : " + tableCatalog + "nSchema : " + tableSchema);
////
////
////            }
//            }
        } catch (Exception e) {
            System.out.println(e.getMessage());

        }
    }

    private void runConflictSeeker(){
        try {
            validated_roas = new ArrayList<>();
            validatedAnnouncements = new HashMap<>();
            this.getRoas();
            PreparedStatement ps = this.detectOverlap();
            ps.executeBatch();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }


    }


}
