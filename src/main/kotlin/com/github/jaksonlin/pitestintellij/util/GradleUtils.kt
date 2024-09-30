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
            }
        }
        return classpath
    }
}