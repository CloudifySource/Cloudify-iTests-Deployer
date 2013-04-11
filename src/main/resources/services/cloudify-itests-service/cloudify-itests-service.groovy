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
        init "cloudify_itests_install.groovy"
        start "cloudify_itests_start.groovy"
        locator {
            NO_PROCESS_LOCATORS
        }
        stop "cloudify_itests_stop.groovy"
    }

    customCommands ([

            "STOP_TESTS" : {
                def gigaSpace = new GigaSpaceConfigurer(new UrlSpaceConfigurer("/./iTestsManagementSpace")).gigaSpace();
                gigaSpace.write(new SpaceDocument().addProperties(['id': "<name>", 'stop' : true]))
            }
    ])

}
