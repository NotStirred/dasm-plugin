package io.github.notstirred.dasm.plugin.reference

import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import io.github.notstirred.dasm.plugin.DasmConstants.TRANSFORM_FROM_METHOD
import io.github.notstirred.dasm.plugin.DasmConstants.TRANSFORM_METHOD
import io.github.notstirred.dasm.plugin.parseClassRef

object OwnerOwningMethodReference : PsiReferenceProvider() {
    val TRANSFORM_METHOD_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(TRANSFORM_METHOD)
    val TRANSFORM_FROM_METHOD_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(TRANSFORM_FROM_METHOD)

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        return arrayOf(Reference(element as PsiLiteral))
    }

    open class Reference(element: PsiLiteral) : MethodReference.Reference(element) {
        override fun sourceClass(): Iterable<PsiClass> {
            val refAnnotation = element.parentOfType<PsiAnnotationParameterList>(false)
                ?.attributes?.find { it.name == "owner" }?.value
            if (refAnnotation is PsiAnnotation) {
                parseClassRef(refAnnotation, element.project)?.let {
                    return listOf(it)
                }
            }
            // no owner attribute, fallback to default (inherit from dasm class)
            return super.sourceClass()
        }
    }
}
