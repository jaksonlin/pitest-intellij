package com.github.jaksonlin.pitestintellij.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleUtil

object GradleUtils {
    fun getCompilationOutputPaths(project: Project): List<String> {
        val projectBasePath = project.basePath ?: return emptyList()
        val outputPaths: MutableList<String> = ArrayList()

        for (module in project.modules) {
            val compilerModuleExtension = CompilerModuleExtension.getInstance(module)
            if (compilerModuleExtension != null) {
                val outputPath = compilerModuleExtension.compilerOutputUrl
                if (outputPath != null) {
                    outputPaths.add(outputPath.toString().removePrefix("file://"))
                }
                val testOutputPath = compilerModuleExtension.compilerOutputUrlForTests
                if (testOutputPath != null) {
                    outputPaths.add(testOutputPath.toString().removePrefix("file://"))
                }
            }
        }
        return outputPaths
    }

    fun getTestRunDependencies(project: Project): List<String> {
        val dependencies: MutableSet<String> = mutableSetOf()

        for (module in project.modules) {
            val moduleRootManager = ModuleRootManager.getInstance(module)

            // Get all dependencies, including libraries
            for (orderEntry in moduleRootManager.orderEntries) {
                if (orderEntry is LibraryOrderEntry) {
                    for (file in orderEntry.getRootFiles(OrderRootType.CLASSES)) {
                        dependencies.add(file.path.removeSuffix("!/"))
                    }
                } else {
                    val moduleDependency = orderEntry.ownerModule
                    val moduleDependencyRootManager = ModuleRootManager.getInstance(moduleDependency)
                    for (moduleDependencyOrderEntry in moduleDependencyRootManager.orderEntries) {
                        if (moduleDependencyOrderEntry is LibraryOrderEntry) {
                            for (file in moduleDependencyOrderEntry.getRootFiles(OrderRootType.CLASSES)) {
                                dependencies.add(file.path.removeSuffix("!/"))
                            }
                        }
                    }
                }
            }
        }

        return dependencies.toList()
    }

    fun getResourceDirectories(project: Project): List<String> {
        val testResourceDirectories: MutableList<String> = ArrayList()
        val projectRootManager = ProjectRootManager.getInstance(project)
        val vFiles = projectRootManager.contentSourceRoots
        for (vFile in vFiles) {
            if (vFile.path.endsWith("resources")) {
                testResourceDirectories.add(vFile.path)
            }
        }

        return testResourceDirectories
    }
}