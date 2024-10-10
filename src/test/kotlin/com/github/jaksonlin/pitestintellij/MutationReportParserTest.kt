package com.github.jaksonlin.pitestintellij
import com.github.jaksonlin.pitestintellij.util.MutationReportParser
import org.junit.Assert.*
import org.junit.Test

import java.nio.file.Paths

class MutationReportParserTest {

    @Test
    fun testparseMutationsFromXml() {
        // get the abs path of the file
        val testFilePath = Paths.get("src/test/resources/test_mutations.xml").toAbsolutePath().toString()
        // add timer to check how long it takes to parse the file
        val startTime = System.currentTimeMillis()
        val mutations = MutationReportParser.parseMutationsFromXml(testFilePath)
        val endTime = System.currentTimeMillis()
        println("Time taken to parse the file: ${endTime - startTime} ms")

        assertFalse(mutations.partial)
        assertEquals(1, mutations.mutation.size)

        val mutation = mutations.mutation[0]
        assertTrue(mutation.detected)
        assertEquals("KILLED", mutation.status)
        assertEquals(5, mutation.numberOfTestsRun)
        assertEquals("Example.java", mutation.sourceFile)
        assertEquals("com.example.Example", mutation.mutatedClass)
        assertEquals("exampleMethod", mutation.mutatedMethod)
        assertEquals("()V", mutation.methodDescription)
        assertEquals(10, mutation.lineNumber)
        assertEquals("org.pitest.mutationtest.engine.gregor.mutators.ReturnValsMutator", mutation.mutator)
        assertEquals(listOf(0), mutation.indexes.index)
        assertEquals(listOf(0), mutation.blocks.block)
        assertEquals("com.example.ExampleTest", mutation.killingTest)
        assertEquals("Replaced return of integer sized value with 0", mutation.description)
    }
    @Test
    fun testparseMutationsFromXml2() {
        // get the abs path of the file
        val testFilePath = Paths.get("src/test/resources/mutations.xml").toAbsolutePath().toString()
        // add timer to check how long it takes to parse the file
        val startTime = System.currentTimeMillis()
        val mutations = MutationReportParser.parseMutationsFromXml(testFilePath)
        val endTime = System.currentTimeMillis()
        println("Time taken to parse the file: ${endTime - startTime} ms")

        assertTrue(mutations.partial)
        assertEquals(14, mutations.mutation.size)

        val mutation = mutations.mutation[0]
        assertTrue(mutation.detected)
        assertEquals("KILLED", mutation.status)
        assertEquals(1, mutation.numberOfTestsRun)
        assertEquals("DiffParser.java", mutation.sourceFile)
        assertEquals("com.github.jaksonlin.jacocoparser.util.DiffParser", mutation.mutatedClass)
        assertEquals("parseDiff", mutation.mutatedMethod)
        assertEquals("(Ljava/lang/String;)Lcom/github/jaksonlin/jacocoparser/util/DiffParser\$DiffInfo;", mutation.methodDescription)
        assertEquals(23, mutation.lineNumber)
        assertEquals("org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator", mutation.mutator)
        assertEquals(listOf(62), mutation.indexes.index)
        assertEquals(listOf(11), mutation.blocks.block)
        assertEquals("com.github.jaksonlin.jacocoparser.TestDiffParser.testDiffParser(com.github.jaksonlin.jacocoparser.TestDiffParser)", mutation.killingTest)
        assertEquals("changed conditional boundary", mutation.description)
    }
}