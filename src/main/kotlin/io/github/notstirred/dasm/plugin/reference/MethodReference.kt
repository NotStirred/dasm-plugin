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
import io.github.notstirred.dasm.plugin.DasmConstants.METHOD_REDIRECT
import io.github.notstirred.dasm.plugin.dasmSrcTypeHierarchy
import io.github.notstirred.dasm.plugin.splatMap
import org.jetbrains.coverage.org.objectweb.asm.Type

object MethodReference : PsiReferenceProvider() {
    val METHOD_REDIRECT_PATTERN: ElementPattern<PsiLiteral> = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(METHOD_REDIRECT)

    val METHOD_REFERENCE_REGEX = Regex("(?<name>\\S+)(?<desc>\\(\\S*\\)\\S+)")

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        return arrayOf(Reference(element as PsiLiteral))
    }

    open class Reference(element: PsiLiteral) : PsiReferenceBase<PsiLiteral>(element, false) {
        open fun sourceClass(): Iterable<PsiClass> {
            return element.findContainingClass()?.dasmSrcTypeHierarchy ?: emptyList()
        }

        open fun methods(): Array<out PsiMethod> {
            return sourceClass()
                .splatMap { it.constructors + it.methods }
                .toTypedArray()
        }

        override fun resolve(): PsiElement? {
            val text = element.text.substring(1, element.text.length - 1)

            val match = METHOD_REFERENCE_REGEX.find(text) ?: return null
            val methodType = Type.getMethodType(match.groups["desc"]!!.value)
            val methodName = match.groups["name"]!!.value

            return methods().filter {
                return@filter if (methodName == "<init>") {
                    it.isConstructor
                } else {
                    it.name == methodName
                }
            }.firstOrNull { method ->
                if (method.parameterList.parametersCount != methodType.argumentTypes.size) {
                    return@firstOrNull false
                }
                if (!method.parameterList.parameters.zip(methodType.argumentTypes)
                        .all { (psiParam, paramToMatch) -> psiParam.typeElement!!.type.descriptor == paramToMatch.descriptor }
                ) {
                    return@firstOrNull false
                }

                if (method.isConstructor) {
                    if (methodType.returnType.descriptor != "V") {
                        return@firstOrNull false
                    }
                } else if (method.returnTypeElement!!.type.descriptor != methodType.returnType.descriptor) {
                    return@firstOrNull false
                }

                return@firstOrNull true
            }
        }

        override fun getVariants(): Array<out Any?> {
            val methods = methods()
            return methods.map {
                val name = if (it.isConstructor) "<init>" else it.name
                JavaLookupElementBuilder.forMethod(it, PsiSubstitutor.EMPTY)
                    .withBaseLookupString(
                        StringBuilder()
                            .append(name)
                            .append(it.descriptor)
                            .toString()
                    )
            }.toTypedArray()
        }
    }
}
