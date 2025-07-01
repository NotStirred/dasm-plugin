package io.github.notstirred.dasm.plugin.reference

import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import io.github.notstirred.dasm.plugin.DasmConstants.CONSTRUCTOR_TO_FACTORY_REDIRECT
import io.github.notstirred.dasm.plugin.containerTypes

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
        override fun methods(methodName: String): Array<out PsiMethod>? {
            if (methodName != "<init>")
                return null
            return element.findContainingClass()?.containerTypes?.from?.constructors
        }

        override fun getVariants(): Array<out Any?> {
            val fromType = element.findContainingClass()?.containerTypes?.from
            val methods = ArrayList<PsiMethod>()
            fromType?.constructors?.let { methods.addAll(it) }

            return methods
                .map {
                    JavaLookupElementBuilder.forMethod(it, PsiSubstitutor.EMPTY)
                        .withBaseLookupString(
                            StringBuilder()
                                .append("<init>")
                                .append(it.descriptor)
                                .toString()
                        )
                }.toTypedArray()
        }
    }
}
