@Grapes(
        @Grab(group='org.jclouds.api', module='s3', version='1.5.8')
)
import org.cloudifysource.dsl.context.ServiceContextFactory
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
    def ant = new AntBuilder()
    ant.exec(executable: mvnExec,
            failonerror : false,
            dir : directory,
            newEnvironment : true,
            resultProperty : 'result') {
        env(key:'JAVA_HOME',value:"${System.getProperty("user.home")}/java")
        arg(line: arguments)
    }
    return ant.project.properties.'result' as int
}

logger = Logger.getLogger(this.getClass().getName())
serviceDir = "${System.getProperty("user.home")}/cloudify-itests-service"
config = new ConfigSlurper().parse(new File("cloudify-itests.properties").text)
context = ServiceContextFactory.getServiceContext()

logger.info "started running instance: ${context.instanceId} of ${config.test.TEST_RUN_ID}"

def testRunResult = 0

def buildDir = "${serviceDir}/${config.test.BUILD_DIR}"

def mvnExec = "${serviceDir}/maven/apache-maven-${config.maven.version}/bin/mvn"

def suiteId = context.instanceId - 1
def arguments = "test -e -U -P tgrid-cloudify-iTests " +
        "-DiTests.cloud.enabled=true " +
        "-DiTests.buildNumber=${config.test.BUILD_NUMBER} " +
        "-Dcloudify.home=${buildDir} " +
        "-Dincludes=${config.test.INCLUDE} " +
        "-Dexcludes=${config.test.EXCLUDE} " +
        "-Djava.security.policy=policy/policy.all " +
        "-Djava.awt.headless=true " +
        "-DiTests.suiteName=${config.test.SUITE_NAME} " +
        "-DiTests.suiteType=${config.test.SUITE_TYPE} " +
        "-DiTests.suiteId=${suiteId} " +
        "-DiTests.summary.dir=${serviceDir}/${config.test.SUITE_NAME} " +
        "-DiTests.numOfSuites=${config.test.SUITE_NUMBER} " +
        "-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger " +
        "-Dcom.gs.logging.level.config=true " +
        "-Djava.util.logging.config.file=${serviceDir}/${config.scm.projectName}/src/main/config/sgtest_logging.properties " +
        "-Dsgtest.buildFolder=${serviceDir} " +
        "-Dec2.region=${config.test.EC2_REGION} " +
        "-DipList=${config.test.BYON_MACHINES} " +
        "-Dsupported-clouds=${config.test.SUPPORTED_CLOUDS} " +
        "-DiTests.credentialsFolder=${context.getServiceDirectory()}/credentials " +
        "-Dbranch.name=${config.test.BRANCH_NAME}"

try{
    logger.info "running ${mvnExec} in dir: ${serviceDir}/${config.scm.projectName} with arguments: ${arguments}"
    testRunResult = executeMaven mvnExec, arguments, "${serviceDir}/${config.scm.projectName}"
}finally{
    if (testRunResult != 0){
        logger.severe "error while running the tests, exited with error: ${testRunResult}"
        def testRunIdReverse = "${config.test.TEST_RUN_ID}".reverse()
        context.attributes.thisService["failed-${testRunIdReverse}"] = context.instanceId
    } else {
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
            reportName = "sgtest-result-${config.test.SUITE_NAME}${suiteId}.xml"
            def reportFilePath = "${serviceDir}/${config.test.SUITE_NAME}/${reportName}"
            blob = blobStore.blobBuilder(reportName)
                    .payload(new File(reportFilePath)).build()
            blobStore.putBlob(containerName, blob)
        }
        context.attributes.thisService.remove "${config.test.TEST_RUN_ID}-${context.instanceId}"
    }
}

