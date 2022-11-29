/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.jetbrains.teamcity

import com.jetbrains.teamcity.common.constants.ValidationConstants
import com.jetbrains.teamcity.docker.exceptions.DockerImageValidationException
import com.jetbrains.teamcity.docker.validation.DockerImageValidationUtilities
import kotlinx.cli.*


/**
 * Subcommand for image validation. Will be consumed by ..
 * ... argument parser.
 */
@OptIn(ExperimentalCli::class)
class ValidateImage: Subcommand("validate", "Validate Docker Image with (optionally) provided credentials.") {
    private val validationArgs by argument(ArgType.String, description = "Image, (optional) Username, (optional) Token").vararg()

    /**
     * Execute image validation option specified via CLI.
     */
    override fun execute() {


        // 1. Capture current image size
        val originalImageName = validationArgs[0]
        val username = if (validationArgs.size > 1) validationArgs[1] else null
        val token = if (validationArgs.size > 2) validationArgs[2] else null
        if (username.isNullOrEmpty() != token.isNullOrEmpty()) {
            // we could tolerate when credentials were not provided at all, but not vise-versa
            throw IllegalArgumentException("If credentials should be used, both username and token must be provided. \n ${super.helpMessage}")
        }

        val percentageChangeThreshold = ValidationConstants.ALLOWED_IMAGE_SIZE_INCREASE_THRESHOLD_PERCENT
        val imagesFailedValidation = DockerImageValidationUtilities.validateImageSize(originalImageName,
            "https://hub.docker.com/v2",
            percentageChangeThreshold, username, token)

        if (imagesFailedValidation.isNotEmpty()) {
            imagesFailedValidation.forEach {
                println("Validation failed for ${originalImageName}, OS: ${it.os}, OS version: ${it.osVersion}, architecture: ${it.architecture}")
            }
            // throw exception in order to handle it within upstream DSL
            throw DockerImageValidationException("Validation had failed for $originalImageName")
        }
    }
}

/**
 * Print out the trend for image sizes.
 */
class PrintImageSizeTrend: Subcommand("get-size-trend", "Print out the trend for the size of given Docker image.") {
    private val imageName by argument(ArgType.String, description = "Image, (optional) logic, (optional) access token").vararg()

    override fun execute() {
        val image = imageName[0]
        val registryUri = "https://hub.docker.com/v2"

        val username = if (imageName.size > 1) imageName[1] else null
        val token = if (imageName.size > 2) imageName[2] else null
        if (username.isNullOrEmpty() != token.isNullOrEmpty()) {
            // we could tolerate when credentials were not provided at all, but not vise-versa
            throw IllegalArgumentException("If credentials should be used, both username and token must be provided. \n ${super.helpMessage}")
        }

        DockerImageValidationUtilities.printImageSizeTrend(image, registryUri, username, token)
    }
}

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) {

    val parser = ArgParser("automation")
    // -- add subcommands
    parser.subcommands(ValidateImage())
    parser.subcommands(PrintImageSizeTrend())

    // Splitting arguments into a list as the "--args" options might be treated as a ...
    // ... single string in non-interactive terminals, thus the parsing could be done incorrectly. ...
    // ... "\\s" is used to also cover non-unicode whitespaces.
    val argsList: Array<String> = if (args.size > 1) args else args[0].split("\\s+".toRegex()).toTypedArray()
    try {
        parser.parse(argsList)
    } catch (e: Exception) {
        println(e.message)
    }
}
