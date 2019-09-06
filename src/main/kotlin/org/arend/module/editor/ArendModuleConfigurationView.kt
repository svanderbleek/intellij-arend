package org.arend.module.editor

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.panel
import org.arend.library.LibraryDependency
import org.arend.module.config.ArendModuleConfiguration
import org.arend.util.checked
import org.arend.util.labeled
import javax.swing.BorderFactory
import javax.swing.JTextField


class ArendModuleConfigurationView(project: Project?, root: String?, name: String? = null) : ArendModuleConfiguration {
    private val moduleRoot = root?.let { FileUtil.toSystemDependentName(it) }

    private val textComponentAccessor = object : TextComponentAccessor<JTextField> {
        override fun getText(component: JTextField) =
            toAbsolute(moduleRoot, component.text)

        override fun setText(component: JTextField, text: String) {
            component.text = toRelative(moduleRoot, text)
        }
    }

    private val sourcesTextField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Sources directory", "Select the directory in which the source files${if (name == null) "" else " of module $name"} are located", project, FileChooserDescriptorFactory.createSingleFolderDescriptor(), textComponentAccessor)
    }
    private val binariesSwitch = JBCheckBox("Save typechecker output to ", false)
    private val binariesTextField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Binaries directory", "Select the directory in which the binary files${if (name == null) "" else " of module $name"} will be put", project, FileChooserDescriptorFactory.createSingleFolderDescriptor(), textComponentAccessor)
    }
    private val libRootTextField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener("Path to libraries", "Select the directory in which dependencies${if (name == null) "" else " of module $name"} are located", project, FileChooserDescriptorFactory.createSingleFolderDescriptor())
    }

    override var librariesRoot: String
        get() = libRootTextField.text
        set(value) {
            libRootTextField.text = value
        }

    override var sourcesDir: String
        get() = sourcesTextField.text
        set(value) {
            sourcesTextField.text = value
        }

    override var withBinaries: Boolean
        get() = binariesSwitch.isSelected
        set(value) {
            binariesSwitch.isSelected = value
        }

    override var binariesDirectory: String
        get() = binariesTextField.text
        set(value) {
            binariesTextField.text = value
        }

    override var dependencies: List<LibraryDependency> = emptyList() // TODO

    fun createComponent() = panel {
        labeled("Sources directory: ", sourcesTextField)
        checked(binariesSwitch, binariesTextField)

        titledRow("Libraries") {
            labeled("Path to libraries: ", libRootTextField)
        }
    }.apply {
        border = BorderFactory.createEmptyBorder(0, 10, 0, 10)
    }
}