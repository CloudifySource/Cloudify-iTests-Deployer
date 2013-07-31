import com.gigaspaces.document.SpaceDocument
import org.openspaces.core.GigaSpaceConfigurer
import org.openspaces.core.space.UrlSpaceConfigurer

/**
 * User: Sagi Bernstein
 * Date: 10/02/13
 * Time: 11:48
 */

service {
    name "<name>"
    icon "QA.jpg"
    type "WEB_SERVER"
    numInstances "<numInstances>"
    maxAllowedInstances "<numInstances>"

    lifecycle{
        init "itests_install.groovy"
        start "itests_start.groovy"
        locator {
            NO_PROCESS_LOCATORS
        }
        stop "itests_stop.groovy"
    }

    customCommands ([

            "STOP_TESTS" : {
                def gigaSpace = new GigaSpaceConfigurer(
                        new UrlSpaceConfigurer("/./iTestsManagementSpace")).gigaSpace();
                gigaSpace.write(new SpaceDocument()
                        .setTypeName("TestSuiteStatus")
                        .addProperties(['id': "<name>", 'stop' : true]))
            }
    ])

    compute {
        template "MEDIUM_LINUX"
    }

}
