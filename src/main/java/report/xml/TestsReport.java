package deployer.report.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author moran
 */
@XStreamAlias("TestsReport")
public class TestsReport {
    @XStreamImplicit(itemFieldName="report")
    private List<TestReport> reports;

    @XStreamAlias("suiteName")
	private String suiteName;
    
    public TestsReport(List<TestReport> reports) {
        this.reports = reports;
    }
    public TestsReport() {
    }

    /** @return null-object TestsReport */
    public static TestsReport newEmptyReport() {
        TestsReport report = new TestsReport();
        report.setReports(new ArrayList<TestReport>(0));
        return report;
    }

    public List<TestReport> getReports() {
        return reports;
    }
    public void setReports(List<TestReport> reports) {
        this.reports = reports;
    }
    
    public void setSuiteName(String suiteName) {
		this.suiteName = suiteName;
	}
    
    public String getSuiteName() {
		return suiteName;
	}
}
