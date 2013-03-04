@Grab(group = "org.eclipse.jgit", module = "org.eclipse.jgit", version = "2.2.0.201212191850-r")

import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * User: Sagi Bernstein
 * Date: 10/02/13
 * Time: 12:05
 */



config = new ConfigSlurper().parse(new File("cloudify-itests.properties").toURL())
serviceDir = System.getProperty("user.home") + "/cloudify-itests-service"
def context = ServiceContextFactory.getServiceContext()


new AntBuilder().sequential{
    mkdir(dir:serviceDir)
}

def install(installDir, downloadPath, zipName) {
    new AntBuilder().sequential{
        mkdir(dir:installDir)
        get(src:downloadPath, dest:"${installDir}/${zipName}", skipexisting:true)
        unzip(src:installDir + "/" + zipName, dest:installDir, overwrite:true)
    }
}

def pool = Executors.newCachedThreadPool()
results = pool.invokeAll([
    { install(serviceDir + "/" + config.cloudify.installDir, config.cloudify.downloadPath, config.cloudify.zipName) },
    { install(serviceDir + "/" + config.maven.installDir, config.maven.downloadPath, config.maven.zipName) },
    {
        switch(config.scm.type){
        
            case "git":
                    def gitDir = new File(serviceDir + "/" + config.scm.projectName)
                    FileRepositoryBuilder repBuilder = new FileRepositoryBuilder()
                    repository = repBuilder.setGitDir(gitDir)
                            .readEnvironment()
                            .findGitDir()
                            .build()
    
                    git = new Git(repository)
                    clone = git.cloneRepository()
                    clone.setDirectory(gitDir)
                            .setURI("${config.git.checkoutUrl}")
                    clone.call()
                break

            case "svn":
                    install(serviceDir + "/" + config.svn.installDir, config.svn.downloadPath, config.svn.zipName)
                    ext = ServiceUtils.isWindows() ? ".bat" : ""
                    svnBinDir = "${serviceDir}/svn/svnkit-${config.svn.version}/bin"
                    new AntBuilder().sequential{
                        chmod(dir:svnBinDir, perm:'+x', includes:'**/*')
                        exec(executable:svnBinDir + "/jsvn" + ext,
                                failonerror:true){
                            arg(value:"co")
                            arg(value:"${config.svn.checkoutUrl}")
                            arg(value:"--force")
                            arg(value:"${serviceDir}/${config.scm.projectName}")
                        }
                    }
        }
    }])
try{
    results.each { it.get(15, TimeUnit.MINUTES) }
} finally {
    pool.shutdownNow()
}


context.attributes.thisService["${config.test.TEST_RUN_ID}-${context.getInstanceId()}"] = new Date().format 'dd/MM/yyyy-hh:mm:ss'