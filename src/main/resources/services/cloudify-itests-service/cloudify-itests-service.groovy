
/**
 * User: Sagi Bernstein
 * Date: 10/02/13
 * Time: 11:48
 */

service {
    name "cloudify-itests"
    icon "tgrid.jpg"
    type "WEB_SERVER"
    numInstances 1

    lifecycle{
        init "cloudify_itests_install.groovy"
        start "cloudify_itests_start.groovy"
        locator {
            NO_PROCESS_LOCATORS
        }
    }
}