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
def arguments = [:] as HashMap<String, String>
def i = 0


//function definitions
def cp(from, to){
    new AntBuilder().sequential{
        copy(todir : to){
            fileset(dir : from)
        }
    }
}

def replaceTextInFile(filePath, props){
    def file = new File(filePath) as File
    def propsText = file.text
    for (it in props.keySet()) {
        propsText = propsText.replace(it, props[it])
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

def cloudify(arguments){
    cloudify(arguments, false, true)
}

def shouldBootstrap(){
    def connectionStatus = cloudify("", true, true)
    return connectionStatus.contains("Connected successfully")
}

logger.info "strating itests suite with id: ${arguments["testRunId"]}"

arguments["<buildNumber>"] = args[i++]                                      //build.number
arguments["<version>"] = args[i++]                                          //cloudify_product_version
arguments["<milestone>"] = args[i++]                                        //milestone
arguments["<milestoneUpperCase>"] = arguments["<milestone>"].toUpperCase()  //milestone upper case
arguments["cloudify_package_name"] = args[i++]                              //cloudify_package_name
arguments["xap_jdk"] = args[i++]                                            //xap_jdk
arguments["sgtest_jdk"] = args[i++]                                         //sgtest_jdk
arguments["sgtest_jvm_settings"] = args[i++]                                //sgtest_jvm_settings
arguments["sgtest_module"] = args[i++]                                      //sgtest_module
arguments["sgtest_gsa_wan_machines"] = args[i++]                            //sgtest_gsa_wan_machines
arguments["sgtest_type"] = args[i++]                                        //sgtest_type
arguments["sgtest_client_mode"] = args[i++]                                 //sgtest_client_mode
arguments["branch_name"] = args[i++]                                        //branch_name
arguments["<include>"] = args[i++]                                          //include_list
arguments["<exclude>"] = args[i++]                                          //exclude_list
arguments["<suite.name>"] = args[i++]                                       //suite_name
arguments["svn_tags_and_branches_directory"] = args[i++]                    //svn_tags_and_branches_directory
arguments["<suite.number>"] = args[i++]                                     //suite_number
arguments["build.logUrl"] = args[i++]                                       //build.logUrl
arguments["<ec2.region>"] = args[i++]                                       //ec2_region
arguments["<supported.clouds>"] = args[i++]                                 //sgtest_clouds
arguments["testRunId"] = "${arguments["suite_name"]}-${System.currentTimeMillis()}"
arguments["<credentials.folder>"] =

logger.info "checking if management machine is up"
if (shouldBootstrap()){
    logger.info "management is down and should be bootstrapped"
    cloudify "bootstrap --verbose ec2", false, false
    cloudify "install-service ${config.MYSQL.serviceDir}", false, true
}
logger.info "management is up"



logger.info "copy service dir"
cp "../resources/services/cloudify-itests-service", arguments["testRunId"]

cp "${config.CREDENTIAL_DIR}", "${arguments["testRunId"]}/credentials"


logger.info "configure test suite"
def servicePropsPath = "${arguments["testRunId"]}/cloudify-itests.properties"
replaceTextInFile servicePropsPath, arguments

def serviceFilePath = "${arguments["testRunId"]}/cloudify-itests-service.groovy"
replaceTextInFile serviceFilePath, ["<name>" : arguments["testRunId"], "<numInstances>" : arguments["<suite.number>"]]

logger.info "install service"
cloudify "install-service --verbose ${System.getProperty("user.dir")}/${arguments["testRunId"]}"

logger.info "poll for suite completion"
while(cloudify("list-attributes", true, true).count(arguments["testRunId"]) > 0){
    sleep(TimeUnit.SECONDS.toMillis(10))
}

logger.info "uninstall service"
cloudify "uninstall-service --verbose ${arguments["testRunId"]}"

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