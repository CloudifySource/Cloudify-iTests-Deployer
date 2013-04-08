service {
    name "space"
    numInstances 1
    maxAllowedInstances 1
    statefulProcessingUnit {
        binaries "iTestsManagementSpace"
        sla {
            memoryCapacity 512
            maxMemoryCapacity 512
            highlyAvailable true
            memoryCapacityPerContainer 128
        }

    }
}