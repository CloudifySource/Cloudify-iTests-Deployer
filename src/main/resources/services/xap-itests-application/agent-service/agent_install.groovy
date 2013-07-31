config = new ConfigSlurper().parse(new File("xap-agent-service.properties").text)
serviceDir = "${System.getProperty("user.home")}/xap-agent-service"

//def context = ServiceContextFactory.getServiceContext()


new AntBuilder().mkdir(dir:serviceDir)

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

install("${serviceDir}/${config.build.installDir}", config.build.downloadPath, config.build.zipName)
chmod("${serviceDir}/${config.test.BUILD_DIR}/bin")
chmod("${serviceDir}/${config.test.BUILD_DIR}/lib")
chmod("${serviceDir}/${config.test.BUILD_DIR}/tools")

