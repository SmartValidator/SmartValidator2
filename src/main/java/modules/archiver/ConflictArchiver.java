package modules.archiver;

import modules.helper.DbHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ConflictArchiver implements Callable<Void>{
    private static int conflict_timeout_days = 365;


    public ConflictArchiver(){

    }


    private void archiveConflicts() throws ExecutionException, SQLException {
        System.out.println("Archiving conflicts");

        Connection dbc = null;
        try {
            dbc = DbHandler.produceConnection();
            dbc.setAutoCommit(false);

            Statement s;
            s = dbc.createStatement();
            s.executeUpdate("insert into archive_snapshot_times (start) values (now());");

            /* copy the validated_roas_verified_announcements table */
            s = dbc.createStatement();
            s.executeUpdate("insert into archived_conflicts (prefix, reason) ( select prefix, route_validity from announcements inner join validated_roas_verified_announcements as vrva on announcements.id = vrva.verified_announcement_id where route_validity > 0 and not exists ( select verified_announcement_id from validated_roas_verified_announcements where route_validity = 0 and verified_announcement_id = vrva.verified_announcement_id ) group by prefix, route_validity ) on conflict (prefix, reason) do update set updated_at = now();");
            s.close();

            s = dbc.createStatement();
            s.executeUpdate("delete from archived_conflicts where updated_at < now() - interval '" + Integer.toString(conflict_timeout_days) + " days'");
            s.close();

            dbc.commit();
            dbc.close();
        } catch (Exception ex) {
            if (dbc != null) {
                dbc.rollback();
            }
            throw new ExecutionException(ex);
        }

        System.out.println("archiving of conflicts Finished.");
    }

    @Override
    public Void call() throws ExecutionException, SQLException {
            archiveConflicts();
            return null;
    }
}
