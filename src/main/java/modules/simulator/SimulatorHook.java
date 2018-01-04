package modules.simulator;

import modules.helper.DbHandler;
import java.sql.*;


public class SimulatorHook implements Runnable {
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
			s.executeUpdate("insert into archived_conflicts (prefix,reason) select prefix, route_validity from announcements inner join validated_roas_verified_announcements as o on announcements.id = verified_announcement_id where route_validity > 0 and not exists ( select verified_announcement_id from validated_roas_verified_announcements where route_validity = 0 and verified_announcement_id = o.verified_announcement_id ) group by prefix, route_validity;");
			s.close();

			dbc.commit();
			dbc.close();
		} catch (Exception ex) {
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

