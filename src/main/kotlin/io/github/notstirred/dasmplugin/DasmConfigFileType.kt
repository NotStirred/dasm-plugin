package io.github.notstirred.dasmplugin

import com.intellij.json.json5.Json5Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader

object DasmConfigFileType : LanguageFileType(Json5Language.INSTANCE) {
    private val icon = IconLoader.getIcon("/assets/fileIcon.svg", DasmConfigFileType::class.java);

    override fun getName() = "DASM Configuration"

    override fun getDescription() = "DASM configuration"

    override fun getDefaultExtension() = "dasm"

    override fun getIcon() = icon
}