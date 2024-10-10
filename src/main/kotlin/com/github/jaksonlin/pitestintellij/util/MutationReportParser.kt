package com.github.jaksonlin.pitestintellij.util

import javax.xml.bind.JAXBContext
import java.io.File
import javax.xml.bind.annotation.*

object MutationReportParser {

    fun parseMutationsFromXml(filePath: String): Mutations {
        val jaxbContext = JAXBContext.newInstance(Mutations::class.java)
        val unmarshaller = jaxbContext.createUnmarshaller()

        val file = File(filePath)
        return unmarshaller.unmarshal(file) as Mutations
    }

}


@XmlRootElement(name = "mutations")
@XmlAccessorType(XmlAccessType.FIELD)
data class Mutations(
    @XmlAttribute val partial: Boolean,
    @XmlElement(name = "mutation") val mutations: List<Mutation> = listOf()
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Mutation(
    @XmlAttribute val detected: Boolean,
    @XmlAttribute val status: MutationStatus,
    @XmlAttribute val numberOfTestsRun: Int,
    @XmlElement val sourceFile: String? = null,
    @XmlElement val mutatedClass: String? = null,
    @XmlElement val mutatedMethod: String? = null,
    @XmlElement val methodDescription: String? = null,
    @XmlElement val lineNumber: Int = 0,
    @XmlElement val mutator: String? = null,
    @XmlElement val indexes: Indexes? = null,
    @XmlElement val blocks: Blocks? = null,
    @XmlElement val killingTest: String? = null,
    @XmlElement val description: String? = null
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Indexes(
    @XmlElement(name = "index") val index: List<Int> = listOf()
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Blocks(
    @XmlElement(name = "block") val block: List<Int> = listOf()
)

enum class MutationStatus {
    KILLED, SURVIVED
}