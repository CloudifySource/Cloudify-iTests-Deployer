
/**
 * User: Sagi Bernstein
 * Date: 10/02/13
 * Time: 11:48
 */

service {
    name "<name>"
    icon "tgrid.jpg"
    type "WEB_SERVER"
    numInstances "<numInstances>"

    lifecycle{
        init "cloudify_itests_install.groovy"
        start "cloudify_itests_start.groovy"
        preStop "cloudify_itests_preStop.groovy"
        locator {
            NO_PROCESS_LOCATORS
        }
    }
}