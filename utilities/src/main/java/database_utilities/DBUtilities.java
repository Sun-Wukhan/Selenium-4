package database_utilities;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class DBUtilities {

    String userName = loadProperties().getProperty("userName.test");
    String password = loadProperties().getProperty("password.test");
    // Where is the DB server
    // jdbc:postgresql://host:port/database
    String url = loadProperties().getProperty("url.test");
    String query = null;

    public DBUtilities() throws IOException {
    }

    public Properties loadProperties() throws IOException {
        //relative path for file
        //todo add realative path
        //todo learn client usage and information
        //todo add a find all links/pictures
        File file = new File("/Users/navid/Documents/BondBrandFramework/utilities/src/main/resources/app.properties");
        FileReader fileReader = new FileReader(file);
        // Create a properties object to handle properties inside the properties file
        Properties properties = new Properties();

        // Loaded properties in the system from the properties file
        properties.load(fileReader);

        return properties;

    }

    public Connection connectDB() throws SQLException, IOException {

        Connection connection = DriverManager.getConnection(url,userName,password);
        return connection;
    }

    public ResultSet executeSelectQuery(String query) throws IOException, SQLException {

        Statement statement = connectDB().createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        return resultSet;

    }
    public void processResultSet(String query, String col1, String col2, String col3) throws IOException, SQLException {
        ResultSet resultSet = executeSelectQuery(query);
        // Process the result
        while(resultSet.next()) {
            System.out.println(resultSet.getString(col1) + "            " +resultSet.getString(col2) + "            "
                    +  resultSet.getString(col3));
        }
    }


}
