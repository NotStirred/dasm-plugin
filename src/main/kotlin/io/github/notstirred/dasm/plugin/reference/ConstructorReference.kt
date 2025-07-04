package io.github.notstirred.dasm.plugin.reference

import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import io.github.notstirred.dasm.plugin.DasmConstants.CONSTRUCTOR_TO_FACTORY_REDIRECT
import io.github.notstirred.dasm.plugin.splatMap

object ConstructorReference : PsiReferenceProvider() {
    val PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(CONSTRUCTOR_TO_FACTORY_REDIRECT)

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        return arrayOf(Reference(element as PsiLiteral))
    }

    class Reference(element: PsiLiteral) : MethodReference.Reference(element) {
        override fun members(): Array<out PsiMember> {
            return this.sourceClass()
                .splatMap { it.constructors }
                .toTypedArray()
        }
    }
}
