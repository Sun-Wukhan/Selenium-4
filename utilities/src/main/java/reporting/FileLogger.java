package reporting;

import java.io.FileWriter;
import java.io.IOException;

public class FileLogger {

    public static FileWriter myFileWriter;

    public static void initializeFileWriter(String fileName) {
        try {

            myFileWriter = new FileWriter(System.getProperty("user.dir")+"/Text/"+fileName +".txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void start() throws IOException {
        myFileWriter.write("---------- START ----------" + "\n");
    }

    public static void log(String text) throws IOException {
        myFileWriter.write(text + "\n");
        myFileWriter.flush();
    }

    public static void end() throws IOException {
        // myFileWriter.write("----------- END -----------" + "\n");
        myFileWriter.flush();
        myFileWriter.close();
    }


}
