import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * User: Sagi Bernstein
 * Date: 27/02/13
 * Time: 15:48
 */



//variable definitions
logger = Logger.getLogger(this.getClass().getName())
scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent
commandOptions = "--verbose -timeout 15"
deployerPropertiesFile = new File("${scriptDir}/deployer.properties")
config = new ConfigSlurper().parse(deployerPropertiesFile.text)
deployerStaticConfigFile = new File("deployer-config.properties")
staticConfig = new ConfigSlurper().parse(deployerStaticConfigFile.text)
props = [:] as Map<String, String>
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
        propsText = propsText.replaceAll(it, properties[it])
    }
    file.write(propsText)
}

def cloudify(arguments, shouldConnect){
    ant = new AntBuilder()
    ant.sequential{
        if(shouldConnect){
            logger.info "connecting to " + ${staticConfig.MGT_MACHINE};
            arguments = "connect ${staticConfig.MGT_MACHINE};" + arguments
        }
        exec(executable: "./cloudify.sh",
                failonerror:false,
                dir:"${config.CLOUDIFY_HOME}/tools/cli",
                outputProperty: 'output',
                resultProperty: 'result'
                ) {arg(value: arguments)}
    }
    return ant.project.properties
}

def cloudify(arguments){
    return cloudify (arguments, true)
}

def shouldBootstrap(){
    return cloudify("")['result'] as int != 0
}

def exitOnError(msg, output, errorCode) {
    logger.severe msg
    logger.severe output as String
    System.exit errorCode as int
}

def getServiceAttributes(){
    int timesToGetServicesAttr = 3;
    def att = cloudify("list-attributes -scope service:${props["testRunId"]}")['output']
    while (att == null && timesToGetServicesAttr > 0){
        logger.info "failed to get service attributes, got ${timesToGetServicesAttr} times to try again"
        timesToGetServicesAttr = timesToGetServicesAttr - 1;
        sleep TimeUnit.MINUTES.toMillis(0.5)
        att = cloudify("list-attributes -scope service:${props["testRunId"]}")['output']
    }
    if (att == null) {
        logger.severe "Error getting service attributes"
        throw new RuntimeException("Error getting service attributes ${props["testRunId"]}")
    }
    return att
}

def counter(toCount) {
    return getServiceAttributes().find("\\{.*\\}").count(toCount)
}

def teardownIfManagementInstallFails(Hashtable installServiceResults) {
    if (installServiceResults['result'] as int != 0) {
        logger.severe "install management failed, finishing run"
        def teardownResults = cloudify "teardown-cloud ${commandOptions} ec2"
        if (teardownResults['result'] as int != 0) {
            //TODO send mail
            exitOnError "teardown failed, finishing run", teardownResults['output'], teardownResults['result']
            System.exit teardownResults['result'] as int
        }
        exitOnError "installing service failed, teared down ec2 and finished run", installServiceResults['output'], installServiceResults['result']
    }
}

def isParamValid(String paramValue){
    return paramValue != null && !paramValue.isEmpty() && !"dummy".equals(paramValue)
}

//start

props['<buildNumber>'] = args[i++]                 //0
props['<version>'] = args[i++]                     //1
props['<milestone>'] = args[i++]                   //2
props['<suite.number>'] = args[i++]                //3
props['<suite.name>'] = args[i++]                  //4
props['<suite.type>'] = args[i++]                  //5
props['<include>'] = args[i++]                     //6
props['<exclude>'] = args[i++]                     //7
props['<ec2.region>'] = args[i++]                  //8
props['<byon.machines>'] = args[i++]               //9
props['<branch.name>'] = args[i++]                 //10
props['<package.name>'] = args[i++]                //11
props['<s3_cloudify_publish_folder>'] = args[i++]  //12
props['<maven.version.xap>'] = args[i++]           //13
props['<maven.version.cloudify>'] = args[i++]      //14
props['<maven.repo.local>'] = args[i++]            //15
props['<enableLogstash>'] = args[i++]              //16
props['<computeTemplate>'] = args[i]               //17
props['testRunId'] = "${props["<suite.name>"]}-${new Date().format 'dd-MM-yyyy-HH-mm-ss' }"
props['<mysql.user>'] = config.MYSQL_USER as String
props['<mysql.pass>'] = config.MYSQL_PASS as String

logger.info "strating itests suite with id: ${props["testRunId"]}"

logger.info "checking if management machine is up"
if (shouldBootstrap()){
    logger.info "management is down and should be bootstrapped..."
    def bootstrapResults = cloudify("bootstrap-cloud ${commandOptions} ec2", false)
    if (bootstrapResults['result'] as int != 0){
        exitOnError "bootstrap failed, finishing run", bootstrapResults['output'], bootstrapResults['result']
    }
    staticConfig.MGT_MACHINE = bootstrapResults['output'].find("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")
    logger.info "writing management ip to " + deployerStaticConfigFile.getAbsolutePath();
    deployerStaticConfigFile.withWriter {writer -> config.writeTo(writer)}

    logger.info "management machine was bootstrapped successfully on ${staticConfig.MGT_MACHINE}"


    logger.info "inject mysql username and password"
    replaceTextInFile "${scriptDir}/../resources/services/iTests-Management/mysql/mysql-service.properties",
            ['dbUser=".*"' : "dbUser=\"${config.MYSQL_USER}\"", 'dbPassW=".*"' : "dbPassW=\"${config.MYSQL_PASS}\""]

    replaceTextInFile "${scriptDir}/../resources/services/iTests-Management/tomcat/tomcat-service.properties",
            ['javaOpts=".*"' : "javaOpts=\"-Dmysql.user=${config.MYSQL_USER} -Dmysql.pass=${config.MYSQL_PASS}\""]

    logger.info "installing iTests-Management application on the management machine..."
    def installResults = cloudify "install-application ${commandOptions} ${scriptDir}/../resources/services/iTests-Management"
    teardownIfManagementInstallFails(installResults)
    logger.info "iTests-Management application was successfully installed on the management machine"

    //logger.info "importing existing dashboard DB to management machine..."
    //"ssh tgrid@pc-lab24 'mysqldump -u sa dashboard SgtestResult | ssh -i ${config.PEM_FILE} -o StrictHostKeyChecking=no ec2-user@${staticC
    // staticConfig.MGT_MACHINE} mysql -u ${config.MYSQL_USER} -p ${config.MYSQL_PASS} dashboard'".execute().waitFor()

}


def testingBuildVersion = "${config.CLOUDIFY_HOME}/bin/platform-info.sh".execute()

def suiteType = props['<suite.type>'].toLowerCase().contains('cloudify') ? 'cloudify' : 'xap'

logger.info """management is up
>>> the tester build is: ${testingBuildVersion.text.trim()}
>>> the tested build is: GigaSpaces ${suiteType.equals('cloudify') ? 'Cloudify' : 'XAP Premium'} ${props['<version>']} ${props['<milestone>'].toUpperCase()} (build ${props['<buildNumber>']})
>>> web-ui is available at http://${staticConfig.MGT_MACHINE}:8099
>>> rest is available at http://${staticConfig.MGT_MACHINE}:8100
>>> dashboard is available at: http://${staticConfig.MGT_MACHINE}:8080/dashboard
>>> test suite is: ${props['<suite.name>']}, split into ${props['<suite.number>']} parts
>>> test suite id is: ${props['testRunId']}
"""



logger.info "copy service dir"
cp "${scriptDir}/../resources/services/${suiteType}-itests-service", props['testRunId']

cp "${config.CREDENTIAL_DIR}", "${props['testRunId']}/credentials"


logger.info "configure test suite"
props["<mgt.machine>"] = "${staticConfig.MGT_MACHINE}"
def servicePropsPath = "${props['testRunId']}/itests-service.properties"
replaceTextInFile servicePropsPath, props

def serviceFilePath = "${props['testRunId']}/${suiteType}-itests-service.groovy"
replaceTextInFile serviceFilePath, ["<name>" : props['testRunId'], "<numInstances>" : props['<suite.number>']]

def serviceComputeTemplate = props['<computeTemplate>'].equals('dummy') ? 'SMALL_LINUX' : 'LARGE_LINUX'
replaceTextInFile serviceFilePath, ["<computeTemplate>" : serviceComputeTemplate]

logger.info "install service"
def installServiceResults = cloudify "install-service -disableSelfHealing ${commandOptions} ${props['testRunId']}"
if (installServiceResults['result'] as int != 0){
    exitOnError "installing iTests service failed, finishing run", installServiceResults['output'], installServiceResults['result']
}



logger.info "poll for suite completion"
def testRunIdReverse = "${props['testRunId']}".reverse()
int count
int status = 0
while((count = counter(props['testRunId'])) > 0){
    if (counter("failed-${testRunIdReverse}") != 0){
        logger.severe "test run failed in service instance with id(s) ${getServiceAttributes().grep("failed-${testRunIdReverse}")}, uninstalling service ${props['testRunId']}"
        status = 1
        break
    }
    logger.info "test run ${props['testRunId']} still has ${count} suites running"
    sleep TimeUnit.MINUTES.toMillis(1)
}

logger.info "uninstalling iTests service..."
def uninstallResults = cloudify "uninstall-service ${commandOptions} ${props['testRunId']}"
if (uninstallResults['result'] as int != 0){
    //send mail
    exitOnError "uninstalling the iTests service failed, finishing run", uninstallResults['output'], uninstallResults['result']
}
logger.info "uninstalled iTest service successfully"

logger.info "removing ${props['testRunId']} service dir"
new File(props['testRunId']).deleteDir()

System.exit status
