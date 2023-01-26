package application_page_actions;

import org.codehaus.plexus.util.FileUtils;
import reporting.TestLogger;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Clank {

    public static void sendFile(String filePath) throws AWTException {
        Robot robot = new Robot();
        //put path to your file in a clipboard
        StringSelection selection = new StringSelection(filePath);
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        systemClipboard.setContents(selection, null);
        //imitate mouse events like ENTER, CTRL+C, CTRL+V
        robot.delay(1000);
      robot.keyPress(KeyEvent.VK_CONTROL);
      robot.keyPress(KeyEvent.VK_V);
      robot.keyRelease(KeyEvent.VK_V);
      robot.keyRelease(KeyEvent.VK_CONTROL);
      robot.keyPress(KeyEvent.VK_ENTER);
      robot.keyRelease(KeyEvent.VK_ENTER);

    }

    public static void moveToLocation(int x, int y) throws AWTException {
        Robot robot = new Robot();
        robot.mouseMove(x, y);

    }

    public static void screenCapture() throws AWTException, IOException {
        Robot robot = new Robot();

        DateFormat df = new SimpleDateFormat("(MM.dd.yyyy-HH mma)");
        Date date = new Date();
        df.format(date);

        Rectangle rectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage bufferedImage = robot.createScreenCapture(rectangle);
        File file = new File("screen-capture"+df.format(date)+".png");

        try {
            FileUtils.copyFile(file, new File(System.getProperty("user.dir")+ "/screenshots/"+""+df.format(date)+".png"));
            System.out.println("Screenshot captured");
        } catch (Exception e) {
            System.out.println("Exception while taking screenshot "+e.getMessage());;
        }
    }

    public static void enterKey() throws AWTException {

        Robot robot = new Robot();

        robot.keyPress(KeyEvent.VK_ENTER);
        TestLogger.log("Press Enter Key");

    }

    public static void tabKey() throws AWTException {
        Robot robot = new Robot();

        robot.keyPress(KeyEvent.VK_TAB);
        TestLogger.log("Press Tab Key");
    }

    public static void downKey() throws AWTException {
        Robot rob = new Robot();

        rob.keyPress(KeyEvent.VK_DOWN);
        rob.keyRelease(KeyEvent.VK_DOWN);
    }

    public static void helloWorld() throws AWTException {
        Robot robot = new Robot();

        robot.keyPress(KeyEvent.VK_H);
        robot.keyPress(KeyEvent.VK_E);
        robot.keyPress(KeyEvent.VK_L);
        robot.keyPress(KeyEvent.VK_L);
        robot.keyPress(KeyEvent.VK_O);
    }
}

