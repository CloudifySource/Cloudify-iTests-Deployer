package deployer

import org.apache.tools.ant.DefaultLogger
import java.util.concurrent.TimeUnit
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
deployerPropertiesFile = new File("${scriptDir}/deployer.properties")
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
            if(it instanceof DefaultLogger){
                it.setOutputPrintStream(new PrintStream(output))
	    }
        }
2    }
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
    return """${config.CLOUDIFY_HOME}/bin/cloudify.sh connect ${config.MGT_MACHINE}""".execute().waitFor() != 0
}



props["<buildNumber>"] = args[i++]         //0
props["<version>"] = args[i++]             //1
props["<milestone>"] = args[i++]           //2
props["<suite.number>"] = args[i++]        //3
props["<suite.name>"] = args[i++]          //4
props["<include>"] = args[i++]             //5
props["<exclude>"] = args[i++]             //6
props["<ec2.region>"] = args[i++]          //7
props["<supported.clouds>"] = args[i++]    //8
props["<byon.machines>"] = args[i++]       //9
props["<branch.name>"] = args[i++]         //10
props["<package.name>"] = args[i++]        //11
props["<xap.jdk>"] = args[i++]             //12
props["<sgtest.jdk>"] = args[i++]          //13
props["<sgtest.jvm.settings>"] = args[i]   //14
props["<milestoneUpperCase>"] = "SNAPSHOT" //props["<milestone>"].toUpperCase()
props["testRunId"] = "${props["<suite.name>"]}-${System.currentTimeMillis()}"


logger.info "strating itests suite with id: ${props["testRunId"]}"

logger.info "checking if management machine is up"
if (shouldBootstrap()){
    logger.info "management is down and should be bootstrapped"
    config.MGT_MACHINE = cloudify("bootstrap-cloud ${commandOptions} ec2", true, false).find("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")
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
while((count = cloudify("list-attributes -scope service:${props["testRunId"]}", true, true).find("\\{.*\\}").count(props["testRunId"])) > 0){
    logger.info "test run ${props["testRunId"]} has still ${count} suites running"
    sleep TimeUnit.MINUTES.toMillis(1)
}

logger.info "uninstall service"
cloudify "uninstall-service ${commandOptions} ${props["testRunId"]}"

logger.info "TODO merge reports and send mail..."
testConfig = new ConfigSlurper().parse(new File(servicePropsPath).toURL())

logger.info "removing ${props["testRunId"]} service dir"
new File(props["testRunId"]).deleteDir()

System.exit 0
