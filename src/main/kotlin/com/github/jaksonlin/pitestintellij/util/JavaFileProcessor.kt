package com.github.jaksonlin.pitestintellij.util

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import java.io.File

class JavaFileProcessor {
    // Read the Java File and get the main class's fully qualified name
    fun getFullyQualifiedName(file: String): String? {
        val content = File(file).readText()
        val parser = JavaParser()
        val compilationUnit: CompilationUnit = parser.parse(content).result.get()

        // Find the main class in the compilation unit
        val classDeclaration = compilationUnit.findFirst(ClassOrInterfaceDeclaration::class.java).orElse(null)
        return classDeclaration?.let {
            val packageName = compilationUnit.packageDeclaration.map { it.nameAsString }.orElse("")
            if (packageName.isNotEmpty()) "$packageName.${it.nameAsString}" else it.nameAsString
        }
    }
}