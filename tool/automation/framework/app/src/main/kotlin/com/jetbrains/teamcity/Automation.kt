/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.jetbrains.teamcity

import com.jetbrains.teamcity.common.MathUtils
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.Subcommand
import kotlinx.cli.vararg
import java.lang.IllegalArgumentException
import com.jetbrains.teamcity.common.constants.ValidationConstants
import com.jetbrains.teamcity.docker.DockerImage
import com.jetbrains.teamcity.docker.exceptions.DockerImageValidationException
import com.jetbrains.teamcity.docker.validation.ImageValidationUtils
import com.jetbrains.teamcity.docker.hub.data.DockerRegistryAccessor
import com.jetbrains.teamcity.teamcity.TeamCityUtils



/**
 * Subcommand for image validation. Will be consumed by ..
 * ... argument parser.
 */
class ValidateImage: Subcommand("validate", "Validate Docker Image") {
    private val imageNames by argument(ArgType.String, description = "Images").vararg()

    /**
     * Execute image validation option specified via CLI.
     */
    override fun execute() {
        if (imageNames.size > 2) {
            throw IllegalArgumentException("Too many image names")
        }

        // 1. Capture current image size
        val registryAccessor = DockerRegistryAccessor("https://hub.docker.com/v2")
        val currentImage = DockerImage(imageNames[0])
        val size = registryAccessor.getSize(currentImage)
        TeamCityUtils.reportTeamCityStatistics("SIZE-${ImageValidationUtils.getImageStatisticsId(currentImage.toString())}", size)


        // 2. Get size of previous image
        val previousImage = if (imageNames.size > 1) DockerImage(imageNames[1]) else ImageValidationUtils.getPrevDockerImageId(currentImage)
        if (previousImage == null) {
            println("Unable to determine previous instance of image $currentImage")
            return
        }

        val previousImageSize = registryAccessor.getSize(previousImage)
        val percentageIncrease = MathUtils.getPercentageIncrease(size.toLong(), previousImageSize.toLong())

        // 3. Compare the sizes
        if (percentageIncrease > ValidationConstants.ALLOWED_IMAGE_SIZE_INCREASE_THRESHOLD_PERCENT) {
            throw DockerImageValidationException("Image $currentImage size compared to previous ($previousImage) " +
                    "suppresses ${ValidationConstants.ALLOWED_IMAGE_SIZE_INCREASE_THRESHOLD_PERCENT}% threshold.")
        }
    }
}

fun main(args: Array<String>) {

    args.forEach {
        println("Argument: $it")
    }

    println("[Automation] Arguments are " + args)
    println("[Automation] Arguments size is " + args.size)
    val parser = ArgParser("automation")
    val imageValidation = ValidateImage()
    parser.subcommands(imageValidation)
    // prevent issue with launching the task from Gradle with non-interactive terminal
    parser.skipExtraArguments = true

    parser.parse(args)
}
