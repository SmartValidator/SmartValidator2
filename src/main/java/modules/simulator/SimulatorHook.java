package modules.simulator;

import modules.helper.DbHandler;
import java.sql.*;


public class SimulatorHook implements Runnable {
	public static int conflict_timeout_days = 365;

	public void run() {
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
			System.out.println(ex);
			if (dbc != null) {
				try {
					dbc.rollback();
				} catch (Exception ex2) {
				}
			}
		}

		System.out.println("SimulatorHook: Finished.");
	}

	public static void main(String args[]) {
		Thread t = new Thread(new SimulatorHook());
		t.start();
		try {
			t.join();
		} catch (Exception ex) {
		}
	}
}

