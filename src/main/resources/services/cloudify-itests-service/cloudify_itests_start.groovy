import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils

/**
 * User: Sagi Bernstein
 * Date: 10/02/13
 * Time: 12:06
 */

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
        "-Dsgtest.cloud.enabled=false " +
        "-DiTests.buildNumber=${config.test.BUILD_NUMBER} " +
        "-Dcloudify.home=${buildDir} " +
        "-Dincludes=${config.test.INCLUDE} " +
        "-Dexcludes=${config.test.EXCLUDE} " +
        "-Djava.security.policy=policy/policy.all " +
        "-Djava.awt.headless=true " +
        "-Dsgtest.suiteName=${config.test.SUITE_NAME} " +
        "-DiTests.suiteId=${context.instanceId}" +
        "-Dsgtest.summary.dir=${buildDir}/../${config.test.SUITE_NAME} " +
        "-DiTests.numOfSuites=${config.test.SUITE_NUMBER} " +
        "-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger " +
        "-Dcom.gs.logging.level.config=true " +
        "-Djava.util.logging.config.file=/export/tgrid/sgtest3.0-cloudify/bin/../logging/sgtest_logging.properties " +
        "-Dsgtest.buildFolder=../ " +
        "-DiTests.url=http://192.168.9.121:8087/sgtest3.0-cloudify/ " +
        "-Dcom.gs.work=${config.test.SUITE_WORK_DIR} " +
        "-Dcom.gs.deploy=${config.test.SUITE_DEPLOY_DIR} " +
        "-Dec2.region=${config.test.EC2_REGION} " +
        "-DipList=${config.test.BYON_MACHINES} " +
        "-Dsupported-clouds=${config.test.SUPPORTED_CLOUDS} " +
        "-Dcom.quality.sgtest.credentialsFolder=${context.getServiceDirectory()}/credentials"

try{
    new AntBuilder().sequential{
        chmod(dir:mvnBinDir, perm:'+x', includes:"**/*")
        exec(executable: mvnExec,
                failonerror:true,
                dir:"${serviceDir}/${config.scm.projectName}") {
            env(key: "SUITE_WORK_DIR", value: "${config.test.SUITE_WORK_DIR}")
            env(key: "SUITE_DEPLOY_DIR", value: "${config.test.SUITE_DEPLOY_DIR}")
            env(key: "EXT_JAVA_OPTIONS", value: prefix + "-Dcom.gs.work=${config.test.SUITE_WORK_DIR} -Dcom.gs.deploy=${config.test.SUITE_DEPLOY_DIR}")
            arguments.split(" ").each { arg(value: it) }
        }
    }
}finally{
    context.attributes.thisService.remove "${config.test.TEST_RUN_ID}-${context.instanceId}"
}
