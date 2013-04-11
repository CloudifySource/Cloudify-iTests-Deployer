import org.cloudifysource.dsl.context.ServiceContextFactory
import org.openspaces.admin.AdminFactory
import org.openspaces.admin.pu.ProcessingUnitDeployment

import java.util.concurrent.TimeUnit

def context = ServiceContextFactory.getServiceContext()

def factory = new AdminFactory();
factory.addGroup(System.getProperty('com.gs.jini_lus.groups'))
def admin = factory.createAdmin()



def pu = admin.getGridServiceManagers().deploy(
        new ProcessingUnitDeployment(
                new File(
                        "${context.serviceDirectory}iTestsManagementSpace/target/iTestsManagementSpace.jar")))

pu.waitForSpace(5, TimeUnit.MINUTES)