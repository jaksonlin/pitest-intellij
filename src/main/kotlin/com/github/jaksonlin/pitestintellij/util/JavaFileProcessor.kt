package com.github.jaksonlin.pitestintellij.util

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import java.io.File
import kotlin.reflect.jvm.internal.impl.renderer.ClassifierNamePolicy.FULLY_QUALIFIED

data class ClassFileInfo(val fullyQualifiedName: String, val className: String, val packageName: String)
class JavaFileProcessor {
    // Read the Java File and get the main class's fully qualified name
    fun getFullyQualifiedName(file: String): ClassFileInfo? {
        val content = File(file).readText()
        val parser = JavaParser()
        val compilationUnit: CompilationUnit = parser.parse(content).result.get()

        // Find the main class in the compilation unit
        val classDeclaration = compilationUnit.findFirst(ClassOrInterfaceDeclaration::class.java).orElse(null)
        return classDeclaration?.let {
            val packageName = compilationUnit.packageDeclaration.get().nameAsString
            val className = it.nameAsString
            val fullyQualifiedName = if (packageName.isEmpty()) {
                className
            } else {
                "$packageName.$className"
            }
            ClassFileInfo(fullyQualifiedName, className, packageName)
        }
    }

}