package modules.conflictSeeker;

import modules.helper.DbHandler;

import java.sql.Connection;
import java.sql.ResultSet;

public class ConflictTimeLine implements Runnable {
    Connection connection;
    public void run(){
        System.out.println("Running conflict timeline editor\n");
        connection = DbHandler.produceConnection();
    }

    public void start(){
//        SELECT COUNT(*) FROM validated_roas_verified_announcements WHERE route_validity = 2 OR route_validity = 1
        try{
            ResultSet rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM validated_roas_verified_announcements WHERE route_validity = 2 OR route_validity = 1");
            if(rs.next()){
                int count_conflits = rs.getInt("count");
                connection.createStatement().execute("INSERT INTO conflict_timeline(count) VALUES ('" + count_conflits + "')");
            }

        }catch(Exception e){

        }



    }
    

}
