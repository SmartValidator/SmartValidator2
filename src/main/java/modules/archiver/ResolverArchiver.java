package modules.archiver;

import modules.helper.DbHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ResolverArchiver implements Callable<Void>{
    private static int conflict_timeout_days = 365;


    public ResolverArchiver(){

    }

    private void archiveResolvedConflicts() throws ExecutionException, SQLException {
        System.out.println("Resolving conflicts");

        Connection dbc = null;
        try {
            dbc = DbHandler.produceConnection();
            dbc.setAutoCommit(false);

            Statement s;
            s = dbc.createStatement();
            s.executeUpdate("insert into archive_snapshot_times (start) values (now());");

            /* create fake ROA - method = 0 */
            s = dbc.createStatement();
            s.executeUpdate("insert into archived_resolutions (prefix, asn, max_length, method) select prefix, asn, max_length, 0 from payload_roas as pr where not exists ( select id from validated_roas where prefix = pr.prefix and asn = pr.asn and max_length = pr.max_length );");
            s.close();

            /* discard too restrictive ROA - method = 1 */
            s = dbc.createStatement();
            s.executeUpdate("insert into archived_resolutions (prefix, asn, max_length, method) select prefix, asn, max_length, 1 from validated_roas as vr where not exists ( select id from payload_roas where prefix = vr.prefix and asn = vr.asn and max_length = vr.max_length );");
            s.close();

            dbc.commit();
            dbc.close();
        } catch (Exception ex) {
            if (dbc != null) {
                dbc.rollback();
            }
            throw new ExecutionException(ex);
        }

        System.out.println("archiving of resolved conflicts.");
    }


    @Override
    public Void call() throws ExecutionException, SQLException {
        archiveResolvedConflicts();
        return null;
    }
}
