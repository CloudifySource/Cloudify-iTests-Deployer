@Grapes(
        @Grab(group = 'org.eclipse.jgit', module = 'org.eclipse.jgit', version = '2.2.0.201212191850-r')
)
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



config = new ConfigSlurper().parse(new File("itests-service.properties").text)
serviceDir = "${System.getProperty("user.home")}/itests-service"
type = "${config.test.SUITE_TYPE}".toLowerCase().contains('cloudify') ? 'cloudify' : 'xap'

def context = ServiceContextFactory.getServiceContext()


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

def pool = Executors.newCachedThreadPool()
results = pool.invokeAll([
        { install("${serviceDir}/${config.build.installDir}", config.build.downloadPath, config.build.zipName)
            chmod("${serviceDir}/${config.test.BUILD_DIR}/bin")
            chmod("${serviceDir}/${config.test.BUILD_DIR}/lib")
            chmod("${serviceDir}/${config.test.BUILD_DIR}/tools") },
        { install("${serviceDir}/${config.maven.installDir}", config.maven.downloadPath, config.maven.zipName)
            chmod("${serviceDir}/maven/apache-maven-${config.maven.version}/bin") },
        {
            branchName = 'dummy' == "${config.scm.branchName}" ? 'master' : "${config.scm.branchName}"
            switch(config.scm.type){

                case "git":
                    def gitDir = new File("${serviceDir}/${config.scm.projectName}")
                    FileRepositoryBuilder repBuilder = new FileRepositoryBuilder()
                    repository = repBuilder.setGitDir(gitDir)
                            .readEnvironment()
                            .findGitDir()
                            .build()

                    git = new Git(repository)
                    clone = git.cloneRepository()
                            .setDirectory(gitDir)
                            .setURI("${config.git.checkoutUrl}")
                            .setBranchesToClone([branchName])
                            .setBranch(branchName)
                            .setCloneAllBranches(false)
                    clone.call()
                    break

                case "svn":
                    install("${serviceDir}/${config.svn.installDir}", config.svn.downloadPath, config.svn.zipName)
                    ext = ServiceUtils.isWindows() ? '.bat' : ''
                    svnBinDir = "${serviceDir}/svn/svnkit-${config.svn.version}/bin"
                    new AntBuilder().sequential{
                        chmod(dir:svnBinDir, perm:'+x', includes:'**/*')
                        exec(executable:svnBinDir + "/jsvn" + ext,
                                failonerror:true){
                            arg(value:'co')
                            arg(value:"${config.svn.checkoutUrl}")
                            arg(value:'--force')
                            arg(value:"${serviceDir}/${config.scm.projectName}")
                        }
                    }
                    break

                case "zip":
                    zipName = type.equals('cloudify') ? "Cloudify-iTests-${branchName}" :"SGTest-${branchName}"
                    projectUrl= type.equals('cloudify') ? 'CloudifySource/Cloudify-iTests' : 'GigaSpaces-QA/SGTest'
                    install("${serviceDir}", "https://github.com/${projectUrl}/archive/${branchName}.zip", "${zipName}.zip")
                    new AntBuilder().move(todir : "${serviceDir}/${config.scm.projectName}"){
                        fileset(dir : "${serviceDir}/${zipName}")
                    }

            }
        }])
try{

    results.each { it.get(15, TimeUnit.MINUTES) }
    context.attributes.thisService["${config.test.TEST_RUN_ID}-${context.getInstanceId()}"] = new Date().format 'dd/MM/yyyy-hh:mm:ss'

} catch (Exception e){

    def testRunIdReverse = "${config.test.TEST_RUN_ID}".reverse()
    context.attributes.thisService["failed-${testRunIdReverse}"] = context.instanceId

} finally {

    pool.shutdownNow()

}