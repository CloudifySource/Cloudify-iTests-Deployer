/**
 * User: Sagi Bernstein
 * Date: 10/02/13
 * Time: 11:48
 */

service {
    extend '../../../services/cloudify-itests-service'
    name "test"
    numInstances "2"
    maxAllowedInstances "2"
}
