package deployer

import org.apache.tools.ant.DefaultLogger

import java.util.concurrent.TimeUnit

/*@Grapes([
    @GrabResolver(name = 'openspaces', root = 'http://maven-repository.openspaces.org'),
    @Grab(group = "com.gigaspaces", module = "gs-openspaces", version = "9.5.0-SNAPSHOT"),
    @Grab(group = "com.gigaspaces.quality", module = "DashboardReporter", version = "0.0.2-SNAPSHOT"),
    @Grab(group = "org.jclouds.provider", module = "aws-s3", version = "1.5.3"),
    @Grab(group = "javax.mail", module = "mail", version = "1.4.5"),
    @Grab(group = "org.swift.common", module = "confluence-soap", version = "0.5"),
    @Grab(group = "javax.xml", module = "jaxrpc-api", version = "1.1"),
])
import deployer.report.TestsReportMerger
import deployer.report.wiki.WikiReporter*/

import java.util.logging.Logger

/**
 * User: Sagi Bernstein
 * Date: 27/02/13
 * Time: 15:48
 */

//variable definitions
Logger logger = Logger.getLogger(this.getClass().getName())
scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
commandOptions="--verbose -timeout 15"
deployerPropertiesFile = new File("deployer.properties")
config= new ConfigSlurper().parse(deployerPropertiesFile.toURL())
def props = [:] as Map<String, String>
def i = 0


//function definitions
def cp(from, to){
    new AntBuilder().sequential{
        copy(todir : to){
            fileset(dir : from)
        }
    }
}

def replaceTextInFile(String filePath, Map<String, String> properties){
    def file = new File(filePath) as File
    def propsText = file.text
    for (it in properties.keySet()) {
        propsText = propsText.replace(it, properties[it])
    }
    file.write(propsText)
}



def cloudify(arguments, capture, shouldConnect){
    def output = new ByteArrayOutputStream()
    ant = new AntBuilder()
    if (capture){
        ant.project.getBuildListeners().each {
            if(it instanceof DefaultLogger)
                it.setOutputPrintStream(new PrintStream(output))
        }
    }
    ant.sequential{
        if(shouldConnect){
            arguments = "connect ${config.MGT_MACHINE};" + arguments
        }
        exec(executable: "./cloudify.sh",
                failonerror:true,
                dir:"${config.CLOUDIFY_HOME}/bin") {
            arg(value: arguments)
        }
    }
    return output.toString()
}

def cloudify(arguments){
    cloudify(arguments, false, true)
}

def shouldBootstrap(){
    def connectionStatus = cloudify("connect -timeout 1 ${config.MGT_MACHINE}", true, false)
    return !connectionStatus.contains("Connected successfully")
}



/*props["package_name"] = args[i++]                                       //cloudify_package_name
props["xap_jdk"] = args[i++]                                            //xap_jdk
props["sgtest_jdk"] = args[i++]                                         //sgtest_jdk
props["sgtest_jvm_settings"] = args[i++]                                //sgtest_jvm_settings
props["branch_name"] = args[i++]                                        //branch_name
props["<include>"] = args[i++]                                          //include_list
props["<exclude>"] = args[i++]                                          //exclude_list
props["build.logUrl"] = args[i++]                                       //build.logUrl
props["<ec2.region>"] = args[i++]                                       //ec2_region
props["<supported.clouds>"] = args[i++]                                 //sgtest_clouds*/



props["<buildNumber>"] = args[i++]                                      //build.number
props["<version>"] = args[i++]                                          //cloudify_product_version
props["<milestone>"] = args[i++]                                        //milestone
props["<milestoneUpperCase>"] = "SNAPSHOT"//props["<milestone>"].toUpperCase()      //milestone upper case
props["<suite.number>"] = args[i++]                                     //suite_number
props["<suite.name>"] = args[i++]                                       //suite_name
props["testRunId"] = "${props["<suite.name>"]}-${System.currentTimeMillis()}"

logger.info "strating itests suite with id: ${props["testRunId"]}"

logger.info "checking if management machine is up"
if (shouldBootstrap()){
    logger.info "management is down and should be bootstrapped"
    config.MGT_MACHINE = cloudify("bootstrap ${commandOptions} ec2", true, false).find("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")
    deployerPropertiesFile.withWriter {
        writer -> config.writeTo(writer)
    }
    cloudify "install-service ${commandOptions} ${scriptDir}/../resources/services/mysql", false, true
}
logger.info "management is up"



logger.info "copy service dir"
cp "../resources/services/cloudify-itests-service", props["testRunId"]

cp "${config.CREDENTIAL_DIR}", "${props["testRunId"]}/credentials"


logger.info "configure test suite"
def servicePropsPath = "${props["testRunId"]}/cloudify-itests.properties"
replaceTextInFile servicePropsPath, props

def serviceFilePath = "${props["testRunId"]}/cloudify-itests-service.groovy"
replaceTextInFile serviceFilePath, ["<name>" : props["testRunId"], "<numInstances>" : props["<suite.number>"]]

logger.info "install service"
cloudify "install-service ${commandOptions} ${scriptDir}/${props["testRunId"]}"

logger.info "poll for suite completion"
int count
while((count = cloudify("list-attributes -scope service:${props["testRunId"]}", true, true).count(props["testRunId"])) > 0){
    logger.info "test run ${props["suite_name"]} has still ${count} suites running"
    sleep(TimeUnit.MINUTES.toMillis(1))
}

logger.info "uninstall service"
cloudify "uninstall-service ${commandOptions} ${props["testRunId"]}"

logger.info "TODO merge reports and send mail..."
testConfig = new ConfigSlurper().parse(new File(servicePropsPath).toURL())

logger.info "removing ${props["testRunId"]} service dir"
new File(props["testRunId"]).deleteDir()

System.exit 0
