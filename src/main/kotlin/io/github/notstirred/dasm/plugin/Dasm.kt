package io.github.notstirred.dasm.plugin

import com.demonwav.mcdev.platform.mixin.MixinModule
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.resolveClass
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import io.github.notstirred.dasm.annotation.parse.DasmImpl
import io.github.notstirred.dasm.api.annotations.Dasm
import io.github.notstirred.dasm.plugin.DasmConstants.DASM
import io.github.notstirred.dasm.plugin.DasmConstants.INTER_OWNER_CONTAINER
import io.github.notstirred.dasm.plugin.DasmConstants.INTRA_OWNER_CONTAINER
import io.github.notstirred.dasm.plugin.DasmConstants.REF
import io.github.notstirred.dasm.plugin.DasmConstants.TYPE_REDIRECT
import io.github.notstirred.dasm.plugin.config.DasmConfigFileType
import io.github.notstirred.dasm.util.TypeUtil
import org.objectweb.asm.tree.AnnotationNode

val PsiClass.dasmAnnotation
    get() = modifierList?.findAnnotation(DASM)

data class ContainerTypes(val from: PsiClass, val to: PsiClass) {
    companion object {
        fun create(annotation: PsiAnnotation): ContainerTypes? {
            return annotation.qualifiedName?.let {
                when (it) {
                    TYPE_REDIRECT, INTER_OWNER_CONTAINER -> {
                        val from = parseRef(annotation.findDeclaredAttributeValue("from") as PsiAnnotation, annotation.project)
                        val to = parseRef(annotation.findDeclaredAttributeValue("to") as PsiAnnotation, annotation.project)
                        if (from != null && to != null) {
                            ContainerTypes(from, to)
                        } else {
                            null
                        }
                    }
                    INTRA_OWNER_CONTAINER -> {
                        val value = parseRef(annotation.findDeclaredAttributeValue(null) as PsiAnnotation, annotation.project)
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

val PsiClass.dasmSrcType: PsiClass?
    get() {
        this.dasmTarget?.let { return it }
        this.containerTypes?.from.let { return it }
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

fun parseRef(refAnnotation: PsiAnnotation, project: Project): PsiClass? {
    refAnnotation.findDeclaredAttributeValue("value")?.let {
        return it.resolveClass()
    }
    refAnnotation.findDeclaredAttributeValue("string")?.let {
        return JavaPsiFacade.getInstance(project).findClass(it.text.substring(1, it.textLength - 1), refAnnotation.resolveScope)
    }

    return null;
}

val PsiClass.dasmTarget: PsiClass?
    get() {
        val dasmAnnotation = this@dasmTarget.dasmAnnotation ?: return null

        // Get @Dasm target
        val refTarget = dasmAnnotation.findDeclaredAttributeValue("target")?.findAnnotations()
            ?.first { it.qualifiedName.equals(REF) }

        return refTarget?.let { parseRef(it, project) }
    }

class Dasm {
    companion object {
        private val dasmFileTypes = listOf(DasmConfigFileType.Json, DasmConfigFileType.Json5)

        fun getAllDasmClasses(project: Project, scope: GlobalSearchScope): Collection<PsiClass> {
            // TODO: replace this with parsing dasm.json once it exists
            val dasmTargetMap = HashMap<PsiClass, MutableList<PsiClass>>();

            val mixinClasses = MixinModule.getAllMixinClasses(project, scope)

            mixinClasses.forEach { mixinClass ->
                mixinClass.getAnnotation(Dasm::class.qualifiedName!!)?.let {
                    val ann = mixinClass.getAnnotation(Dasm::class.qualifiedName!!)!!.qualifiedName
                    val annotationNode = AnnotationNode(TypeUtil.typeNameToDescriptor(ann))
                    val target = DasmImpl.parse(annotationNode).target()
                    JavaPsiFacade.getInstance(project).findClasses(target.get().className, scope).toList().forEach { dasmTarget ->
                        dasmTargetMap.computeIfAbsent(dasmTarget) { ArrayList() }.add(mixinClass)
                    }
                }
            }

            return null!!
        }
    }
}
