package modules.conflictSeeker;

import modules.helper.DbHandler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;

public class ConflictTimeLine implements Runnable {

    Connection connection;
    public void run(){
        System.out.println("Running conflict timeline editor\n");
        try {
            connection = DbHandler.produceConnection();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void resetConflictTimelineTable() throws Exception{
        Connection resetConnection = DbHandler.produceConnection();
//        CREATE TABLE public.conflict_timeline
//                (
//                        id INTEGER DEFAULT nextval('conflict_timeline_id_seq'::regclass) PRIMARY KEY NOT NULL,
//                        count INTEGER,
//                        check_date TIMESTAMP DEFAULT now() NOT NULL
//                );
        Statement stmt = resetConnection.createStatement();
//                        ResultSet rs = stmt.executeQuery("CREATE TABLE validated_roas_rv_test ( id SERIAL (10) DEFAULT nextval('validated_roas_rv_test_id_seq':: REGCLASS ) NOT NULL\n" + "  CONSTRAINT validated_roas_rv_test_pkey\n" + "  PRIMARY KEY,\n" + "  asn          CIDR(max)    NOT NULL, max_length   INT4(10)     NOT NULL,  trust_anchor VARCHAR(255) NOT NULL\n" + "); COMMENT ON TABLE validated_roas_rv_test IS 'test table to be filled from the rpki validator run'");
        stmt.execute("DROP TABLE IF EXISTS timeline_conflicts");
        stmt.execute("CREATE TABLE public.timeline_conflicts\n" +
                "(\n" +
                "  id INTEGER DEFAULT nextval('conflict_timeline_id_seq'::regclass) PRIMARY KEY NOT NULL,\n" +
                "  count INTEGER,\n" +
                "  check_date TIMESTAMP DEFAULT now() NOT NULL\n" +
                ");");

    }

    public void start(){
//        SELECT COUNT(*) FROM validated_roas_verified_announcements WHERE route_validity = 2 OR route_validity = 1
        try{
            ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM validated_roas_verified_announcements WHERE route_validity = 2 OR route_validity = 1");
            if(rs.next()){
                int countConflicts = rs.getInt("count");
                connection.createStatement().execute("INSERT INTO conflict_timeline(count) VALUES ('" + countConflicts + "')");
            }

        }catch(Exception e){

        }



    }
    

}
