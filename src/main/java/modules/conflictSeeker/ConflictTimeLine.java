package modules.conflictSeeker;

import modules.helper.DbHandler;

import java.sql.Connection;

public class ConflictTimeLine implements Runnable {
    Connection connection;
    public void run(){
        System.out.println("Running conflict timeline editor\n");
        connection = DbHandler.produceConnection();
    }

    public void start(){
//        SELECT COUNT(*) FROM validated_roas_verified_announcements WHERE route_validity = 2 OR route_validity = 1


    }
    

}
