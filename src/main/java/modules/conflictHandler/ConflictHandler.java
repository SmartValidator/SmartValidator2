package modules.conflictHandler;

import modules.helper.DbHandler;
import org.postgresql.util.PGobject;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.sql.Types.OTHER;

public class ConflictHandler {

    private Map<Integer, Announcement> announcements;
    private Map<Integer, Roa> roas;
    private List<Overlap> overlaps;

    private static final long DAY = 86400000;
    private Connection connection;

    public class Overlap {
        private Announcement announcement;
        private List<RoaEntry> roaEntrys;

        public Overlap(Announcement announcement, List<RoaEntry> roaEntrys){
            this.announcement = announcement;
            this.roaEntrys = roaEntrys;
        }

        public Overlap(Announcement announcement, RoaEntry roaEntry){
            this.announcement = announcement;
            roaEntrys = new ArrayList<>();
            roaEntrys.add(roaEntry);
        }

        public Overlap(Announcement announcement) {
            this.announcement = announcement;
            roaEntrys = new ArrayList<>();
        }

        public Announcement getAnnouncement(){
            return announcement;
        }

        public void addRoa(RoaEntry roaEntry){
            roaEntrys.add(roaEntry);
        }

        public List<RoaEntry> getRoaEntrys() {
            return roaEntrys;
        }

        public Map<Roa, Integer> getRoasWithId() {
            Map<Roa, Integer> roasWithId = new HashMap<>();
            for (int i = 0; i < roaEntrys.size(); i++){
                roasWithId.put(roaEntrys.get(i).getRoa(), roaEntrys.get(i).getRoaId());
            }
            return roasWithId;
        }

        public List<Roa> getRoas(){
            List<Roa> roas = new ArrayList<>();
            for(int i = 0; i < roaEntrys.size(); i++){
                roas.add(roaEntrys.get(i).getRoa());
            }
            return roas;
        }
    }

    public class RoaEntry {
        private Roa roa;
        private int roaId;
        private int route_validity;
        private Timestamp created_at;
        private Timestamp updated_at;

        public RoaEntry(Roa roa, int roaId, int route_validity, Timestamp created_at, Timestamp updated_at){
            this.roa = roa;
            this.roaId = roaId;
            this.route_validity = route_validity;
            this.created_at = created_at;
            this.updated_at = updated_at;
        }

    	public Roa getRoa(){
    		return this.roa;
    	}

    	public int getRoaId() {
            return roaId;
        }

        public int getValidity(){
            return this.route_validity;
        }
    }

    public class Roa {
        private long asn;
        private String prefix;
        private int max_length;
        private int trust_anchor_id;
        private Timestamp created_at;
        private Timestamp updated_at;
        private boolean isFilter = false;
        private boolean isWhitelist = false;

        public Roa(long asn, String prefix, int max_length,
                   int trust_anchor_id, Timestamp created_at, Timestamp updated_at){
            this.asn = asn;
            this.prefix = prefix;
            this.max_length = max_length;
            this.trust_anchor_id = trust_anchor_id;
            this.created_at = created_at;
            this.updated_at = updated_at;
        }

        public long getAsn(){
            return asn;
        }

        public String getPrefix(){
            return prefix;
        }

        public int getMax_length(){
            return max_length;
        }

        public int getTrust_anchor_id(){
            return trust_anchor_id;
        }

        public void setFilter(){this.isFilter = true;}

        public void setWhitelist(){this.isWhitelist = true;}
    }

    public class Announcement {
        private long asn;
        private String prefix;
        private Timestamp created_at;
        private Timestamp updated_at;

        public Announcement(long asn, String prefix, Timestamp created_at, Timestamp updated_at){
            this.asn = asn;
            this.prefix = prefix;
            this.created_at = created_at;
            this.updated_at = updated_at;
        }

        public long getAsn() {
            return asn;
        }

        public String getPrefix() {
            return prefix;
        }

        public Timestamp getCreated_at() {
            return created_at;
        }
    }

    /**
     * Start the Conflict Handler.
     */
    public ConflictHandler() {
    }

    public void run() throws ExecutionException {
        connection = DbHandler.produceConnection();

        if (connection != null) {
            loadData();
            handleConflicts();
            pushRoas();
        } else {
            throw new ExecutionException(new Exception("Failed to make connection!"));
        }
    }

    /*----- Data Loading -----*/

    /**
     * Loads the table of BPG announcements from the database.
     */
    private void loadAnnouncements() throws ExecutionException {
        this.announcements = new HashMap<>();
        try{
            ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT id, asn, prefix, created_at, updated_at FROM announcements"
            );
            while(rs.next()){
                announcements.put(rs.getInt("id"),
                        new Announcement(
                                rs.getLong("asn"),
                                rs.getString("prefix"),
                                rs.getTimestamp("created_at"),
                                rs.getTimestamp("updated_at")
                        )
                );
                if(announcements.size() % 100000 == 0){
                    System.out.println("Loaded " + announcements.size() + " announcements.");
                }
            }
        }catch(Exception e){
            throw new ExecutionException(e);
        }
    }

    /**
     * Loads the ROA table from the database.
     */
    private void loadRoas() throws ExecutionException {
        roas = new HashMap<>();
        try{
            ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT id, asn, prefix, max_length," +
                            " trust_anchor_id, created_at, updated_at FROM validated_roas"
            );
            while(rs.next()){
               roas.put(rs.getInt("id"),
                        new Roa(
                                rs.getLong("asn"),
                                rs.getString("prefix"),
                                rs.getInt("max_length"),
                                rs.getInt("trust_anchor_id"),
                                rs.getTimestamp("created_at"),
                                rs.getTimestamp("updated_at")
                        )
                );
                if(roas.size() % 10000 == 0){
                    System.out.println("Loaded " + roas.size() + " ROAs.");
                }
            }
        }catch(Exception e){
            throw new ExecutionException(e);
        }
    }

    /**
     * Loads the ROA - BGP announcement overlaps, i.e. possible conflicts,
     * from the database.
     */
    private void loadOverlaps() throws ExecutionException {
        overlaps = new ArrayList<>();
        try{
            ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT id, announcement_id, validated_roa_id," +
                            " route_validity, created_at, updated_at" +
                            " FROM validated_roas_verified_announcements" +
                            " ORDER BY announcement_id"
            );
            rs.next();
            int current_announcement_id = rs.getInt("announcement_id");
            int previous_announcement_id = current_announcement_id;
            Overlap overlap = new Overlap(announcements.get(current_announcement_id));

            do {
                current_announcement_id = rs.getInt("announcement_id");
                RoaEntry roaEntry = new RoaEntry(
                        roas.get(rs.getInt("validated_roa_id")),
                        rs.getInt("validated_roa_id"),
                        rs.getInt("route_validity"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                );
                if(previous_announcement_id == current_announcement_id) {
                    overlap.addRoa(roaEntry);
                } else {
                    overlaps.add(overlap);
                    overlap = new Overlap(announcements.get(current_announcement_id));
                }
                previous_announcement_id = current_announcement_id;
                if(overlaps.size() % 10000 == 0){
                    System.out.println("Loaded " + overlaps.size() + " overlaps.");
                }
            } while(rs.next());

            if(!overlaps.contains(overlap)) {
                overlaps.add(overlap);
            }
//            rs.next();
//
//            int previousAnnouncement_id;
//            int announcement_id = rs.getInt(2);
//            Announcement announcement = announcements.get(announcement_id - 1);
//
//            int roa_id = rs.getInt(3);
//            Roa roa = roas.get(roa_id - 1);
//            int route_validity = rs.getInt(4);
//            Timestamp created_at = rs.getTimestamp(5);
//            Timestamp updated_at = rs.getTimestamp(6);
//            RoaEntry roaEntry = new RoaEntry(roa, route_validity, created_at, updated_at);
//
//            Overlap conflict = new Overlap(announcement, roaEntry);
//
//            while(rs.next()){
//                previousAnnouncement_id = announcement_id;
//                announcement_id = rs.getInt(2);
//                announcement = announcements.get(announcement_id - 1);
//
//                roa_id = rs.getInt(3);
//                roa = roas.get(roa_id - 1);
//                route_validity = rs.getInt(4);
//                created_at = rs.getTimestamp(5);
//                updated_at = rs.getTimestamp(6);
//                roaEntry = new RoaEntry(roa, route_validity, created_at, updated_at);
//                if(announcement_id == previousAnnouncement_id){
//                    conflict.addRoa(roaEntry);
//                }else{
//                    overlaps.add(conflict);
//                    conflict = new Overlap(announcement, roaEntry);
//                }
//                if(overlaps.size() % 10000 == 0){
//                    System.out.println("Loaded " + overlaps.size() + " overlaps.");
//                }
//            }
//            if(!overlaps.contains(conflict)){
//                overlaps.add(conflict);
//            }
        }catch(Exception e){
            throw new ExecutionException(e);
        }
    }

    private void loadData() throws ExecutionException {
        System.out.println("Loading conflicts, please stand by...");
        long start = System.currentTimeMillis();

        loadAnnouncements();
        loadRoas();
        loadOverlaps();

        long end = System.currentTimeMillis();
        long diff = end - start;
        System.out.println("Loading took " + (diff / 1000)  + "." + (diff % 1000) + " s.\n");
    }

    /*----- Conflict Handling -----*/

    /**
     * Cleans the overlap table from entries with a valid ROA, so that only conflicts remain.
     */
    private void removeValidOverlaps(){
        for(int i = 0; i < overlaps.size(); i++){
            for(int j = 0; j < overlaps.get(i).getRoaEntrys().size(); j++){
                // If the ROAs field contains a valid ROA, the overlap is removed from the list.
                if(overlaps.get(i).getRoaEntrys().get(j).getValidity() == 0){
                    overlaps.remove(i);
                    break;
                }
            }
        }
    }

    private void ignore(){}

/*
    private void filter(int days){
        long now = System.currentTimeMillis();
        for( int i = 0; i < overlaps.size(); i++ ){
            Announcement announcement = overlaps.get(i).getAnnouncement();
            if((now - announcement.getCreated_at().getTime()) / DAY > days){
                List<Roa> roas = overlaps.get(i).getRoas();
                for(int j = 0; j < roas.size(); j++){
                    if(this.roas.conta this.roas.remove(ins(roas.get(j))) {
                       this.roas.indexOf(roas.get(j)));
                    }
                }
            }
        }
    }
*/
    private void filter(int days){
        List<RoaEntry> roasToBeFiltered = null;
        long now = System.currentTimeMillis();
        for( int i = 0; i < overlaps.size(); i++ ){
            Announcement announcement = overlaps.get(i).getAnnouncement();
            if((now - announcement.getCreated_at().getTime()) / DAY > days){
//                List<Roa> roasToBeFiltered = overlaps.get(i).getRoas();
//                Map<Roa, Integer> roasToBeFilteredWithKeys = overlaps.get(i).getRoasWithId();
//                for(int j = 0; j < roasToBeFiltered.size(); j++){
////                    if(this.roas.contains(roas.get(j))) {
////                        this.roas.get(this.roas.indexOf(roas.get(j))).setFilter();
////                    }
//                    if(roas.containsValue(roasToBeFiltered.get(j))) {
//                        roas.get(roasToBeFilteredWithKeys.get(roasToBeFiltered.get(j)));
//                    }
//                }
                roasToBeFiltered = overlaps.get(i).getRoaEntrys();
                for(int j = 0; j < roasToBeFiltered.size(); j++){
                    roas.get(roasToBeFiltered.get(j).getRoaId()).setFilter();
                }
//
            }
        }
    }

    private void whitelist(int days){
        long now = System.currentTimeMillis();
        Roa newRoa = null;
        for( int i = 0; i < overlaps.size(); i++ ){
            Announcement announcement = overlaps.get(i).getAnnouncement();
            if((now - announcement.getCreated_at().getTime()) / DAY > days){
                int max_length = Integer.parseInt(announcement.prefix.split("/")[1]);
                newRoa = new Roa(announcement.getAsn(), announcement.getPrefix(),
                        max_length, 0, null, null);
                newRoa.setWhitelist();
                roas.put(-(i + 1), newRoa);
            }
        }
    }

    private int[] getHeuristicSettings() throws ExecutionException {
        int[] settings = new int[2];
        settings[0] = 0;
        settings[1] = 1;
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT value FROM settings " +
                    "WHERE key = 'conflictHandler.heuristic'");
            rs.next();
            settings[0] = Integer.parseInt(rs.getString("value"));
            rs = connection.createStatement().executeQuery("SELECT value FROM settings " +
                    "WHERE key = 'conflictHandler.thresholdDays'");
            rs.next();
            settings[1] = Integer.parseInt(rs.getString("value"));
        } catch (SQLException | NumberFormatException e) {
            throw new ExecutionException(e);
        } finally {
            if(settings[0] < 0 || settings[0] > 3) {
                settings[0] = 0;
            }
            if(settings[1] < 0) {
                settings[1] = 0;
            } else if(settings[1] > 32) {
                settings[1] = 32;
            }
        }
        return settings;
    }

    private void handleConflicts() throws ExecutionException {
        System.out.println("Resolving conflicts, please stand by...");
        long start = System.currentTimeMillis();

        removeValidOverlaps();
        int[] settings = getHeuristicSettings();
        switch(settings[0]){
            case 0: ignore();
            break;
            case 1: filter(0);
            break;
            case 2: whitelist(0);
            break;
            case 3: whitelist(settings[1]);
        }

        long end = System.currentTimeMillis();
        long diff = end - start;
        System.out.println("Resolving took " + (diff / 1000)  + "." + (diff % 1000) + " s.\n");
    }

    /*----- ROA Pushing -----*/

    private void dropAndCreateTable() throws SQLException {
        connection.createStatement().execute("DROP TABLE IF EXISTS payload_roas");
        connection.createStatement().execute("CREATE TABLE payload_roas (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    asn bigint NOT NULL,\n" +
                "    prefix cidr NOT NULL,\n" +
                "    max_length int NOT NULL,\n" +
                "    filtered BOOLEAN DEFAULT false,\n" +
                "    whitelisted BOOLEAN DEFAULT false,\n" +
                "    created_at TIMESTAMP NOT NULL DEFAULT NOW(),\n" +
                "    updated_at TIMESTAMP NOT NULL DEFAULT NOW()\n" +
                ")");
        connection.createStatement().execute("CREATE TRIGGER set_timestamp BEFORE UPDATE ON payload_roas FOR EACH ROW EXECUTE PROCEDURE update_timestamp()");

    }

    private void collectRoas() throws ExecutionException {
        try{
            String papo = "INSERT INTO payload_roas " +
                    "(asn, prefix, max_length, whitelisted, filtered) VALUES " + "(?, ?, ?, ?, ?)" +
                    "ON CONFLICT (asn, prefix, max_length) DO UPDATE SET whitelisted =?, filtered = ?, updated_at = now()";
            String updateFilter = "UPDATE payload_roas SET filtered = TRUE WHERE asn = ? AND prefix= ? AND max_length = ?";
            PreparedStatement ps = connection.prepareStatement(papo);
            PreparedStatement psFilter =  connection.prepareStatement(updateFilter);
            ResultSet rs;
            Statement stmt = connection.createStatement();
            int counter = 0;
            for(Roa roa : roas.values()){
                if (counter % 500 == 0){
                    System.out.println(counter);
                }
                ps.setLong(1, roa.getAsn());
                ps.setObject(2, roa.getPrefix(), OTHER);
                ps.setInt(3, roa.getMax_length());
                ps.setBoolean(4, roa.isWhitelist);
                ps.setBoolean(5, roa.isFilter);
                ps.setBoolean(6, roa.isWhitelist);
                ps.setBoolean(7, roa.isFilter);
                ps.addBatch();
                if(roa.isFilter){
                   rs = stmt.executeQuery("SELECT *\n" +
                            "FROM validated_roas\n" +
                            "WHERE INET '" + roa.prefix + "' <<=  prefix");
                   while(rs.next()){
                       psFilter.setLong(1, rs.getLong(2));
                       psFilter.setObject(2, rs.getObject(3), OTHER);
                       psFilter.setInt(3, rs.getInt(4));
                       psFilter.addBatch();
                   }
                }
                counter++;

            }
            System.out.println("Execute first batch");
            ps.executeBatch();
            System.out.println("Execute second batch");
            psFilter.executeBatch();
        }catch(Exception e){
            throw new ExecutionException(e);
        }

    }

    private void pushRoas() throws ExecutionException {
        try {
            System.out.println("Pushing ROAs, please stand by...");
            long start = System.currentTimeMillis();

//            dropAndCreateTable();
            collectRoas();

            long end = System.currentTimeMillis();
            long diff = end - start;
            System.out.println("Pushing took " + (diff / 1000)  + "." + (diff % 1000) + " s.\n");
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

}
