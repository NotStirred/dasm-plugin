package io.github.notstirred.dasm.plugin.reference

import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.demonwav.mcdev.util.resolveClass
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import io.github.notstirred.dasm.plugin.DasmConstants.ADD_FIELD_TO_METHOD_TO_SETS
import io.github.notstirred.dasm.plugin.DasmConstants.ADD_FIELD_TO_SETS
import io.github.notstirred.dasm.plugin.containerHierarchy
import io.github.notstirred.dasm.plugin.splatMap

object AddFieldToSetsReference : PsiReferenceProvider() {
    val ADD_FIELD_TO_SETS_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(ADD_FIELD_TO_SETS, "field")
    val ADD_FIELD_TO_METHOD_TO_SETS_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(ADD_FIELD_TO_METHOD_TO_SETS, "field")

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        return arrayOf(Reference(element as PsiLiteral))
    }

    class Reference(element: PsiLiteral) : FieldReference.Reference(element) {
        override fun fields(): Array<out PsiField>? {
            val srcTypeHierarchy = (element.parent?.parent as? PsiAnnotationParameterList)?.attributes?.find { it.name == "containers" }?.value
                ?.resolveClass()?.containerHierarchy()
            return srcTypeHierarchy
                ?.splatMap { it.fields }
                ?.toTypedArray()
        }
    }
}
