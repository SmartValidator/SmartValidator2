package modules.helper;

import modules.helper.options.OptionsHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbHandler {

    private DbHandler(){

    }



    public static synchronized Connection produceConnection(){
        Connection connection = null;
        System.out.println("-------- PostgreSQL "
                + "JDBC Connection Testing ------------");

        try {

            Class.forName("org.postgresql.Driver");

        } catch (ClassNotFoundException e) {

            System.out.println("Where is your PostgreSQL JDBC Driver? "
                    + "Include in your library path!");
            e.printStackTrace();
            return null;
        }

        System.out.println("PostgreSQL JDBC Driver Registered!");


        try {
            final String connectionString = "jdbc:postgresql://" + OptionsHandler.getInstance().getOptions().getDatabase().getHost() + "/" + OptionsHandler.getInstance().getOptions().getDatabase().getName();
            connection = DriverManager.getConnection(
                    connectionString, OptionsHandler.getInstance().getOptions().getDatabase().getUser(),
                    OptionsHandler.getInstance().getOptions().getDatabase().getPassword());

        } catch (SQLException e) {

            System.out.println("Connection Failed! Check output console");
            e.printStackTrace();
            return null;

        }

        if (connection != null) {
            System.out.println("You made it, take control your database now!");
        } else {
            System.out.println("Failed to make connection!");
        }
        return connection;

    }

}
