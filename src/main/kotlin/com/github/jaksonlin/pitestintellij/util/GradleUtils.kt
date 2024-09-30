package com.github.jaksonlin.pitestintellij.util

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency
import java.io.File

object GradleUtils {
    fun getClasspath(projectPath: String): List<String> {
        val classpath = mutableListOf<String>()
        val connector = GradleConnector.newConnector().forProjectDirectory(File(projectPath))
        connector.connect().use { connection ->
            val ideaProject = connection.getModel(IdeaProject::class.java)
            ideaProject.modules.forEach { ideaModule ->
                ideaModule.dependencies.forEach { dependency ->
                    if (dependency is IdeaSingleEntryLibraryDependency) {
                        classpath.add(dependency.file.absolutePath)
                    }
                }
                ideaModule.gradleProject.buildDirectory?.let { buildDir ->
                    classpath.add(File(buildDir, "classes/java/main").absolutePath)
                    classpath.add(File(buildDir, "classes/java/test").absolutePath)
                }
                // in our project some additional class resides in resource directory
                ideaModule.contentRoots.forEach { contentRoot ->
                    for (resourceDirectory in contentRoot.testResourceDirectories) {
                        classpath.add(resourceDirectory.toString())
                    }
                }
            }
        }
        return classpath
    }

    fun getResourceDirectories(projectPath: String): List<String> {
        val resourceDirectories = mutableListOf<String>()
        val connector = GradleConnector.newConnector().forProjectDirectory(File(projectPath))
        connector.connect().use { connection ->
            val ideaProject = connection.getModel(IdeaProject::class.java)
            ideaProject.modules.forEach { ideaModule ->
                ideaModule.contentRoots.forEach { contentRoot ->
                    for (resourceDirectory in contentRoot.resourceDirectories) {
                        resourceDirectories.add(resourceDirectory.toString())
                    }
                }
            }
        }
        return resourceDirectories
    }

    fun getSourceCodeDirectories(projectPath: String): List<String> {
        val sourceDirectories = mutableListOf<String>()
        val connector = GradleConnector.newConnector().forProjectDirectory(File(projectPath))
        connector.connect().use { connection ->
            val ideaProject = connection.getModel(IdeaProject::class.java)
            ideaProject.modules.forEach { ideaModule ->
                ideaModule.contentRoots.forEach { contentRoot ->
                    for (sourceDirectory in contentRoot.sourceDirectories) {
                        sourceDirectories.add(sourceDirectory.toString())
                    }
                }
            }
        }
        return sourceDirectories
    }

    fun getTestCodeDirectories(projectPath: String): List<String> {
        val testDirectories = mutableListOf<String>()
        val connector = GradleConnector.newConnector().forProjectDirectory(File(projectPath))
        connector.connect().use { connection ->
            val ideaProject = connection.getModel(IdeaProject::class.java)
            ideaProject.modules.forEach { ideaModule ->
                ideaModule.contentRoots.forEach { contentRoot ->
                    for (testDirectory in contentRoot.testDirectories) {
                        testDirectories.add(testDirectory.toString())
                    }
                }
            }
        }
        return testDirectories
    }
}