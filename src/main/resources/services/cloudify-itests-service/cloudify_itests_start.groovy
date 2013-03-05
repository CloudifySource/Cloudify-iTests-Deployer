@Grapes(
        @Grab(group='org.jclouds.api', module='s3', version='1.5.8')
)
import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils

import java.util.concurrent.TimeUnit

/**
 * User: Sagi Bernstein
 * Date: 10/02/13
 * Time: 12:06
 */

def executeMaven (mvnExec, String arguments, directory){
    new AntBuilder().sequential{
        exec(executable: mvnExec,
                failonerror:false,
                dir:directory) {
            arguments.split(" ").each { arg(value: it) }
        }
    }
}

serviceDir = "${System.getProperty("user.home")}/cloudify-itests-service"
config = new ConfigSlurper().parse(new File("cloudify-itests.properties").toURL())
context = ServiceContextFactory.getServiceContext()

def buildDir = "${serviceDir}/${config.test.BUILD_DIR}"

def prefix = System.getenv().get("EXT_JAVA_OPTIONS")
if (prefix == null)
    prefix = ""
else
    prefix += " "
def ext = ServiceUtils.isWindows() ? ".bat" : "";
def mvnBinDir = "${serviceDir}/maven/apache-maven-${config.maven.version}/bin"

def mvnExec = mvnBinDir +"/mvn" + ext

def arguments = "test -e -X -U -P tgrid-sgtest-cloudify " +
        "-Dsgtest.cloud.enabled=true " +
        "-DiTests.buildNumber=${config.test.BUILD_NUMBER} " +
        "-Dcloudify.home=${buildDir} " +
        "-Dincludes=${config.test.INCLUDE} " +
        "-Dexcludes=${config.test.EXCLUDE} " +
        "-Djava.security.policy=policy/policy.all " +
        "-Djava.awt.headless=true " +
        "-Dsgtest.suiteName=${config.test.SUITE_NAME} " +
        "-DiTests.suiteId=${context.instanceId}" +
        "-Dsgtest.summary.dir=${serviceDir}/${config.test.SUITE_NAME} " +
        "-DiTests.numOfSuites=${config.test.SUITE_NUMBER} " +
        "-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger " +
        "-Dcom.gs.logging.level.config=true " +
        "-Djava.util.logging.config.file=${serviceDir}/${config.scm.projectName}/src/main/config/sgtest_logging.properties " +
        "-Dsgtest.buildFolder=${serviceDir} " +
        "-Dec2.region=${config.test.EC2_REGION} " +
        "-DipList=${config.test.BYON_MACHINES} " +
        "-Dsupported-clouds=${config.test.SUPPORTED_CLOUDS} " +
        "-Dcom.quality.sgtest.credentialsFolder=${context.getServiceDirectory()}/credentials"

try{
    executeMaven mvnExec, arguments, "${serviceDir}/${config.scm.projectName}"
}finally{
    //TODO: upload to s3 bucket
    context.attributes.thisService.remove "${config.test.TEST_RUN_ID}-${context.instanceId}"
}
if (context.instanceId == 1){
    while(context.attributes.thisService.grep(~/^\${config.test.TEST_RUN_ID}.*/).size() > 0){
        sleep TimeUnit.MINUTES.toMillis(1)
    }
    //TODO: download from s3 bucket
    executeMaven(mvnExec,
            "exec:java -Dexec.mainClass=\"framework.testng.report.TestsReportMerger\" -Dexec.args=\"${config.test.SUITE_TYPE} ${config.test.BUILD_NUMBER} ${config.test.SUITE_NAME} ${config.test.MAJOR_VERSION} ${config.test.MINOR_VERSION}\" -Dcloudify.home=${buildDir}",
            "${serviceDir}/${config.scm.projectName}")
    executeMaven(mvnExec,
            "exec:java -Dexec.mainClass=\"framework.testng.report.wiki.WikiReporter\" -Dexec.args=\"${config.test.SUITE_TYPE} ${config.test.BUILD_NUMBER} ${config.test.SUITE_NAME} ${config.test.MAJOR_VERSION} ${config.test.MINOR_VERSION} ${config.test.BUILD_LOG_URL}\" -Dcloudify.home=${buildDir}",
            "${serviceDir}/${config.scm.projectName}")
}




while(true){
    try{
        sleep TimeUnit.MINUTES.toMillis(5)
    }catch(Exception e){
        break
    }
}