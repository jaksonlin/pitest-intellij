package com.github.jaksonlin.pitestintellij.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileUtils {
    fun findTargetClassFile(sourceRoots: List<Path>, targetClass: String): TargetClassInfo? {
        for (sourceRoot in sourceRoots) {
            val targetClassFile = findFileRecursively(sourceRoot, targetClass)
            if (targetClassFile != null) {
                return targetClassFile
            }
        }
        return null
    }

    private fun findFileRecursively(directory: Path, targetClass: String): TargetClassInfo? {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return null
        }

        // Determine if the targetClass is a fully qualified name
        val targetFileName = if (targetClass.contains('.')) {
            // Convert fully qualified class name to path
            targetClass.replace('.', File.separatorChar) + ".java"
        } else {
            "$targetClass.java"
        }
        for (file in Files.walk(directory).filter { path -> Files.isRegularFile(path) }.toList()) {
            if (file.toString().endsWith(targetFileName)) {
                val directoryPath = file.parent
                val indexToSrcMainJava = directoryPath.toString().indexOf("src${File.separator}main${File.separator}java")
                val sourceRootPath = directoryPath.toString().substring(0, indexToSrcMainJava)
                return TargetClassInfo(file, Paths.get(sourceRootPath))
            }
        }
        return null
    }
}

data class TargetClassInfo(val file: Path, val sourceRoot: Path)