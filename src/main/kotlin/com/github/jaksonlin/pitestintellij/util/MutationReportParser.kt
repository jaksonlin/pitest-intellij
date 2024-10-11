package com.github.jaksonlin.pitestintellij.util
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import java.io.File
import java.io.BufferedInputStream
import java.io.FileInputStream
import javax.xml.bind.annotation.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

object MutationReportParser {
    private val xmlMapper = XmlMapper().apply {
        registerModule(KotlinModule.Builder().build())
        // Enable features that might improve performance
        enable(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE)
        enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    fun parseMutationsFromXml(filePath: String): Mutations {
        BufferedInputStream(FileInputStream(File(filePath))).use { inputStream ->
            return xmlMapper.readValue(inputStream, Mutations::class.java)
        }
    }
}

// Data classes for XML structure

data class Mutations(
    val partial: Boolean,
    @JacksonXmlElementWrapper(useWrapping = false)
    val mutation: List<Mutation>
)

data class Mutation(
    val detected: Boolean,
    val status: String,
    val numberOfTestsRun: Int,
    val sourceFile: String,
    val mutatedClass: String,
    val mutatedMethod: String,
    val methodDescription: String,
    val lineNumber: Int,
    val mutator: String,
    val indexes: Indexes,
    val blocks: Blocks,
    val killingTest: String,
    val description: String
)

data class Indexes(
    @JacksonXmlElementWrapper(useWrapping = false)
    val index: List<Int>
)
data class Blocks(
    @JacksonXmlElementWrapper(useWrapping = false)
    val block: List<Int>
)