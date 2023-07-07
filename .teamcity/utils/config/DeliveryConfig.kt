package utils.config

/**
 * Holds Delivery-related configuration.
 */
class DeliveryConfig {
    companion object {
        // Configuration within remote TeamCity Instance, thus the ID is stored as string
        const val buildDistDockerDepId = "TC_Trunk_BuildDistDocker"

        // Version must correspond to generated Dockerfiles, as minimal agent is a ...
        // ... base image for regular agent.
        const val tcVersion = "EAP"
    }
}