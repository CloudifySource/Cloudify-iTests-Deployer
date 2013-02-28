package deployer.report.xml;


/**
 * @author moran
 */
public class SummaryReport {
	
	private final int success;
	private final int failed;
    private final int suspected;
	private final int skipped;
	private final int total;
	private final long duration;
	private String suiteName;
	
	public SummaryReport(TestsReport testsReport) {
		
		int success = 0;
		int failed = 0;
        int suspected = 0;
		int skipped = 0;
		long duration = 0;
		
		for (TestReport testReport : testsReport.getReports()) {
            if (testReport.isSuspected()) {
                ++suspected;
            }else if (testReport.isSuccess()) {
				++success;
			} else if (testReport.isFailed()) {
				++failed;
			} else if (testReport.isSkipped()) {
				++skipped;
			}
			duration += testReport.getDuration();
		}
		
		this.success = success;
		this.failed = failed;
        this.suspected = suspected;
		this.skipped = skipped;
		this.total = testsReport.getReports().size();
		this.duration = duration;
		this.suiteName = testsReport.getSuiteName();
	}
	
	public int getSuccess() {
		return success;
	}
	
	public int getFailed() {
		return failed;
	}

    public int getSuspected() {
        return suspected;
    }
	
	public int getSkipped() {
		return skipped;
	}
	
	public int getTotalTestsRun() {
		return total;
	}
	
	public long getDuration() {
		return duration;
	}
	
	public String getSuiteName() {
		return suiteName;
	}
	
	@Override
	public String toString() {
		return "total tests run: " + total + ", success: " + success + ", failed: " + failed + ", suspected: " + suspected + ", skipped: " + skipped;
	}
}
