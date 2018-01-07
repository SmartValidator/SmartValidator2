package modules.dataFeeder;

import modules.helper.DbHandler;
import org.postgresql.util.PGobject;

import java.sql.*;
import java.util.concurrent.ExecutionException;

public class AnnouncementMerger {

    private Connection dbConnection;

    private void mergeAnnouncementsTables() {
        try {
            dbConnection.createStatement().executeUpdate(
                    "INSERT INTO" +
                    " announcements(asn, prefix) (SELECT asn, prefix FROM local_announcements)" +
                    " ON CONFLICT (asn, prefix)" +
                    " DO UPDATE SET updated_at = now()"
            );
//            ResultSet rs = dbConnection.createStatement().executeQuery("SELECT asn, prefix FROM local_announcements");
//            String insertString = "INSERT INTO" +
//                    " announcements(asn, prefix) VALUES (?, ?)" +
//                    " ON CONFLICT (asn, prefix)" +
//                    " DO UPDATE SET updated_at = now()";
//            PreparedStatement ps = dbConnection.prepareStatement(insertString);
//            while (rs.next()) {
//                ps.setLong(1, rs.getLong(1));
//                PGobject dummyObject = new PGobject();
//                dummyObject.setType("cidr");
//                dummyObject.setValue(rs.getString(2));
//                ps.setObject(2, dummyObject, Types.OTHER);
//                ps.addBatch();
//            }
//            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void run() throws ExecutionException {
        try {
            dbConnection = DbHandler.produceConnection();
            assert dbConnection != null;
        } catch (ExecutionException e) {
            throw e;
        }
    }
}
