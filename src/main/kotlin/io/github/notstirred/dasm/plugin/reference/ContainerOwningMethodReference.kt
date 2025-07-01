package io.github.notstirred.dasm.plugin.reference

import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.demonwav.mcdev.util.resolveClass
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import io.github.notstirred.dasm.plugin.DasmConstants.ADD_METHOD_TO_SETS
import io.github.notstirred.dasm.plugin.containerHierarchy
import io.github.notstirred.dasm.plugin.splatMap

object ContainerOwningMethodReference : PsiReferenceProvider() {
    val ADD_METHOD_TO_SETS_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(ADD_METHOD_TO_SETS, "method")

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        return arrayOf(Reference(element as PsiLiteral))
    }

    open class Reference(element: PsiLiteral) : MethodReference.Reference(element) {
        override fun methods(): Array<out PsiMethod>? {
            return (element.parent?.parent as? PsiAnnotationParameterList)?.attributes?.find { it.name == "containers" }?.value
                ?.resolveClass()?.containerHierarchy()
                ?.splatMap { it.methods }?.toTypedArray()
        }
    }
}
