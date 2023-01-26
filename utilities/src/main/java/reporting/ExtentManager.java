package reporting;


import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.relevantcodes.extentreports.ExtentReports;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.Reporter;
import webmanager.WebManager;

import java.io.File;

public class ExtentManager {
    private static ExtentReports extent;
    private static ITestContext context;

    WebDriver driver;

    public static ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter(System.getProperty("user.dir")+"/Extent-Report/" + WebManager.browserName + " ExtentReport.html");

    public synchronized static ExtentReports getInstance(){
        if(extent == null){
            
            File outputDirectory = new File(context.getOutputDirectory());
            File resultDirectory = new File(outputDirectory.getParentFile(),"html");
            extent = new ExtentReports(System.getProperty("user.dir")+"/Extent-Report/" + WebManager.browserName + " ExtentReport.html", false);
            Reporter.log("Extent Report Directory"+ resultDirectory, true);
            extent.addSystemInfo("Host Name", "Navid Khan")
                    .addSystemInfo("Environment","QA")
                    .addSystemInfo("User Name", "Team Khan");


            extent.loadConfig(new File(System.getProperty("user.dir")+"/extent-config.xml"));
        }

        return extent;

    }


    public static void setOutputDirectory(ITestContext context){
        ExtentManager.context = context;

    }
}
