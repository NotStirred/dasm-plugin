package io.github.notstirred.dasm.plugin.reference

import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import io.github.notstirred.dasm.plugin.DasmConstants.REF

object RefReference: PsiReferenceProvider() {
    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(REF, "string")

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        return arrayOf(Reference(element as PsiLiteral))
    }

    private class Reference(element: PsiLiteral) : PsiReferenceBase<PsiLiteral>(element) {
        override fun resolve(): PsiElement? {
            return JavaPsiFacade.getInstance(element.project).findClass(element.text.substring(1, element.text.length - 1).replace('$', '.'), element.resolveScope)
        }
    }
}