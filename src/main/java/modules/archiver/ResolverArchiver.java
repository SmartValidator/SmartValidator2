package modules.archiver;

import modules.helper.DbHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ResolverArchiver implements Callable<Void> {
    private static int conflict_timeout_days = 365;


    public ResolverArchiver() {

    }

    private void archiveResolvedConflicts() throws ExecutionException, SQLException {
        System.out.println("Resolving conflicts");

        Connection dbc = null;
        try {
            dbc = DbHandler.produceConnection();
            dbc.setAutoCommit(false);

            Statement s;
            s = dbc.createStatement();
            s.executeUpdate("INSERT INTO archive_snapshot_times (start) VALUES (now());");

            /* create fake ROA - method = 0 */
            s = dbc.createStatement();
            s.executeUpdate("INSERT INTO archived_resolutions (prefix, asn, max_length, method) SELECT prefix, asn, max_length, 0 FROM payload_roas AS pr WHERE NOT exists ( SELECT id FROM validated_roas WHERE prefix = pr.prefix AND asn = pr.asn AND max_length = pr.max_length );");
            s.close();

            /* discard too restrictive ROA - method = 1 */
            s = dbc.createStatement();
            s.executeUpdate("INSERT INTO archived_resolutions (prefix, asn, max_length, method) SELECT prefix, asn, max_length, 1 FROM validated_roas AS vr WHERE NOT exists ( SELECT id FROM payload_roas WHERE prefix = vr.prefix AND asn = vr.asn AND max_length = vr.max_length );");
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
