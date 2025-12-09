package io.github.notstirred.dasm.plugin.reference

import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.source.PsiClassImpl
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

        open fun members(): Array<out PsiMember> {
            return sourceClass()
                .splatMap { arrayOf(*it.constructors, *it.methods, *it.initializers) }
                .toTypedArray()
        }

        override fun resolve(): PsiElement? {
            val text = element.text.substring(1, element.text.length - 1)

            if (text == "<clinit>") {
                try {
                    return ((sourceClass().first() as ClsClassImpl).mirror as PsiClassImpl).initializers.getOrNull(0) // FIXME: SURELY there is a better way to get initializers than this
                } catch (_: Exception) {
                }
            }

            val match = METHOD_REFERENCE_REGEX.find(text) ?: return null
            val methodType = Type.getMethodType(match.groups["desc"]!!.value)
            val methodName = match.groups["name"]!!.value

            return members().filter {
                when (it) {
                    is PsiClassInitializer -> methodName == "<clinit>"
                    is PsiMethod -> when (methodName) {
                        "<init>" -> it.isConstructor
                        else -> it.name == methodName
                    }

                    else -> throw IllegalArgumentException()
                }
            }.firstOrNull { method ->
                when (method) {
                    is PsiClassInitializer -> true
                    is PsiMethod -> {
                        try {
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
                        } catch (_: IllegalArgumentException) {
                            return@firstOrNull false
                        }
                    }

                    else -> throw IllegalArgumentException()
                }
            }
        }

        override fun getVariants(): Array<out Any?> {
            return members().map {
                when (it) {
                    is PsiClassInitializer -> {
                        LookupElementBuilder.create("<clinit>")
                            .withIcon(com.intellij.util.PlatformIcons.METHOD_ICON)
                            .withBaseLookupString("<clinit>()")
                    }

                    is PsiMethod -> {
                        val name = if (it.isConstructor) "<init>" else it.name
                        JavaLookupElementBuilder.forMethod(it, PsiSubstitutor.EMPTY)
                            .withPresentableText(name)
                            .withBaseLookupString(
                                StringBuilder()
                                    .append(name)
                                    .append(it.descriptor)
                                    .toString()
                            ).withLookupString(name + it.descriptor)
                    }
                }
            }.toTypedArray()
        }
    }
}
