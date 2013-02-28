package deployer.report;

import deployer.report.utils.S3DeployUtil;
import deployer.report.xml.TestLog;
import org.testng.ITestResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author moran
 */
public class LogFetcher {
    private static final String BUILD_FOLDER_KEY = "sgtest.buildFolder";
    private static final String REPORT_URL_KEY = "iTests.url";
    private static final String GIGASPACES_QUALITY_S3 = "http://gigaspaces-quality.s3.amazonaws.com/";
    boolean isCloudEnabled = Boolean.parseBoolean(System.getProperty("iTests.cloud.enabled", "false"));

    public LogFetcher() {
    }

    public List<TestLog> getLogs(ITestResult result) {
        List<TestLog> logs = new ArrayList<TestLog>();
        String suiteName = System.getProperty("iTests.suiteName");
        String buildNumber = System.getProperty("iTests.buildNumber");
        //TODO
        String testName = "should resolve dependency in here";//TestNGUtils.constructTestMethodName(result);
        File testDir = new File(getBuildFolder() + "/" + suiteName + "/" + testName);

        if(isCloudEnabled){
            S3DeployUtil.uploadLogFile(testDir, buildNumber, suiteName, testName);
        }
        return fetchLogs(testDir, logs);
    }

    private String getBuildFolder() {
        return System.getProperty(BUILD_FOLDER_KEY);
    }

    private List<TestLog> fetchLogs(File dir, List<TestLog> list) {
        File[] children = dir.listFiles();
        if (children == null)
            return list;
        for (int n = 0; n < children.length; n++) {
            File file = children[n];
            if (file.getName().endsWith((".log"))) {
                TestLog testLog = new TestLog(file.getName(),
                        getFileUrl(file.getAbsolutePath()));
                list.add(testLog);
                continue;
            } else if (file.isDirectory()
                    && file.getName().equalsIgnoreCase("logs")) {
                File[] logs = file.listFiles();
                for (int i = 0; i < logs.length; i++) {
                    TestLog testLog = new TestLog(logs[i].getName(),
                            getFileUrl(logs[i].getAbsolutePath()));
                    list.add(testLog);
                }
                break;
            } else if (file.isDirectory()
                    && !file.getName().contains(".zip")) {
                fetchLogs(file, list);
            }
        }
        return list;
    }

    private String getFileUrl(String path) {
        int index = path.indexOf("build_");
        return getUrl() + path.substring(index);
    }

    private String getUrl() {
        if(isCloudEnabled){
            return GIGASPACES_QUALITY_S3;
        }else{
            return System.getProperty(REPORT_URL_KEY);
        }
    }
}
