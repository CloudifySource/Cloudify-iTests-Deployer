import org.cloudifysource.dsl.context.ServiceContextFactory
import org.cloudifysource.dsl.utils.ServiceUtils
import org.openspaces.admin.AdminFactory
import org.openspaces.admin.gsa.GridServiceManagerOptions
import org.openspaces.admin.pu.ProcessingUnitDeployment

import java.util.concurrent.TimeUnit

long pid

service {
    name "space"
    numInstances 1
    //CLOUDIFY-1654
    statefulProcessingUnit {
        binaries "iTestsManagementSpace/target/iTestsManagementSpace.jar"
        sla {
            memoryCapacity 128
            maxMemoryCapacity 128
            highlyAvailable false
            memoryCapacityPerContainer 128
        }

    }

    /*lifecycle{
        install{
            def context = ServiceContextFactory.getServiceContext()
            def admin = new AdminFactory().addGroup(System.getProperty('com.gs.jini_lus.groups')).createAdmin()
            def agent = admin.getGridServiceAgents().waitForAtLeastOne()
            def gsm = agent.startGridServiceAndWait(new GridServiceManagerOptions()) //?
            def pu = gsm.deploy(
                    new ProcessingUnitDeployment(
                            new File(
                                    "${context.serviceDirectory}/iTestsManagementSpace/target/iTestsManagementSpace.jar")
                    ).numberOfInstances(1).numberOfBackups(0))

            pu.waitForSpace(5, TimeUnit.MINUTES)
            pid = pu.getInstances()[0].getGridServiceContainer().getVirtualMachine().details.pid
        }
        startDetection {
            return ServiceUtils.isPortOccupied(4174)
        }
        locator {
            return pid
        }
    }*/
    compute {
        template "MANAGEMENT_LINUX"
    }
    isolationSLA {
        global {
            useManagement true
        }
    }
}