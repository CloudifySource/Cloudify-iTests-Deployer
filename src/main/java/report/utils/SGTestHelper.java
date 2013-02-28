package deployer.report.utils;

import java.io.File;

public class SGTestHelper {

	/**
	 * This is a very cool method which returns jar or directory from where the supplied class has loaded.
	 * 
	 * @param claz the class to get location.
	 * @return jar/path location of supplied class.
	 */
	public static String getClassLocation(Class claz)
	{
		return claz.getProtectionDomain().getCodeSource().getLocation().toString().substring(5);
	}

	public static boolean isDevMode() {
		boolean isDevMode;
		if (System.getenv().containsKey("DEV_ENV")) {
			boolean value = Boolean.getBoolean(System.getenv("DEV_ENV"));
			return value;
		} else if (System.getProperties().contains("DEV_ENV")) {
			boolean value = Boolean.getBoolean(System.getProperty("DEV_ENV"));
			return value;
		}
		

		if (System.getProperty("file.separator").equals("\\")) {
			isDevMode = !System.getenv("USERNAME").equals("ca");
		}
		else {
			isDevMode = !System.getProperty("user.name").equals("tgrid");
		}
		return isDevMode;
	}

	public static String getSGTestSrcDir() {
		String sgtestSrcDir;
		if (isDevMode()) {
			sgtestSrcDir = getSGTestRootDir() + "/src";
		}
		else {
			sgtestSrcDir = getSGTestRootDir() + "/tmp";
		}
		return sgtestSrcDir;

	}
	
	
	public static String getSuiteName(){
		return System.getProperty("iTests.suiteName", "");
	}
	
	public static String getSuiteId() {
		return System.getProperty("iTests.suiteId", "");
	}
	
	//each suite has it's own work dir.
	public static String getWorkDirName() {
		String suiteDir = getSuiteName();
		String suiteId = getSuiteId();
		Integer numberOfSuite = Integer.getInteger("iTests.numOfSuites",1);

		if (isEmpty(suiteDir) || isEmpty(suiteId) || numberOfSuite == 1) {
			return "work";
		} else {
			return suiteDir + suiteId + "_work";
		}
	}

	/** @return SGTest root directory */
	public static String getSGTestRootDir(){
		return new File(".").getAbsolutePath();
	}

	public static String getBuildDir() {
		//TODO
        return "not sure what to return";
	}

    public static boolean isEmpty(String test){
        return test == null || "".equals(test);
    }
	
}
