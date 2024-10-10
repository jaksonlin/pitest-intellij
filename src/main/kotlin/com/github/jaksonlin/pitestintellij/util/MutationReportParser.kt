package com.github.jaksonlin.pitestintellij.util
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import java.io.File
import javax.xml.bind.annotation.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

object MutationReportParser {
    private val xmlMapper = XmlMapper().registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, enabled = false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )

    fun parseMutationsFromXml(filePath: String): Mutations {
        // Deserialize the XML to Kotlin object
        val mutations: Mutations = xmlMapper.readValue(File(filePath), Mutations::class.java)
        return mutations
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