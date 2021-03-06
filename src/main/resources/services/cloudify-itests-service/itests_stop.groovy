@Grapes(
        @Grab(group='org.jclouds.api', module='s3', version='1.5.8')
)
//this import is used in 9.7.0 in new recipes, the tester machine is now 9.6.0 so the old import is used
//import org.cloudifysource.utilitydomain.context.ServiceContextFactory
import org.cloudifysource.dsl.context.ServiceContextFactory
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.BlobStoreContext

import java.util.logging.Logger

import static org.jclouds.blobstore.options.ListContainerOptions.Builder.inDirectory

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

def isParamValid(String paramValue){
    return paramValue != null && !paramValue.isEmpty() && !"dummy".equals(paramValue)
}

context = ServiceContextFactory.getServiceContext()
Logger logger = Logger.getLogger(this.getClass().getName())

logger.info "instance ${context.instanceId} is shutting down the start script run"

//Only instance 1 does report and mergers
if (context.instanceId == 1){
    try{
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

        containerName = "gigaspaces-quality"
        reportDirPath = "${serviceDir}/${config.test.SUITE_NAME}"

        //Download from s3 bucket
        logger.info "trying to download the report files"

        try {
            blobStore.list(containerName, inDirectory("${config.build.buildNumber}/${config.test.SUITE_NAME}"))
                    .grep {return it.getName().contains("sgtest-result-")}
            .each {
                def fileNameSplit = it.getName().split("/")
                def outputFileName = "${reportDirPath}/${fileNameSplit[fileNameSplit.length - 1]}"
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
        }
        catch (Exception e){
            logger.severe("failed to download file ${it.getName()} to ${outputFileName}")
            e.printStackTrace();
        }

        logger.info "running the tests reports merger"
        buildDir = "${serviceDir}/${config.test.BUILD_DIR}"


        type = "${config.test.SUITE_TYPE}".toLowerCase().contains('cloudify') ? 'cloudify' : 'xap'
        profile = "tgrid-${type.equals('cloudify') ? 'cloudify-iTests' : 'sgtest-xap'}"

        mavenRepoLocal = isParamValid("${config.test.MAVEN_REPO_LOCAL}") ? " -Dmaven.repo.local=${config.test.MAVEN_REPO_LOCAL}" : ""

        executeMaven(mvnExec,
                "exec:java -Dexec.mainClass=\"iTests.framework.testng.report.TestsReportMerger\" -Dexec.args=\"${config.test.SUITE_NAME}"
                        + " ${reportDirPath} ${reportDirPath}\" -D${type}.home=${buildDir} -Dbuild.home=${buildDir} -DiTests.credentialsFolder=${context.getServiceDirectory()}/credentials"
                        + mavenRepoLocal + " -P " + profile,
                "${serviceDir}/${config.scm.projectName}")

        logger.info "running the wiki reporter \n " +
                "Arguments are: 1.${reportDirPath}, 2.${config.test.SUITE_TYPE} 3.${config.test.BUILD_NUMBER} 4.${config.build.version} 5.${config.build.milestone}"
        executeMaven(mvnExec,
                "exec:java -Dexec.mainClass=\"iTests.framework.testng.report.wiki.WikiReporter\" -Dexec.args=\"${reportDirPath} ${config.test.SUITE_TYPE} ${config.test.BUILD_NUMBER}"
                        + " ${config.build.version} ${config.build.milestone} \""
                        + " -D${type}.home=${buildDir} -Dbuild.home=${buildDir} -DiTests.credentialsFolder=${context.getServiceDirectory()}/credentials"
                        + " -Dmysql.host=${config.test.MGT_MACHINE} -Dmysql.user=${config.mysql.user} -Dmysql.pass=${config.mysql.pass}"
                        + mavenRepoLocal + " -P " + profile,
                "${serviceDir}/${config.scm.projectName}")
    }
    catch (Exception e){
        logger.severe "caught exception during uninstall - report might not have been sent"
        logger.severe "exception cause: ${e.cause}"
        e.printStackTrace();
    }
}
else{
    logger.info "service instance: ${context.instanceId} - nothing to do on stop"
}
logger.info "service instance: ${context.instanceId} - stopped working exiting..."
System.exit 0
