import org.apache.tools.ant.DefaultLogger
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * User: Sagi Bernstein
 * Date: 27/02/13
 * Time: 15:48
 */

//variable definitions
def Logger logger = Logger.getLogger(this.getClass().getName())
def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
def commandOptions="--verbose -timeout 15"
def deployerPropertiesFile = new File("${scriptDir}/deployer.properties")
def config= new ConfigSlurper().parse(deployerPropertiesFile.text)
def props = [:] as Map<String, String>
def i = 0


//function definitions
def cp(from, to){
    new AntBuilder()
        .copy(todir : to){
            fileset(dir : from)
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

def cloudify(arguments, shouldConnect){
    ant = new AntBuilder()
    ant.sequential{
        if(shouldConnect){
            arguments = "connect ${config.MGT_MACHINE};" + arguments
        }
        exec(executable: "./cloudify.sh",
                failonerror:true,
                dir:"${config.CLOUDIFY_HOME}/bin",
                outputProperty: 'output',
                resultProperty: 'result'
                ) {
            arg(value: arguments)
        }
    }
    return ant.project.properties
}

def cloudify(arguments){
    return cloudify (arguments, true)
}

def shouldBootstrap(){
    return cloudify("")['result'] != 0
}

def exitOnError(msg, output, errorCode) {
    logger.severe msg
    logger.severe output as String
    System.exit errorCode as int
}

def counter(toCount) {
    return cloudify("list-attributes -scope service:${props["testRunId"]}")['output'].find("\\{.*\\}").count(toCount)
}


props["<buildNumber>"] = args[i++]         //0
props["<version>"] = args[i++]             //1
props["<milestone>"] = args[i++]           //2
props["<suite.number>"] = args[i++]        //3
props["<suite.name>"] = args[i++]          //4
props["<suite.type>"] = args[i++]          //5
props["<include>"] = args[i++]             //6
props["<exclude>"] = args[i++]             //7
props["<ec2.region>"] = args[i++]          //8
props["<supported.clouds>"] = args[i++]    //9
props["<byon.machines>"] = args[i++]       //10
props["<branch.name>"] = args[i++]         //11
props["<package.name>"] = args[i++]        //12
props["<xap.jdk>"] = args[i++]             //13
props["<sgtest.jdk>"] = args[i++]          //14
props["<sgtest.jvm.settings>"] = args[i]   //15
props["<milestoneUpperCase>"] = "SNAPSHOT" //props["<milestone>"].toUpperCase()
props["testRunId"] = "${props["<suite.name>"]}-${new Date().format 'dd-MM-yyyy-HH-mm-ss' }"


logger.info "strating itests suite with id: ${props["testRunId"]}"

logger.info "checking if management machine is up"
if (shouldBootstrap()){
    logger.info "management is down and should be bootstrapped"
    def bootstrapResults = cloudify("bootstrap-cloud ${commandOptions} ec2", false)
    if (bootstrapResults['result'] != 0){
        exitOnError "bootstrap failed, finishing run", bootstrapResults['output'], bootstrapResults['result']
    }
    config.MGT_MACHINE = bootstrapResults['output'].find("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")

    deployerPropertiesFile.withWriter {
        writer -> config.writeTo(writer)
    }
    def installSQLResults = cloudify "install-service ${commandOptions} ${scriptDir}/../resources/services/mysql"
    if (installSQLResults['result'] != 0){
        logger.severe "bootstrap failed, finishing run"
        def teardownResults = cloudify "teardown-cloud ${commandOptions} ec2"
        if (teardownResults['result'] != 0){
            //TODO send mail
            exitOnError "bootstrap failed, finishing run", teardownResults['output'], teardownResults['result']
            System.exit teardownResults['result'] as int
        }
        exitOnError "installing mysql service failed, teared down ec2 and finishing run", installSQLResults['output'], installSQLResults['result']
    }
    logger.info "importing existing dashboard DB to management machine..."
    //"ssh tgrid@pc-lab24 'mysqldump dashboard SgtestResult | ssh -i ${config.PEM_FILE} -o StrictHostKeyChecking=no ec2-user@${config.MGT_MACHINE} mysql dashboard'".execute().waitFor()
}
logger.info "management is up"



logger.info "copy service dir"
cp "../resources/services/cloudify-itests-service", props["testRunId"]

cp "${config.CREDENTIAL_DIR}", "${props["testRunId"]}/credentials"


logger.info "configure test suite"
props["<mgt.machine>"] = "${config.MGT_MACHINE}"
def servicePropsPath = "${props["testRunId"]}/cloudify-itests.properties"
replaceTextInFile servicePropsPath, props

def serviceFilePath = "${props["testRunId"]}/cloudify-itests-service.groovy"
replaceTextInFile serviceFilePath, ["<name>" : props["testRunId"], "<numInstances>" : props["<suite.number>"]]

logger.info "install service"
def installServiceResults = cloudify "install-service ${commandOptions} ${scriptDir}/${props['testRunId']}"
if (installServiceResults['result'] != 0){
    exitOnError "installing iTests service failed, finishing run", installServiceResults['output'], installServiceResults['result']
}



logger.info "poll for suite completion"
def testRunIdReverse = "${props['testRunId']}".reverse()
int count
while((count = counter(props['testRunId'])) > 0){
    if (counter("failed-${testRunIdReverse}") != 0){
        logger.severe "test run failed, uninstalling service ${props['testRunId']}"
        break
    }
    logger.info "test run ${props["testRunId"]} still has ${count} suites running"
    sleep TimeUnit.MINUTES.toMillis(1)
}

logger.info "uninstall service"
def uninstallResults = cloudify "uninstall-service ${commandOptions} ${props['testRunId']}"
if (uninstallResults['result'] != 0){
    //send mail
    exitOnError "uninstalling the iTests service failed, finishing run", uninstallResults['output'], uninstallResults['result']
}

logger.info "removing ${props['testRunId']} service dir"
new File(props["testRunId"]).deleteDir()

System.exit 0
