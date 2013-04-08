service {
    name "space"
    numInstances 1
    maxAllowedInstances 1
    statefulProcessingUnit {
        binaries "iTestsManagementSpace"
        sla {
            memoryCapacity 128
            maxMemoryCapacity 128
            highlyAvailable true
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