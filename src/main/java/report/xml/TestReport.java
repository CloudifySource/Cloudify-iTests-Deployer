package deployer.report.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;

/**
 * @author moran
 */
@XStreamAlias("TestReport")
public class TestReport implements Comparable<TestReport>{

    @XStreamAlias("name")
    private String name;

    @XStreamAlias("failed")
    private boolean failed;

    @XStreamAlias("suspected")
    private boolean suspected;
    
    @XStreamAlias("skipped")
    private boolean skipped;

    @XStreamAlias("cause")
    private String cause;

    @XStreamAlias("codeUrl")
    private String codeUrl;

    @XStreamImplicit(itemFieldName="log")
    private List<TestLog> logs;

    @XStreamAlias("duration")
    private Long duration;

    public TestReport(String name) {
        this.name = name;
    }
    public TestReport() {
    }

    public String getName() {
        return name;
    }

    public String getCodeUrl() {
        return codeUrl;
    }
    public void setCodeUrl(String codeUrl) {
        this.codeUrl = codeUrl;
    }
    public List<TestLog> getLogs() {
        return logs;
    }
    public void setLogs(List<TestLog> logs) {
        this.logs = logs;
    }
    public void setName(String name) {
        this.name = name;
    }
    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public boolean isSuspected() {
        return suspected;
    }

    public void setSuspected(boolean suspected) {
        this.suspected = suspected;
    }
    
    public boolean isSkipped() {
		return skipped;
	}
    
    public void setSkipped(boolean skipped) {
		this.skipped = skipped;
	}

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestReport that = (TestReport) o;

        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
    }

    public boolean isSmaller(TestReport other) {
        return  this.compareTo(other) < 0;
    }

    public int compareTo(TestReport other) {
        return getName().compareTo(other.getName());
    }

    @Override
    public String toString() {
        return "[TestReport: " + name + ", isSuccess: " + isSuccess() + ", isFailed: " + isFailed() + ", isSuspected: "
                + isSuspected() + ", isSkipped: " + isSkipped() + " ]";
    }

    public boolean isSuccess() {
        return !isFailed() && !isSkipped() && !isSuspected();
    }

    public String getTestngTestMethodName() {
    	String testName = this.getName(); //package.class.method
    	String methodName = testName.substring(testName.lastIndexOf('.')+1);
    	return methodName;
    }
    
    public String getTestngTestClassName() {
    	String testName = this.getName(); //package.class.method
		String className = testName.substring(0, testName.lastIndexOf('.')); //package.class
		return className;
    }
    
    public String getTestngTestPackageName() {
    	String className = getTestngTestClassName();  //package.class
		String packageName = className.substring(0, className.lastIndexOf('.'));
		return packageName;
    }
}
