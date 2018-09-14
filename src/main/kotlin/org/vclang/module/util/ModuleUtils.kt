package org.vclang.module.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.jetbrains.jetpad.vclang.library.LibraryDependency
import com.jetbrains.jetpad.vclang.module.ModulePath
import com.jetbrains.jetpad.vclang.util.FileUtils
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.vclang.psi.VcFile
import org.vclang.psi.module
import java.nio.file.Path
import java.nio.file.Paths

val Module.defaultRoot: VirtualFile?
    get() = ModuleRootManager.getInstance(this).contentEntries.firstOrNull()?.file

private val YAMLFile.moduleBasePath: Path?
    get() = module?.let { Paths.get(FileUtil.toSystemDependentName(it.moduleFilePath)) }

private fun YAMLFile.getProp(name: String) = (documents?.firstOrNull()?.topLevelValue as? YAMLMapping)?.getKeyValueByKey(name)?.value

val YAMLFile.sourcesDirProp: String?
    get() = (getProp("sourcesDir") as? YAMLScalar)?.textValue

val YAMLFile.outputPath: Path?
    get() {
        val path = Paths.get((getProp("outputDir") as? YAMLScalar)?.textValue ?: ".output")
        return if (path.isAbsolute) path else moduleBasePath?.resolveSibling(path)
    }

val YAMLFile.libModulesProp: List<String>?
    get() = (getProp("modules") as? YAMLSequence)?.items?.mapNotNull { (it.value as? YAMLScalar)?.textValue }

val YAMLFile.libModules: List<ModulePath>
    get() = libModulesProp?.mapNotNull { FileUtils.modulePath(it) } ?: sourcesDirFile?.let { getVcFiles(it).map { it.modulePath } } ?: emptyList()

private fun YAMLFile.getVcFiles(root: VirtualFile): List<VcFile> {
    val result = ArrayList<VcFile>()
    val psiManager = PsiManager.getInstance(project)
    VfsUtilCore.iterateChildrenRecursively(root, null) { file ->
        if (file.name.endsWith(FileUtils.EXTENSION)) {
            (psiManager.findFile(file) as? VcFile)?.let { result.add(it) }
        }
        return@iterateChildrenRecursively true
    }
    return result
}

val YAMLFile.dependencies: List<LibraryDependency>
    get() = (getProp("dependencies") as? YAMLSequence)?.items?.mapNotNull { (it.value as? YAMLScalar)?.textValue?.let { LibraryDependency(it) } } ?: emptyList()

val Module.sourcesDir: String?
    get() {
        val root = defaultRoot?.path
        val dir = libraryConfig?.sourcesDirProp ?: return root
        return when {
            root != null -> Paths.get(root).resolve(dir).toString()
            Paths.get(dir).isAbsolute -> dir
            else -> null
        }
    }

val YAMLFile.sourcesDirFile: VirtualFile?
    get() {
        val root = module?.defaultRoot
        val dir = sourcesDirProp ?: return root
        val path = when {
            root != null -> Paths.get(root.path).resolve(dir).toString()
            Paths.get(dir).isAbsolute -> dir
            else -> return null
        }
        return VirtualFileManager.getInstance().getFileSystem(LocalFileSystem.PROTOCOL).findFileByPath(path)
    }

fun YAMLFile.containsModule(modulePath: ModulePath): Boolean {
    val moduleStr = modulePath.toString()
    return libModulesProp?.any { it == moduleStr } ?: findVcFile(modulePath) != null
}

fun YAMLFile.findVcFilesAndDirectories(modulePath: ModulePath): List<PsiFileSystemItem> {
    var dirs = sourcesDirFile?.let { listOf(it) } ?: return emptyList()
    val path = modulePath.toList()
    val psiManager = PsiManager.getInstance(project)
    for ((i, name) in path.withIndex()) {
        if (i < path.size - 1) {
            dirs = dirs.mapNotNull { it.findChild(name) }
            if (dirs.isEmpty()) return emptyList()
        } else {
            return dirs.mapNotNull {
                val file = it.findChild(name + FileUtils.EXTENSION)
                if (file == null) {
                    it.findChild(name)?.let { psiManager.findDirectory(it) }
                } else {
                    psiManager.findFile(file) as? VcFile
                }
            }
        }
    }
    return emptyList()
}

fun YAMLFile.findVcFile(modulePath: ModulePath): VcFile? =
    findVcFilesAndDirectories(modulePath).filterIsInstance<VcFile>().firstOrNull()

val Module.libraryConfig: YAMLFile?
    get() {
        val virtualFile = defaultRoot?.findChild(FileUtils.LIBRARY_CONFIG_FILE) ?: return null
        return PsiManager.getInstance(project).findFile(virtualFile) as? YAMLFile
    }

val Module.isVcModule: Boolean
    get() = libraryConfig != null
