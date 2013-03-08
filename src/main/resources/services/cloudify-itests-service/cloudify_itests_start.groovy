@Grapes(
        @Grab(group='org.jclouds.api', module='s3', version='1.5.8')
)
import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.BlobStoreContext

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * User: Sagi Bernstein
 * Date: 10/02/13
 * Time: 12:06
 */

def executeMaven (mvnExec, String arguments, directory){
    new AntBuilder().exec(executable: mvnExec,
            failonerror:false,
            dir:directory) {
        env(key:'JAVA_HOME',value:"${System.getProperty("user.home")}/java")
        arg(line: arguments)
    }
}



Logger logger = Logger.getLogger(this.getClass().getName())
serviceDir = "${System.getProperty("user.home")}/cloudify-itests-service"
config = new ConfigSlurper().parse(new File("cloudify-itests.properties").text)
context = ServiceContextFactory.getServiceContext()

logger.info "started running instance: ${context.instanceId} of ${config.test.TEST_RUN_ID}"


def buildDir = "${serviceDir}/${config.test.BUILD_DIR}"

def ext = ServiceUtils.isWindows() ? '.bat' : ''
def mvnBinDir = "${serviceDir}/maven/apache-maven-${config.maven.version}/bin"
def mvnExec = "${mvnBinDir}/mvn${ext}"

def arguments = "test -e -X -U -P tgrid-cloudify-iTests " +
        "-Dsgtest.cloud.enabled=true " +
        "-DiTests.buildNumber=${config.test.BUILD_NUMBER} " +
        "-Dcloudify.home=${buildDir} " +
        "-Dincludes=${config.test.INCLUDE} " +
        "-Dexcludes=${config.test.EXCLUDE} " +
        "-Djava.security.policy=policy/policy.all " +
        "-Djava.awt.headless=true " +
        "-Dsgtest.suiteName=${config.test.SUITE_NAME} " +
        "-DiTests.suiteId=${context.instanceId} " +
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
    logger.info "running ${mvnExec} in dir: ${serviceDir}/${config.scm.projectName} with arguments: ${arguments}"
    executeMaven mvnExec, arguments, "${serviceDir}/${config.scm.projectName}"
}finally{
    logger.info "finished running the tests"
    strorageProps = new Properties()
    strorageProps.load new FileInputStream(new File("${context.getServiceDirectory()}/credentials/cloud/ec2/ec2-cred.properties"))
    storageConfig = new ConfigSlurper().parse(strorageProps)
    provider = 's3'
    blobStore  = ContextBuilder.newBuilder(provider)
            .credentials("${storageConfig.user}", "${storageConfig.apiKey}")
            .buildView(BlobStoreContext.class).getBlobStore()

    containerName = "${config.test.TEST_RUN_ID}".toLowerCase()
    //Instance 1 does merger so no need to upload
    if (context.instanceId != 1){
        logger.info "uploading the report file to ${provider}"
        // create container
        blobStore.createContainerInLocation(null, containerName)
        // add blob
        def reportFilePath = "${serviceDir}/${config.test.SUITE_NAME}/sgtest-result-${config.test.SUITE_NAME}${context.instanceId}.xml"
        blob = blobStore.blobBuilder(reportFilePath)
                .payload(new File(reportFilePath)).build()
        blobStore.putBlob(containerName, blob)
    }

    context.attributes.thisService.remove "${config.test.TEST_RUN_ID}-${context.instanceId}"

    while(true){
        try{
            logger.info "waiting for uninstall"
            sleep TimeUnit.MINUTES.toMillis(5)
        }catch(Exception ignored){
            break
        }
    }
}
