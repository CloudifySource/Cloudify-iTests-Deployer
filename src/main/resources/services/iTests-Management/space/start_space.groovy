import org.cloudifysource.dsl.context.ServiceContextFactory
import org.openspaces.admin.AdminFactory
import org.openspaces.admin.gsa.GridServiceManagerOptions
import org.openspaces.admin.pu.ProcessingUnitDeployment

import java.util.concurrent.TimeUnit

def context = ServiceContextFactory.getServiceContext()
def admin = new AdminFactory().addGroup(System.getProperty('com.gs.jini_lus.groups')).createAdmin()
def agent = admin.getGridServiceAgents().waitForAtLeastOne()
def gsm = agent.startGridServiceAndWait(new GridServiceManagerOptions())
def pu = gsm.deploy(
        new ProcessingUnitDeployment(
                new File(
                        "${context.serviceDirectory}/iTestsManagementSpace/target/iTestsManagementSpace.jar")))

pu.waitForSpace(5, TimeUnit.MINUTES)
