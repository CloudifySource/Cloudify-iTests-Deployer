@Grapes(
        @Grab(group='org.jclouds.api', module='s3', version='1.5.8')
)
import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.BlobStoreContext

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

    storageConfig = new ConfigSlurper().parse(new File("${context.getServiceDirectory()}/credentials/cloud/ec2/ec2-cred.properties").toURL())
    provider = "s3"
    blobStore  = ContextBuilder.newBuilder(provider)
            .credentials("${storageConfig.user}", "${storageConfig.apiKey}")
            .buildView(BlobStoreContext.class).getBlobStore()

    containerName = "${config.test.TEST_RUN_ID}".toLowerCase()
    //Instance 1 does merger so no need to upload
    if (context.instanceId != 1){
        // create container
        blobStore.createContainerInLocation(null, containerName)
        // add blob
        def reportFilePath = "${serviceDir}/${config.test.SUITE_NAME}/sgtest-result-${config.test.SUITE_NAME}${context.instanceId}.xml"
        blob = blobStore.blobBuilder(reportFilePath)
                .payload(new File(reportFilePath)).build()
        blobStore.putBlob(containerName, blob)
    }
    context.attributes.thisService.remove "${config.test.TEST_RUN_ID}-${context.instanceId}"

    //Only instance 1 does report and mergers
    if (context.instanceId == 1){
        while(context.attributes.thisService.grep("^\\${config.test.TEST_RUN_ID}.*").size() > 0){
            sleep TimeUnit.MINUTES.toMillis(1)
        }
        //Download from s3 bucket
        blobStore.list(containerName).eachParallel {
            def input = blobStore.getBlob(containerName, it.getName()).getPayload().getInput()
            def output = new FileOutputStream(new File("${serviceDir}/${config.test.SUITE_NAME}/${it.getName()}"))
            read = 0
            byte[] bytes = new byte[1024]

            while ((read = input.read(bytes)) != -1) {
                output.write(bytes, 0, read)
            }

            input.close()
            output.flush()
            output.close()
        }

        executeMaven(mvnExec,
                "exec:java -Dexec.mainClass=\"framework.testng.report.TestsReportMerger\" -Dexec.args=\"${config.test.SUITE_TYPE} ${config.test.BUILD_NUMBER}"
                        + " ${serviceDir}/${config.test.SUITE_NAME} ${config.test.MAJOR_VERSION} ${config.test.MINOR_VERSION}\" -Dcloudify.home=${buildDir}",
                "${serviceDir}/${config.scm.projectName}")
        executeMaven(mvnExec,
                "exec:java -Dexec.mainClass=\"framework.testng.report.wiki.WikiReporter\" -Dexec.args=\"${config.test.SUITE_TYPE} ${config.test.BUILD_NUMBER}"
                        + " ${serviceDir}/${config.test.SUITE_NAME} ${config.test.MAJOR_VERSION} ${config.test.MINOR_VERSION} ${config.test.BUILD_LOG_URL}\""
                        + " -Dcloudify.home=${buildDir} -Dmysql.host=${config.test.MGT_MACHINE}",
                "${serviceDir}/${config.scm.projectName}")

        blobStore.clearContainer(containerName)
        blobStore.deleteContainer(containerName)
    }




    while(true){
        try{
            sleep TimeUnit.MINUTES.toMillis(5)
        }catch(Exception e){
            break
        }
    }
}