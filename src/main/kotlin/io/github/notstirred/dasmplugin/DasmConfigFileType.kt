package io.github.notstirred.dasmplugin

import com.intellij.json.JsonLanguage
import com.intellij.openapi.fileTypes.LanguageFileType

object DasmConfigFileType : LanguageFileType(JsonLanguage.INSTANCE) {
    override fun getName() = "DASM Configuration"

    override fun getDescription() = "DASM configuration"

    override fun getDefaultExtension() = "dasm"

    override fun getIcon() = null
}