service {
    name "space"
    icon "gigaspaces_logo.gif"
    numInstances 1
    statefulProcessingUnit {
        binaries "iTestsManagementSpace/target/iTestsManagementSpace.jar"
        sla {
            memoryCapacity 128
            maxMemoryCapacity 128
            highlyAvailable false
            memoryCapacityPerContainer 128
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