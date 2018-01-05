package modules.conflictHandler;

import modules.helper.DbHandler;
import org.postgresql.util.PGobject;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.sql.Types.OTHER;

public class ConflictHandler {

    private List<Announcement> announcements;
    private List<Roa> roas;
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

        public Announcement getAnnouncement(){
            return announcement;
        }

        public void addRoa(RoaEntry roaEntry){
            roaEntrys.add(roaEntry);
        }

        public List<RoaEntry> getRoaEntrys() {
            return roaEntrys;
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
        private int route_validity;
        private Timestamp created_at;
        private Timestamp updated_at;

        public RoaEntry(Roa roa, int route_validity, Timestamp created_at, Timestamp updated_at){
            this.roa = roa;
            this.route_validity = route_validity;
            this.created_at = created_at;
            this.updated_at = updated_at;
        }

    	public Roa getRoa(){
    		return this.roa;
    	}

        public int getValidity(){
            return this.route_validity;
        }
    }

    public class Roa  implements Comparable {
        private int id;
        private long asn;
        private String prefix;
        private int max_length;
        private int trust_anchor_id;
        private Timestamp created_at;
        private Timestamp updated_at;

        public Roa(int id, long asn, String prefix, int max_length,
                   int trust_anchor_id, Timestamp created_at, Timestamp updated_at){
            this.id = id;
            this.asn = asn;
            this.prefix = prefix;
            this.max_length = max_length;
            this.trust_anchor_id = trust_anchor_id;
            this.created_at = created_at;
            this.updated_at = updated_at;
        }

        @Override
        public String toString(){
            return this.id + "\t" + this.asn + "\t" + this.prefix + "\t" + this.max_length + "\t" + "\t" + this.trust_anchor_id + "\t" + this.created_at + "\t" + this.updated_at;
        }

        @Override
        public int compareTo(Object o){
            Roa other = (Roa) o;
            return getId() - other.getId();
        }

        public int getId(){
            return id;
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
    }

    public class Announcement implements Comparable {
        private int id;
        private long asn;
        private String prefix;
        private Timestamp created_at;
        private Timestamp updated_at;

        public Announcement(int id, long asn, String prefix, Timestamp created_at, Timestamp updated_at){
            this.id = id;
            this.asn = asn;
            this.prefix = prefix;
            this.created_at = created_at;
            this.updated_at = updated_at;
        }

        @Override
        public int compareTo(Object o){
            Announcement other = (Announcement) o;
            return getId() - other.getId();
        }

        public int getId(){
            return id;
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
        connection = DbHandler.produceConnection();

        if (connection != null) {
            loadData();
            handleConflicts();
            pushRoas();
        } else {
            System.out.println("Failed to make connection!");
        }
    }

    /*----- Data Loading -----*/

    /**
     * Loads the table of verified BPG announcements from the database.
     */
    private void loadAnnouncements(){
        this.announcements = new ArrayList<>();
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT id, asn, prefix, created_at, updated_at FROM announcements WHERE id IN " +
                            "(SELECT announcement_id FROM verified_announcements)");
            while(rs.next()){
                announcements.add(new Announcement(
                        rs.getInt("id"),
                        rs.getLong("asn"),
                        rs.getString("prefix"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                ));
//                if(announcements.size() % 100000 == 0){
//                    System.out.println("Loaded " + announcements.size() + " announcements.");
//                }
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        Collections.sort(announcements);
    }

    /**
     * Loads the ROA table from the database.
     */
    private void loadRoas(){
        roas = new ArrayList<>();
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM validated_roas ORDER BY id");

            while(rs.next()){
                int id = rs.getInt(1);
                roas.add( new Roa(id, rs.getLong(2), rs.getString(3), rs.getInt(4),
                        rs.getInt(7), rs.getTimestamp(8), rs.getTimestamp(9)));
//                if(roas.size() % 10000 == 0){
//                    System.out.println("Loaded " + roas.size() + " ROAs.");
//                }
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        Collections.sort(roas);
    }

    /**
     * Loads the ROA - BGP announcement overlaps, i.e. possible conflicts,
     * from the database.
     */
    private void loadOverlaps(){
        overlaps = new ArrayList<>();
        try{
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT * FROM validated_roas_verified_announcements " +
                            "ORDER BY verified_announcement_id");
            rs.next();

            int previousAnnouncement_id;
            int announcement_id = rs.getInt(2);
            Announcement announcement = announcements.get(announcement_id - 1);

            int roa_id = rs.getInt(3);
            Roa roa = roas.get(roa_id - 1);
            int route_validity = rs.getInt(4);
            Timestamp created_at = rs.getTimestamp(5);
            Timestamp updated_at = rs.getTimestamp(6);
            RoaEntry roaEntry = new RoaEntry(roa, route_validity, created_at, updated_at);

            Overlap conflict = new Overlap(announcement, roaEntry);

            while(rs.next()){
                previousAnnouncement_id = announcement_id;
                announcement_id = rs.getInt(2);
                announcement = announcements.get(announcement_id - 1);

                roa_id = rs.getInt(3);
                roa = roas.get(roa_id - 1);
                route_validity = rs.getInt(4);
                created_at = rs.getTimestamp(5);
                updated_at = rs.getTimestamp(6);
                roaEntry = new RoaEntry(roa, route_validity, created_at, updated_at);
                if(announcement_id == previousAnnouncement_id){
                    conflict.addRoa(roaEntry);
                }else{
                    overlaps.add(conflict);
                    conflict = new Overlap(announcement, roaEntry);
                }
//                if(overlaps.size() % 10000 == 0){
//                    System.out.println("Loaded " + overlaps.size() + " overlaps.");
//                }
            }
            if(!overlaps.contains(conflict)){
                overlaps.add(conflict);
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void loadData() {
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

    private void filter(int days){
        long now = System.currentTimeMillis();
        for( int i = 0; i < overlaps.size(); i++ ){
            Announcement announcement = overlaps.get(i).getAnnouncement();
            if((now - announcement.getCreated_at().getTime()) / DAY > days){
                List<Roa> roas = overlaps.get(i).getRoas();
                for(int j = 0; j < roas.size(); j++){
                    if(this.roas.contains(roas.get(j))) {
                        this.roas.remove(this.roas.indexOf(roas.get(j)));
                    }
                }
            }
        }
    }

    private void whitelist(int days){
        long now = System.currentTimeMillis();
        for( int i = 0; i < overlaps.size(); i++ ){
            Announcement announcement = overlaps.get(i).getAnnouncement();
            if((now - announcement.getCreated_at().getTime()) / DAY > days){
                int max_length = Integer.parseInt(announcement.prefix.split("/")[1]);
                roas.add(new Roa(roas.size(), announcement.getAsn(), announcement.getPrefix(),
                        max_length, 0, null, null));
            }
        }
    }

    private int[] getHeuristicSettings() {
        int[] settings = new int[2];
        settings[0] = 0;
        settings[1] = 1;
        try {
            ResultSet rs = connection.createStatement().executeQuery("SELECT value FROM settings " +
                    "WHERE key IN ('conflictHandler.heuristic', 'conflictHandler.thresholdDays')");
            rs.next();
            settings[0] = Integer.parseInt(rs.getString("value"));
            rs.next();
            settings[1] = Integer.parseInt(rs.getString("value"));
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
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

    private void handleConflicts() {
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

    private void dropAndCreateTable() throws Exception{
        connection.createStatement().execute("DROP TABLE IF EXISTS payload_roas");
        connection.createStatement().execute("CREATE TABLE payload_roas (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    asn bigint NOT NULL,\n" +
                "    prefix cidr NOT NULL,\n" +
                "    max_length int NOT NULL,\n" +
                "    created_at TIMESTAMP NOT NULL DEFAULT NOW(),\n" +
                "    updated_at TIMESTAMP NOT NULL DEFAULT NOW()\n" +
                ")");
        connection.createStatement().execute("CREATE TRIGGER set_timestamp BEFORE UPDATE ON payload_roas FOR EACH ROW EXECUTE PROCEDURE update_timestamp()");

    }

    private PreparedStatement collectRoas(){
        try{
            String papo = "INSERT INTO payload_roas " +
                    "(asn, prefix, max_length) VALUES " + "(?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(papo);
            for(int i = 0; i < roas.size(); i++){
                Roa roa = roas.get(i);
                ps.setLong(1, roa.getAsn());
                ps.setObject(2, roa.getPrefix(), OTHER);
                ps.setInt(3, roa.getMax_length());
//                if(i % 10000 == 0){
//                    System.out.println("Elapsed " + i + " ROAs.");
//                }
                ps.addBatch();
            }
            return ps;
        }catch(Exception e){
            System.out.println(e.getMessage());
        }

        return null;
    }

    private void pushRoas(){
        try {
            System.out.println("Pushing ROAs, please stand by...");
            long start = System.currentTimeMillis();

            dropAndCreateTable();
            PreparedStatement ps = collectRoas();
            ps.executeBatch();

            long end = System.currentTimeMillis();
            long diff = end - start;
            System.out.println("Pushing took " + (diff / 1000)  + "." + (diff % 1000) + " s.\n");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
