package modules.conflictHandler;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConflictHandler {

    private List<Announcement> announcements;
    private List<Roa> roas;
    private List<Overlap> overlaps;

    private static final long DAY = 86400000;
    private Connection connection;

    public class Overlap {
        private Announcement announcement;
        private List<RoaEntry> roas;

        public Overlap(Announcement announcement, List<RoaEntry> roas){
            this.announcement = announcement;
            this.roas = roas;
        }

        public Overlap(Announcement announcement, RoaEntry roaEntry){
            this.announcement = announcement;
            this.roas = new ArrayList<>();
            this.roas.add(roaEntry);
        }

        public Announcement getAnnouncement(){
            return this.announcement;
        }

        public void addRoa(RoaEntry roaEntry){
            this.roas.add(roaEntry);
        }

        public List<RoaEntry> getRoas(){
            return this.roas;
        }

        public int[] getRoaIds(){
            int[] roaIds = new int[roas.size()];
            for(int i = 0; i < roas.size(); i++){
                roaIds[i] = roas.get(i).getRoaId();
            }
            return roaIds;
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

        public RoaEntry(int id, int asn, String prefix, int max_length,
                        boolean filtered, boolean whitelisted, int trust_anchor_id, Timestamp roa_created_at, Timestamp roa_updated_at,
                        int route_validity, Timestamp created_at, Timestamp updated_at){
            this.roa = new Roa(id, asn, prefix, max_length, filtered, whitelisted, trust_anchor_id, roa_created_at, roa_updated_at);
            this.route_validity = route_validity;
            this.created_at = created_at;
            this.updated_at = updated_at;
        }

//    	public Roa getRoa(){
//    		return this.roa;
//    	}

        public int getRoaId(){
            return roa.getId();
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
        private boolean filtered;
        private boolean whitelisted;
        private int trust_anchor_id;
        private Timestamp created_at;
        private Timestamp updated_at;

        public Roa(int id, long asn, String prefix, int max_length,
                   boolean filtered, boolean whitelisted, int trust_anchor_id, Timestamp created_at, Timestamp updated_at){
            this.id = id;
            this.asn = asn;
            this.prefix = prefix;
            this.max_length = max_length;
            this.filtered = filtered;
            this.whitelisted = whitelisted;
            this.trust_anchor_id = trust_anchor_id;
            this.created_at = created_at;
            this.updated_at = updated_at;
        }

        @Override
        public String toString(){
            return this.id + "\t" + this.asn + "\t" + this.prefix + "\t" + this.max_length + "\t" + this.filtered + "\t" +
                    this.whitelisted + "\t" + this.trust_anchor_id + "\t" + this.created_at + "\t" + this.updated_at;
        }

        @Override
        public int compareTo(Object o){
            Announcement other = (Announcement) o;
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

        public boolean getFiltered(){
            return this.filtered;
        }

        public void setFiltered(boolean filtered){
            this.filtered = filtered;
        }

        public boolean getWhitelisted(){
            return this.whitelisted;
        }

        public int getTrust_anchor_id(){
            return trust_anchor_id;
        }

        public void setFilteredWhitelistFalse(){
            this.filtered = false;
            this.whitelisted = false;
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

        public Timestamp getCreated_at() {
            return created_at;
        }
    }

    public ConflictHandler( String database, String user, String password ){
        connect( database, user, password );
        loadConflicts();
    }

    public void loadConflicts(){
        System.out.println("Loading conflicts, please stand by...\n");
        long start = System.currentTimeMillis();

        loadAnnouncements();
        loadRoas();
        loadOverlaps();

        long end = System.currentTimeMillis();
        long diff = end - start;
        System.out.println("Loaded ROAs: " + roas.size() + ", Loaded overlaps: " + overlaps.size() +
                ". Loading took " + (diff / 1000)  + "." + (diff % 1000) + " s.\n");

        removeValid();

        System.out.println("Conflicts: " + overlaps.size());
    }

    public void handleConflicts(int heuristic, int days){
        System.out.println("Resolving conflicts, please stand by...\n");
        long start = System.currentTimeMillis();

        switch(heuristic){
            case -1 :	ignore();
                break;
            case 0 :	filter(days);
                break;
            case 1 :	whitelist(days);
                break;
            default :	break;
        }

        long end = System.currentTimeMillis();
        long diff = end - start;
        System.out.println("Invalid overlaps / conflicts: " + overlaps.size() + ", ROAs: " + roas.size() +
                ". Resolving took " + (diff / 1000)  + "." + (diff % 1000) + " s.");
    }

    /**
     * Connects the Conflict Handler to the database.
     * @param database Database name
     * @param user Database user name
     * @param password Database user password
     */
    private void connect( String database, String user, String password ){
        System.out.println("---------- PostgreSQL "
                + "JDBC Connection ----------");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Where is your PostgreSQL JDBC Driver? "
                    + "Include in your library path!");
            e.printStackTrace();
            return;
        }
        System.out.println("PostgreSQL JDBC Driver Registered!");
        connection = null;
        try {
            connection = DriverManager.getConnection(database, user, password);
        } catch (SQLException e) {
            System.out.println("Connection Failed! Check output console.\n");
            e.printStackTrace();
            return;
        }
        if (connection != null) {
            System.out.println("You made it, take control over your database now!");
        } else {
            System.out.println("\nFailed to make connection!");
        }
    }

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
            Roa roa;

            while(rs.next()){
                int id = rs.getInt(1);
                boolean filtered = rs.getBoolean(5);
                boolean whitelisted = rs.getBoolean(6);
                roa = new Roa(id, rs.getLong(2), rs.getString(3), rs.getInt(4),
                        filtered, whitelisted, rs.getInt(7), rs.getTimestamp(8), rs.getTimestamp(9));
                roas.add(roa);
                if(roas.size() % 10000 == 0){
                    System.out.println("Loaded " + roas.size() + " ROAs.");
                }
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
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
                            "GROUP BY verified_announcement_id");
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
                if(overlaps.size() % 10000 == 0){
                    System.out.println("Loaded " + overlaps.size() + " overlaps.");
                }
            }
            if(!overlaps.contains(conflict)){
                overlaps.add(conflict);
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    /**
     * Cleans the overlap table from entries with a valid ROA, so that only conflicts remain.
     */
    private void removeValid(){
        for(int i = 0; i < overlaps.size(); i++){
            for(int j = 0; j < overlaps.get(i).getRoas().size(); j++){
                // If the ROAs field contains a valid ROA, the overlap is removed from the list.
                if(overlaps.get(i).getRoas().get(j).getValidity() == 0){
                    overlaps.remove(i);
                    break;
                }
            }
        }
    }

    private void ignore(){
        try {
            Statement stmt = connection.createStatement();
            int wl = stmt.executeUpdate("DELETE FROM validated_roas WHERE whitelisted = true");
            int fi = stmt.executeUpdate("UPDATE validated_roas SET filtered = false WHERE filtered = true");
            System.out.println("Removed " + wl + " whitelisted ROAs, restored " + fi + " filtered ROAs.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filter(int days){
        long now = System.currentTimeMillis();
//    	LocalDateTime now = LocalDateTime.now();
        try {
            Statement stmt = connection.createStatement();
            int wl = stmt.executeUpdate("DELETE FROM validated_roas WHERE whitelisted = true");
            for( int i = 0; i < overlaps.size(); i++ ){
                Announcement announcement = overlaps.get(i).getAnnouncement();
                if((now - announcement.getCreated_at().getTime()) / DAY > days){
                    for(int j = 0; j < overlaps.get(i).getRoaIds().length; j++){
                        int roaId = overlaps.get(i).getRoaIds()[j];
                        stmt.executeUpdate("UPDATE validated_roas SET filtered = true WHERE id = " + roaId);
//						stmt.executeUpdate("UPDATE validated_roas SET filtered = true WHERE created_at + interval '" + days + " days' < now()");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void whitelist(int days){
        long now = System.currentTimeMillis();
//    	LocalDateTime now = LocalDateTime.now();
        try {
            Statement stmt = connection.createStatement();
            int wl = stmt.executeUpdate("DELETE FROM validated_roas WHERE whitelisted = true");
            for( int i = 0; i < overlaps.size(); i++ ){
                Announcement announcement = overlaps.get(i).getAnnouncement();
                if((now - announcement.getCreated_at().getTime()) / DAY > days){
                    for(int j = 0; j < overlaps.get(i).getRoaIds().length; j++){
                        int roaId = overlaps.get(i).getRoaIds()[j];
                        stmt.executeUpdate("UPDATE validated_roas SET whitelisted = true WHERE id = " + roaId);
//						stmt.executeUpdate("UPDATE validated_roas SET whitelisted = true WHERE created_at + interval '" + days + " days' < now()");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
