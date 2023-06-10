package io.github.notstirred.dasmplugin

import com.intellij.json.json5.Json5Language
import com.intellij.openapi.fileTypes.LanguageFileType

object DasmConfigFileType : LanguageFileType(Json5Language.INSTANCE) {
    override fun getName() = "DASM Configuration"

    override fun getDescription() = "DASM configuration"

    override fun getDefaultExtension() = "dasm"

    override fun getIcon() = null
}