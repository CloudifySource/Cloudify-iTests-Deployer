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
config= new ConfigSlurper().parse(new File("deployer.properties").toURL())
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



def cloudify(String arguments, capture, shouldConnect){
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
            arguments = "connent ${config.MGT_MACHINE};" + arguments
        }
        exec(executable: "./cloudify.sh",
                failonerror:true,
                dir:"${config.CLOUDIFY_HOME}/bin") {
            arguments.split(" ").each { arg(value: it) }
        }
    }
    return output.toString()
}

def cloudify(String arguments){
    cloudify(arguments, false, true)
}

def shouldBootstrap(){
    def connectionStatus = cloudify("", true, true)
    return connectionStatus.contains("Connected successfully")
}

logger.info "strating itests suite with id: ${props["testRunId"]}"

props["<buildNumber>"] = args[i++]                                      //build.number
props["<version>"] = args[i++]                                          //cloudify_product_version
props["<milestone>"] = args[i++]                                        //milestone
props["<milestoneUpperCase>"] = props["<milestone>"].toUpperCase()  //milestone upper case
props["cloudify_package_name"] = args[i++]                              //cloudify_package_name
props["xap_jdk"] = args[i++]                                            //xap_jdk
props["sgtest_jdk"] = args[i++]                                         //sgtest_jdk
props["sgtest_jvm_settings"] = args[i++]                                //sgtest_jvm_settings
props["sgtest_module"] = args[i++]                                      //sgtest_module
props["sgtest_gsa_wan_machines"] = args[i++]                            //sgtest_gsa_wan_machines
props["sgtest_type"] = args[i++]                                        //sgtest_type
props["sgtest_client_mode"] = args[i++]                                 //sgtest_client_mode
props["branch_name"] = args[i++]                                        //branch_name
props["<include>"] = args[i++]                                          //include_list
props["<exclude>"] = args[i++]                                          //exclude_list
props["<suite.name>"] = args[i++]                                       //suite_name
props["svn_tags_and_branches_directory"] = args[i++]                    //svn_tags_and_branches_directory
props["<suite.number>"] = args[i++]                                     //suite_number
props["build.logUrl"] = args[i++]                                       //build.logUrl
props["<ec2.region>"] = args[i++]                                       //ec2_region
props["<supported.clouds>"] = args[i++]                                 //sgtest_clouds
props["testRunId"] = "${props["suite_name"]}-${System.currentTimeMillis()}"
props["<credentials.folder>"] =

logger.info "checking if management machine is up"
if (shouldBootstrap()){
    logger.info "management is down and should be bootstrapped"
    cloudify "bootstrap --verbose ec2", false, false
    cloudify "install-service ${config.MYSQL.serviceDir}", false, true
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
cloudify "install-service --verbose ${System.getProperty("user.dir")}/${props["testRunId"]}"

logger.info "poll for suite completion"
while(cloudify("list-attributes", true, true).count(props["testRunId"]) > 0){
    sleep(TimeUnit.SECONDS.toMillis(10))
}

logger.info "uninstall service"
cloudify "uninstall-service --verbose ${props["testRunId"]}"

logger.info "merge reports"
testConfig = new ConfigSlurper().parse(new File(servicePropsPath).toURL())

//TODO -Dcloudify.home=${buildDir}
/*TestsReportMerger.main("${testConfig.test.SUITE_TYPE}",
        "${testConfig.test.BUILD_NUMBER}",
        "${testConfig.test.SUITE_NAME}",
        "${testConfig.test.MAJOR_VERSION}",
        "${testConfig.test.MINOR_VERSION}")

//send mail
WikiReporter.main("${testConfig.test.SUITE_TYPE}",
        "${testConfig.test.BUILD_NUMBER}",
        "${testConfig.test.SUITE_NAME}",
        "${testConfig.test.MAJOR_VERSION}",
        "${testConfig.test.MINOR_VERSION}",
        "${testConfig.test.BUILD_LOG_URL}")*/

System.exit 0