package hosted.scheduled.build

import common.TeamCityDockerImagesRepo.TeamCityDockerImagesRepo
import hosted.utils.ImageInfoRepository
import hosted.utils.steps.buildAndPublishImage
import jetbrains.buildServer.configs.kotlin.v2019_2.AbsoluteId
import jetbrains.buildServer.configs.kotlin.v2019_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.dockerSupport


/**
 * Scheduled build of TeamCity Docker Images for Linux.
 */
object TeamCityScheduledImageBuildLinux : BuildType({
    name = "TeamCity Docker Images - Automated Scheduled Build - Linux"

    vcs {
        root(TeamCityDockerImagesRepo)
    }

    params {
        // the images will be published into registry that holds nightly builds
        param("docker.buildRepository", "%docker.nightlyRepository%")
        // no postfix needed
        param("docker.buildImagePostfix", "")
        param("tc.image.version", "%dockerImage.teamcity.buildNumber%")
    }

    steps {
        ImageInfoRepository
                // args: repository, version
            .getAmdImages("%docker.nightlyRepository%", "%dockerImage.teamcity.buildNumber%")
            .forEach { imageInfo -> buildAndPublishImage(imageInfo) }
    }

    dependencies {
        dependency(AbsoluteId("TC_Trunk_BuildDistDocker")) {
            artifacts {
                artifactRules = "TeamCity.zip!/**=>context/TeamCity"
                cleanDestination = true
                lastSuccessful()
            }
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_315"
            }
        }
    }

    // We retain nightly builds for the previous 2 weeks
    cleanup {
        keepRule {
            id = "TC_DOCKER_IMAGES_SCHEDULED_CLEANUP"
            keepAtLeast = days(14)
            dataToKeep = everything()
            applyPerEachBranch = true
            preserveArtifactsDependencies = true
        }
    }
})
