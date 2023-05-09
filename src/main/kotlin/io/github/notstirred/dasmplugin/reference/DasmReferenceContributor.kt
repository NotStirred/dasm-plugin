package io.github.notstirred.dasmplugin.reference

import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import io.github.notstirred.dasmplugin.DasmConfigFileType

class DasmReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val pattern = PlatformPatterns.psiElement(JsonStringLiteral::class.java)
            .inFile(PlatformPatterns.psiFile().withFileType(StandardPatterns.`object`(DasmConfigFileType)))

        registrar.registerReferenceProvider(pattern, DasmReferenceProvider)
    }
}