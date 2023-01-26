package database_utilities;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBDataProvider {


    public static String[][] dataSupplier(String query, String rowCount) throws IOException, SQLException {

//        query = "SELECT email.email, message FROM public.testdata" ;
//        rowCount = "SELECT count(*) FROM public.testdata";

        DBUtilities dbUtilities = new DBUtilities();
        dbUtilities.connectDB();
        ResultSet resultSet = dbUtilities.executeSelectQuery(query);
        ResultSet rowNumbersInTable = dbUtilities.executeSelectQuery(rowCount);

       rowNumbersInTable.next();
       int rowNumber = rowNumbersInTable.getInt(1);

       String[][] twoDArray = new String[rowNumber][2];

       int i = 0;
       while (resultSet.next()){
               twoDArray[i][0] = resultSet.getString("email");
               twoDArray[i][1] = resultSet.getString("message");
            i++;//1
       }

      /*  // Column title, first row
        twoDArray[0][0] = "konk@konk.com";
        twoDArray[0][1] = "Tell about your konk product";
        // first set data 2nd row
        twoDArray[1][0] = "junk@junk.com";
        twoDArray[1][1] = "Tell about your product";*/
        return twoDArray;
    }
}
