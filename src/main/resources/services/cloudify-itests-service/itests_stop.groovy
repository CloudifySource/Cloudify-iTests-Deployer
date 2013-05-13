@Grapes(
        @Grab(group='org.jclouds.api', module='s3', version='1.5.8')
)
import org.cloudifysource.dsl.context.ServiceContextFactory
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.BlobStoreContext

import java.util.logging.Logger

def executeMaven (mvnExec, String arguments, directory){

    def ant = new AntBuilder().exec(executable: mvnExec,
            failonerror:false,
            dir:directory,
            resultProperty: 'result') {
        env(key:'JAVA_HOME',value:"${System.getProperty("user.home")}/java")
        arg(line: arguments)
    }
    return ant.properties.'result'
}


context = ServiceContextFactory.getServiceContext()
Logger logger = Logger.getLogger(this.getClass().getName())

logger.info "instance ${context.instanceId} is shutting down the start script run"

//Only instance 1 does report and mergers
if (context.instanceId == 1){
    logger.info "service instance: 1 - merging and reporting"

    serviceDir = "${System.getProperty("user.home")}/itests-service"
    config = new ConfigSlurper().parse(new File("itests-service.properties").text)

    def mvnExec = "${serviceDir}/maven/apache-maven-${config.maven.version}/bin/mvn"

    strorageProps = new Properties()
    storagePropsStream = new FileInputStream(new File("${context.getServiceDirectory()}/credentials/cloud/ec2/ec2-cred.properties"))
    strorageProps.load storagePropsStream
    storagePropsStream.close()
    storageConfig = new ConfigSlurper().parse(strorageProps)
    provider = 's3'
    blobStore  = ContextBuilder.newBuilder(provider)
            .credentials("${storageConfig.user}", "${storageConfig.apiKey}")
            .buildView(BlobStoreContext.class).getBlobStore()

    containerName = "${config.test.TEST_RUN_ID}".toLowerCase()
    reportDirPath = "${serviceDir}/${config.test.SUITE_NAME}"

    //Download from s3 bucket
    logger.info "downloading the report files"
    blobStore.list(containerName).each {
        def outputFileName = "${reportDirPath}/${it.getName()}"
        logger.info "downloding file ${it.getName()} to ${outputFileName}"
        def input = blobStore.getBlob(containerName, it.getName()).getPayload().getInput()
        def output = new FileOutputStream(new File(outputFileName))
        read = 0
        byte[] bytes = new byte[1024]

        while ((read = input.read(bytes)) != -1) {
            output.write(bytes, 0, read)
        }

        input.close()
        output.flush()
        output.close()
    }

    logger.info "removing the container for the run"
    blobStore.clearContainer(containerName)
    blobStore.deleteContainer(containerName)

    logger.info "running the tests reports merger"
    buildDir = "${serviceDir}/${config.test.BUILD_DIR}"


    type = "${config.test.SUITE_TYPE}".toLowerCase().contains('cloudify') ? 'cloudify' : 'xap'

    executeMaven(mvnExec,
            "exec:java -Dexec.mainClass=\"iTests.framework.testng.report.TestsReportMerger\" -Dexec.args=\"${config.test.SUITE_NAME}"
                    + " ${reportDirPath} ${reportDirPath}\" -D${type}.home=${buildDir} -DiTests.credentialsFolder=${context.getServiceDirectory()}/credentials",
            "${serviceDir}/${config.scm.projectName}")

    logger.info "running the wiki reporter"
    executeMaven(mvnExec,
            "exec:java -Dexec.mainClass=\"iTests.framework.testng.report.wiki.WikiReporter\" -Dexec.args=\"${reportDirPath} ${config.test.SUITE_TYPE} ${config.test.BUILD_NUMBER}"
                    + " ${config.build.version} ${config.build.milestone} \""
                    + " -D${type}.home=${buildDir} -DiTests.credentialsFolder=${context.getServiceDirectory()}/credentials"
                    + " -Dmysql.host=${config.test.MGT_MACHINE} -Dmysql.user=${config.mysql.user} -Dmysql.pass=${config.mysql.pass}",
            "${serviceDir}/${config.scm.projectName}")

}
else{
    logger.info "service instance: ${context.instanceId} - nothing to do on stop"
}

System.exit 0