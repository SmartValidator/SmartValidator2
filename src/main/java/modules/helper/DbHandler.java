package modules.helper;

import modules.helper.options.OptionsHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class DbHandler {

    private DbHandler() {

    }


    public static synchronized Connection produceConnection() throws ExecutionException {
        Connection connection = null;
        try {
            System.out.println("-------- PostgreSQL "
                    + "JDBC Connection Testing ------------");

            Class.forName("org.postgresql.Driver");


            System.out.println("PostgreSQL JDBC Driver Registered!");


            final String connectionString = "jdbc:postgresql://" + OptionsHandler.getInstance().getOptions().getDatabase().getHost() + "/" + OptionsHandler.getInstance().getOptions().getDatabase().getName();
            connection = DriverManager.getConnection(
                    connectionString, OptionsHandler.getInstance().getOptions().getDatabase().getUser(),
                    OptionsHandler.getInstance().getOptions().getDatabase().getPassword());


            if (connection != null) {
                System.out.println("You made it, take control your database now!");
            } else {
                throw new Exception("Failed to make connection!");
            }
        } catch (SQLException e) {
            System.out.println("Connection Failed! Check output console");
            throw new ExecutionException(e);

        } catch (ClassNotFoundException e) {
            System.out.println("Where is your PostgreSQL JDBC Driver? "
                    + "Include in your library path!");
            throw new ExecutionException(e);
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
        return connection;

    }

}
