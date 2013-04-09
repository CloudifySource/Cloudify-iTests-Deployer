/**
 * User: Sagi Bernstein
 * Date: 10/02/13
 * Time: 11:48
 */

service {
    name "<name>"
    icon "qa.JPG"
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
            // A command with two parameters (firstName and lastName)
            "STOP_TESTS" : {toPrint ->
                println toPrint
            }
    ])

}
