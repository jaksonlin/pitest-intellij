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
}