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
import io.github.notstirred.dasm.plugin.DasmConstants.FIELD_REDIRECT
import io.github.notstirred.dasm.plugin.DasmConstants.FIELD_TO_METHOD_REDIRECT
import io.github.notstirred.dasm.plugin.dasmSrcType
import org.jetbrains.coverage.org.objectweb.asm.Type

object FieldReference: PsiReferenceProvider() {
    val FIELD_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(FIELD_REDIRECT)
    val FIELD_TO_METHOD_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(FIELD_TO_METHOD_REDIRECT)

    val FIELD_REFERENCE_REGEX = Regex("(?<name>\\S+):(?<desc>\\S+)")

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        return arrayOf(Reference(element as PsiLiteral))
    }

    class Reference(element: PsiLiteral) : PsiReferenceBase<PsiLiteral>(element) {
        override fun resolve(): PsiElement? {
            val text = element.text.substring(1, element.text.length - 1)

            val match = FIELD_REFERENCE_REGEX.find(text) ?: return null
            val fieldType = Type.getType(match.groups["desc"]!!.value)
            val fieldName = match.groups["name"]!!.value

            return element.findContainingClass()?.dasmSrcType?.findFieldByName(fieldName, false)?.let { field ->
                if (field.typeElement!!.type.descriptor != fieldType.descriptor) {
                    return@let null
                }
                return@let field
            }
        }

        override fun getVariants(): Array<out Any?> {
            val fromType = element.findContainingClass()?.dasmSrcType
            val fields = ArrayList<PsiField>()
            fromType?.fields?.let { fields.addAll(it) }

            return fields
                .map {
                    JavaLookupElementBuilder.forField(it)
                        .withTypeText(it.typeElement?.type?.presentableText)
                        .withBaseLookupString(
                            StringBuilder()
                                .append(it.name)
                                .append(':')
                                .append(it.descriptor)
                                .toString()
                        )
                }.toTypedArray()
        }
    }
}
