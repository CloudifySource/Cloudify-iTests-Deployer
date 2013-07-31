import org.cloudifysource.dsl.context.ServiceContextFactory

import java.util.concurrent.TimeUnit

config = new ConfigSlurper().parse(new File("xap-agent-service.properties").text)
serviceDir = "${System.getProperty("user.home")}/xap-agent-service"

def context = ServiceContextFactory.getServiceContext()


def builder = new AntBuilder()
builder.mkdir(dir:serviceDir)

def chmod(folder){
    new AntBuilder().chmod(dir: folder, perm:'+x', includes:"**/*")
}

def install(installDir, downloadPath, zipName) {
    new AntBuilder().sequential{
        mkdir(dir:installDir)
        get(src:downloadPath, dest:"${installDir}/${zipName}", skipexisting:true)
        unzip(src:"${installDir}/${zipName}", dest:installDir, overwrite:true)
    }
}

if(context.instanceId == 1){
    def service = context.waitForService(context.serviceName, 20, TimeUnit.SECONDS)
    def instances = service.waitForInstances(service.numberOfPlannedInstances, 30, TimeUnit.SECONDS)
    def locators = "";
    instances.each {
        locators += it.hostAddress + ":" + "${config.port}" + ","
    }
    locators = locators.toString().substring(0, locators.toString().length() -1)
    context.attributes.thisApplication["locators"] = "${locators}"
}

install("${serviceDir}/${config.build.installDir}", config.build.downloadPath, config.build.zipName)
chmod("${serviceDir}/${config.test.BUILD_DIR}/bin")
chmod("${serviceDir}/${config.test.BUILD_DIR}/lib")
chmod("${serviceDir}/${config.test.BUILD_DIR}/tools")


gslicenseAtCloudify =  System.properties["user.home"] +"/gigaspaces/xap-license/gslicense.xml"

builder.sequential {
    copy(file:"${gslicenseAtCloudify}", todir: "${serviceDir}/${config.build.installDir}")
}