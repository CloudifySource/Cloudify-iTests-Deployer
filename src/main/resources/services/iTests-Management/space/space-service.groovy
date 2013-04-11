service {
    name "space"
    numInstances 1
    /* CLOUDIFY-1654
    statefulProcessingUnit {
        binaries "iTestsManagementSpace/target/iTestsManagementSpace.jar"
        sla {
            memoryCapacity 128
            maxMemoryCapacity 128
            highlyAvailable false
            memoryCapacityPerContainer 128
        }

    }*/

    lifecycle{
        install "start_space.groovy"
        /*startDetection {
            return ServiceUtils.isPortOccupied(71)
        }*/
        locator {
            NO_PROCESS_LOCATORS
        }
    }
    compute {
        template "MANAGEMENT_LINUX"
    }
    isolationSLA {
        global {
            useManagement true
        }
    }
}