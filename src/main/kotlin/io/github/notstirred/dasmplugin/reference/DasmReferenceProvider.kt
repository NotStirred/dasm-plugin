package io.github.notstirred.dasmplugin.reference

import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.*
import com.intellij.util.containers.map2Array
import com.intellij.util.containers.stream
import io.github.notstirred.dasmplugin.DasmLanguage

object DasmReferenceProvider : ClassReferenceProvider() {
    override fun typeIsPrimitive(name: String): Boolean {
        return DasmLanguage.PRIMITIVES.contains(name)
    }

    override fun resolveClass(element: PsiElement, name: String): Array<ResolveResult> {
        val facade = JavaPsiFacade.getInstance(element.project)
        val name = name.replace('$', '.')
        val classes = facade.findClasses(name, element.resolveScope)
        if (classes.isNotEmpty()) {
            return classes.map2Array { PsiElementResolveResult(it) }
        }

        val imports = resolveTypeForName(element, name, facade)
        if (imports.isNotEmpty()) {
            return imports.map2Array { PsiElementResolveResult(it) }
        }

        return ResolveResult.EMPTY_ARRAY
    }

    override fun typeVariants(element: PsiElement): Array<Any> {
        val variants = ArrayList<Any>()

        val facade = JavaPsiFacade.getInstance(element.project)
        importsInFile(element.containingFile).stream().map {
            (it as? JsonStringLiteral)?.value?.let { it1 -> facade.findClass(it1, element.resolveScope) }
        }.filter { it != null }.forEach { variants.add(it!!) }
        variants.addAll(DasmLanguage.PRIMITIVES)
        return variants.toArray()
    }

    override fun resolveMethod(owner: PsiClass, element: PsiElement, name: String, parameterTypeNames: List<String>): Array<ResolveResult> {
        val methods = owner.findMethodsByName(name, false)
        for (method in methods) {
            val parameters = method.parameters
            if (parameters.size == parameterTypeNames.size) {
                var isRightMethod = true
                paramLoop@ for ((i, parameter) in parameters.withIndex()) {

                    val paramType = parameter.type
                    if (paramType is PsiType) {
                        if (typeIsPrimitive(paramType.canonicalText)) {
                            // primitive
                            if (parameterTypeNames[i] == paramType.canonicalText) {
                                continue@paramLoop
                            }
                        } else {
                            val resolvedType = resolveImport(element, parameterTypeNames[i].replace('$', '.'))
                            // normal type
                            if (resolvedType == trimGenerics(paramType.canonicalText)) {
                                continue@paramLoop
                            }

                            // type arg
                            if (resolvedType != null) {
                                val clazz = JavaPsiFacade.getInstance(element.project)
                                    .findClass(resolvedType, element.resolveScope)

                                if (clazz != null) {
                                    for (typeParameter in method.typeParameters) {
                                        val bounds = typeParameter.bounds
                                        if (bounds.isNotEmpty()) { // bounds are known
                                            if (resolvedType == (bounds[0] as PsiType).canonicalText) { //only the first type bound is considered in the bytecode for some reason...
                                                continue@paramLoop // param type matches, skip to next param
                                            }
                                        } else { // bounds are Object
                                            if (resolvedType == "java.lang.Object") {
                                                continue@paramLoop // param type matches, skip to next param
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    isRightMethod = false
                    break
                }
                if (isRightMethod) {
                    return arrayOf(PsiElementResolveResult(method))
                }
            }

        }
        return ResolveResult.EMPTY_ARRAY
    }

    override fun methodVariants(element: PsiElement, owner: PsiClass): Array<Any> {
        val methods = owner.allMethods
        val methodStrings = ArrayList<String>()
        methods.forEach { method ->
            val sb = StringBuilder(method.name).append('(')
            method.parameters.mapNotNull { param ->
                if (param is PsiParameter) {
                    var paramTypeName = typeName(param.type)

                    method.typeParameters.forEach { typeParameter ->
                        if (typeParameter.name == paramTypeName) {
                            return@mapNotNull if (typeParameter.bounds.isNotEmpty()) { // bounds are known
                                typeName(typeParameter.bounds[0] as PsiType) //only the first type bound is considered in the bytecode for some reason...
                            } else { // bounds are Object
                                "Object"
                            }
                        }
                    }
                    paramTypeName
                } else {
                    "__FIXME__"
                }
            }.joinTo(sb, ", ").append(")")
            methodStrings.add(sb.toString())
        }

        return methodStrings.toArray()
    }

    private fun typeName(type: PsiType): String {
        val name = trimGenerics(type.canonicalText)
        val indexOfFirstUpperCaseChar = "[A-Z]".toRegex().find(name)?.range?.first ?: 0
        return name.substring(indexOfFirstUpperCaseChar).replace('.', '$')
    }

    override fun resolveField(owner: PsiClass, element: PsiElement, name: String, typeName: String): Array<ResolveResult> {
        val field = owner.findFieldByName(name, false)
        if (field != null) {
            if (typeIsPrimitive(typeName)) {
                if (field.type.canonicalText == typeName) {
                    return arrayOf(PsiElementResolveResult(field))
                }
            } else {
                val resolvedType = resolveImport(element, typeName.replace('$', '.'))
                if (resolvedType != null && resolvedType == trimGenerics(field.type.canonicalText)) {
                    return arrayOf(PsiElementResolveResult(field))
                }
            }
        }
        return ResolveResult.EMPTY_ARRAY
    }

    override fun fieldVariants(element: PsiElement, owner: PsiClass): Array<Any> {
        val fieldStrings = ArrayList<String>()
        owner.allFields.forEach {
            fieldStrings.add(it.name)
        }
        return fieldStrings.toArray()
    }

    private fun trimGenerics(resolvedClassName: String) : String {
        val indexOfGeneric = resolvedClassName.indexOf('<')
        if (indexOfGeneric == -1) {
            return resolvedClassName
        }
        return resolvedClassName.substring(0, indexOfGeneric)
    }

    private fun resolveImport(
        element: PsiElement,
        name: String,
    ): String? {
        val imports = importsInFile(element.containingFile)
        for (import in imports) {
            if (import is JsonStringLiteral) {
                val range = ElementManipulators.getManipulator(import).getRangeInElement(import)
                var importText = range.substring(import.text)
                val lastDot = importText.lastIndexOf('.')
                importText = importText.replace('$', '.')
                if (importText.substring(lastDot + 1) == name) {
                    return importText
                }
            }
        }
        return null
    }

    private fun resolveTypeForName(
        element: PsiElement,
        name: String,
        facade: JavaPsiFacade
    ): Array<PsiClass> {
        val imports = importsInFile(element.containingFile)
        for (import in imports) {
            if (import is JsonStringLiteral) {
                val range = ElementManipulators.getManipulator(import).getRangeInElement(import)
                var importText = range.substring(import.text)
                val lastDot = importText.lastIndexOf('.')
                importText = importText.replace('$', '.')
                if (importText.substring(lastDot + 1) == name) {

                    val classes = facade.findClasses(importText, element.resolveScope)
                    if (classes.isNotEmpty()) {
                        return classes
                    }
                }
            }
        }
        return arrayOf()
    }

    private fun importsInFile(file: PsiFile) : Array<PsiElement> {
        return (file.firstChild as? JsonObject)?.findProperty("imports")?.value?.children ?: arrayOf()
    }
}