package deployer

@GrabResolver(name = 'openspaces', root = 'http://maven-repository.openspaces.org')
@Grab(group = "com.gigaspaces", module = "gs-openspaces", version = "9.5.0-SNAPSHOT")
@Grab(group = "com.gigaspaces.quality", module = "DashboardReporter", version = "0.0.2-SNAPSHOT")
@Grab(group = "org.jclouds.provider", module = "aws-s3", version = "1.5.3")
@Grab(group = "javax.mail", module = "mail", version = "1.4.5")
@Grab(group = "org.swift.common", module = "confluence-soap", version = "0.5")
@Grab(group = "javax.xml", module = "jaxrpc-api", version = "1.1")


import com.gigaspaces.document.SpaceDocument
import deployer.report.TestsReportMerger
import deployer.report.wiki.WikiReporter
import org.openspaces.core.GigaSpaceConfigurer
import org.openspaces.core.space.UrlSpaceConfigurer

/**
 * User: Sagi Bernstein
 * Date: 27/02/13
 * Time: 15:48
 */
config= new ConfigSlurper().parse(new File("deployer.properties").toURL())
def arguments = [:] as HashMap<String, String>
def i = 0;

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

//copy service dir
def originalServiceDir = "../resources/services/cloudify-itests-service"
def cp(from, to){
    new AntBuilder().sequential{
        copy(todir : to){
            fileset(dir : from)
        }
    }
}

cp(originalServiceDir, arguments["testRunId"])

//copy credentials
def credentialsDir = "${System.getProperty("user.home")}/someDir"
cp(credentialsDir, "${arguments["testRunId"]}/credentials")

//override properties
def serviceProperties = new File("${arguments["testRunId"]}/cloudify-itests.properties") as File
def propsText = serviceProperties.text
arguments.keySet().each {
    propsText = propsText.replace(it, arguments[it])
}
serviceProperties.write(propsText)

//install service
def cloudifysh (String arguments){
    new AntBuilder().sequential{
        exec(executable: "cloudify",
                failonerror:true,
                dir:"CLOIDUFY_HOME") {
            arguments.split(" ").each { arg(value: it) }
        }
    }
}

cloudifysh("install-service ${System.getProperty("user.dir")}/${arguments["testRunId"]}")

//poll for suite completion
def spaceConfigurer = new UrlSpaceConfigurer("jini://HOST/*/testSpace?groups=MANAGEMENT_GROUP");
def suiteSpace = new GigaSpaceConfigurer(spaceConfigurer).gigaSpace();


def template = new SpaceDocument()
template.addProperties(["key" : arguments["testRunId"]])
while(suiteSpace.count(template) > 0){
    sleep(10 * 1000)
}

//uninstall service
cloudifysh("uninstall-service ${arguments["testRunId"]}")

//merge reports
testConfig= new ConfigSlurper().parse(serviceProperties.toURL())

//TODO -Dcloudify.home=${buildDir}
TestsReportMerger.main("${testConfig.test.SUITE_TYPE}",
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
        "${testConfig.test.BUILD_LOG_URL}")

return 0


/*${vars.get("sgtest_client_machine1")}
${vars.get("sgtest_gsa_machines1")}
${vars.get("sgtest_client_machine2")}
${vars.get("sgtest_gsa_machines2")}
${vars.get("sgtest_client_machine3")}
${vars.get("sgtest_gsa_machines3")}
${vars.get("sgtest_client_machine4")}
${vars.get("sgtest_gsa_machines4")}
${vars.get("sgtest_client_machine5")}
${vars.get("sgtest_gsa_machines5")}
${vars.get("sgtest_client_machine6")}
${vars.get("sgtest_gsa_machines6")}
${vars.get("sgtest_client_machine7")}
${vars.get("sgtest_gsa_machines7")}
${vars.get("sgtest_client_machine8")}
${vars.get("sgtest_gsa_machines8")}
${vars.get("sgtest_byon_machines")}*/

