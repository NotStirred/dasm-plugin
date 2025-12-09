package io.github.notstirred.dasm.plugin

import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.resolveClass
import com.demonwav.mcdev.util.resolveType
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import io.github.notstirred.dasm.plugin.DasmConstants.DASM
import io.github.notstirred.dasm.plugin.DasmConstants.INTER_OWNER_CONTAINER
import io.github.notstirred.dasm.plugin.DasmConstants.INTRA_OWNER_CONTAINER
import io.github.notstirred.dasm.plugin.DasmConstants.REF
import io.github.notstirred.dasm.plugin.DasmConstants.TYPE_REDIRECT

val PsiClass.dasmAnnotation
    get() = modifierList?.findAnnotation(DASM)

data class ContainerTypes(val from: PsiClass, val to: PsiClass) {
    companion object {
        fun create(annotation: PsiAnnotation): ContainerTypes? {
            return annotation.qualifiedName?.let {
                when (it) {
                    TYPE_REDIRECT, INTER_OWNER_CONTAINER -> {
                        val from =
                            parseClassRef(annotation.findDeclaredAttributeValue("from") as PsiAnnotation, annotation.project)
                        val to =
                            parseClassRef(annotation.findDeclaredAttributeValue("to") as PsiAnnotation, annotation.project)
                        if (from != null && to != null) {
                            ContainerTypes(from, to)
                        } else {
                            null
                        }
                    }

                    INTRA_OWNER_CONTAINER -> {
                        val value =
                            parseClassRef(annotation.findDeclaredAttributeValue(null) as PsiAnnotation, annotation.project)
                        value?.let {
                            ContainerTypes(value, value)
                        }
                    }

                    else -> {
                        null
                    }
                }
            }
        }
    }
}

val PsiClass.dasmSrcTypeHierarchy: Iterable<PsiClass>?
    get() {
        if (this.dasmAnnotation != null) {
            this.dasmTarget?.let { return listOf(it) }
        } else { // it must be a container
            return this.containerHierarchy()
        }
        return null
    }

fun PsiClass.containerHierarchy(): List<PsiClass> {
    var clazz: PsiClass? = this
    val hierarchy = mutableListOf<ContainerTypes>()
    while (clazz != null && clazz.containerTypes != null) {
        hierarchy.add(clazz.containerTypes!!)
        clazz = clazz.superClass
    }
    return hierarchy.map { it.from }
}

val PsiClass.containerTypes: ContainerTypes?
    get() {
        return this.annotations.find { isContainerType(it.qualifiedName) }?.let { annotation ->
            ContainerTypes.create(annotation)
        }
    }

fun isContainerType(qualifiedName: String?): Boolean {
    return qualifiedName.equals(TYPE_REDIRECT)
            || qualifiedName.equals(INTER_OWNER_CONTAINER)
            || qualifiedName.equals(INTRA_OWNER_CONTAINER)
}

fun parseClassRef(refAnnotation: PsiAnnotation, project: Project): PsiClass? {
    refAnnotation.findDeclaredAttributeValue("value")?.let {
        return it.resolveClass()
    }
    refAnnotation.findDeclaredAttributeValue("string")?.let {
        val facade = JavaPsiFacade.getInstance(project)
        return facade.findClass(it.text.substring(1, it.textLength - 1), refAnnotation.resolveScope)
    }
    return null
}

fun parseRef(refAnnotation: PsiAnnotation, project: Project): PsiType? {
    refAnnotation.findDeclaredAttributeValue("value")?.let {
        return it.resolveType()
    }
    refAnnotation.findDeclaredAttributeValue("string")?.let {
        val facade = JavaPsiFacade.getInstance(project).parserFacade
        return facade.createTypeFromText(it.text.substring(1, it.textLength - 1), refAnnotation)
    }
    return null
}

val PsiClass.dasmTarget: PsiClass?
    get() {
        val dasmAnnotation = this@dasmTarget.dasmAnnotation ?: return null

        // Get @Dasm target
        val refTarget = dasmAnnotation.findDeclaredAttributeValue("target")?.findAnnotations()
            ?.first { it.qualifiedName.equals(REF) }

        return refTarget?.let { parseClassRef(it, project) } ?: this
    }

inline fun <T, R> Iterable<T>.splatMap(transform: (T) -> Array<R>): List<R> {
    return this.map(transform).flatMap { it.toList() }
}
