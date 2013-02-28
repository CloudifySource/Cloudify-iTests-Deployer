package deployer.report;

import org.testng.ITestResult;

import java.util.Comparator;

/**
* Comparator for sorting TestNG test results alphabetically by method name.
* @author moran
*/
class TestResultComparator implements Comparator<ITestResult>
{
    public int compare(ITestResult result1, ITestResult result2)
    {
        return result1.getName().compareTo(result2.getName());
    }
}
