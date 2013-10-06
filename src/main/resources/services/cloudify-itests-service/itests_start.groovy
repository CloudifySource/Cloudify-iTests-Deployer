@Grapes(
        @Grab(group='org.jclouds.api', module='s3', version='1.5.8')
)
//this import is used in 9.7.0 in new recipes, the tester machine is now 9.6.0 so the old import is used
//import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import org.cloudifysource.dsl.context.ServiceContextFactory
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.BlobStoreContext

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

def isParamValid(String paramValue){
    return paramValue != null && !paramValue.isEmpty() && !"dummy".equals(paramValue)
}

logger = Logger.getLogger(this.getClass().getName())
serviceDir = "${System.getProperty("user.home")}/itests-service"
config = new ConfigSlurper().parse(new File("itests-service.properties").text)
context = ServiceContextFactory.getServiceContext()

logger.info "started running instance: ${context.instanceId} of ${config.test.TEST_RUN_ID}"

def testRunResult = 0

def buildDir = "${serviceDir}/${config.test.BUILD_DIR}"

def mvnExec = "${serviceDir}/maven/apache-maven-${config.maven.version}/bin/mvn"

def type = "${config.test.SUITE_TYPE}".toLowerCase().contains('cloudify') ? 'cloudify' : 'xap'
def suiteId = context.instanceId - 1
def arguments = "test -e -U -P tgrid-${type.equals('cloudify') ? 'cloudify-iTests' : 'sgtest-xap'} " +
        "-DiTests.cloud.enabled=true " +
        "-DiTests.buildNumber=${config.test.BUILD_NUMBER} " +
        "-D${type}.home=${buildDir} " +
        "-Dbuild.home=${buildDir} " +
        "-Dincludes=${config.test.INCLUDE} " +
        "-Dexcludes=${config.test.EXCLUDE} " +
        "-Djava.security.policy=policy/policy.all " +
        "-DiTests.suiteName=${config.test.SUITE_NAME} " +
        "-DiTests.suiteType=${config.test.SUITE_TYPE} " +
        "-DiTests.suiteId=${suiteId} " +
        "-DiTests.summary.dir=${serviceDir}/${config.test.SUITE_NAME} " +
        "-DiTests.numOfSuites=${config.test.SUITE_NUMBER} " +
        "-Djava.util.logging.config.file=${serviceDir}/${config.scm.projectName}/src/main/config/sgtest_logging.properties " +
        "-Dsgtest.buildFolder=${serviceDir} " +
        "-Dec2.region=${config.test.EC2_REGION} " +
        "-DipList=${config.test.BYON_MACHINES} " +
        "-DiTests.credentialsFolder=${context.getServiceDirectory()}/credentials " +
        "-Dbranch.name=${config.test.BRANCH_NAME} " +
        "-DgsVersion=${config.test.MAVEN_PROJECTS_VERSION_XAP} " +
        "-DcloudifyVersion=${config.test.MAVEN_PROJECTS_VERSION_CLOUDIFY} " +
        "-DiTests.enableLogstash=${config.test.ENABLE_LOGSTASH}"


if (isParamValid("${config.test.MAVEN_REPO_LOCAL}"))
    arguments += " -Dmaven.repo.local=${config.test.MAVEN_REPO_LOCAL}"

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
        containerName = "gigaspaces-quality"
        //Instance 1 does merger so no need to upload
        if (context.instanceId != 1){
            logger.info "uploading the report file to ${provider}"
            // add blob
            reportName = "sgtest-result-${config.test.SUITE_NAME}${suiteId}.xml"
            def reportFilePath = "${serviceDir}/${config.test.SUITE_NAME}/${reportName}"
            try {
                blob = blobStore.blobBuilder("${config.build.buildNumber}/${config.test.SUITE_NAME}/${reportName}")
                        .payload(new File(reportFilePath)).build()
                blobStore.putBlob(containerName, blob)
                logger.info "Putted to blob ${blob}"
            }
            catch (Exception e){
                logger.severe "Failed to put blob ${blob}"
            }

        }
        else {
            logger.info "First instance ended his tests..Will now wait for all agents to finish their tests"
        }
        context.attributes.thisService.remove "${config.test.TEST_RUN_ID}-${context.instanceId}"
    }
}

