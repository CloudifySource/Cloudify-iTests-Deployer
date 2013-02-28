package deployer.report.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author moran
 */
@XStreamAlias("TestLog")
public class TestLog {
	@XStreamAlias("name")
	private String name;
	@XStreamAlias("url")
	private String url;
	
	public TestLog(String name, String url) {
		this.name = name;
		this.url = url;
	}
	
	public TestLog() {
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
}
