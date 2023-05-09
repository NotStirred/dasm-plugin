package io.github.notstirred.dasmplugin.reference

import ai.grazie.nlp.utils.withOffset
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import io.github.notstirred.dasmplugin.DasmLanguage

abstract class ClassReferenceProvider : PsiReferenceProvider() {
    companion object {
        val PRIMITIVES_KEY = Key<List<TextRange>>("primitives")
        private val FIELD_REDIRECT_REGEX = "\"(\\S+)\\s*\\|\\s*(\\S+)\\s+(\\S+)\"".toRegex()
        private val METHOD_REDIRECT_REGEX = "\"((?:\\w|\\\$)+)\\s*\\|\\s*(\\S+)\\s+((?:\\w|\\\$)+)\\s*\\((.*)\\)\"".toRegex()
        private val TARGET_METHOD_REGEX = "\"\\s*(\\S+)\\s+((?:\\w|\\\$)+)\\s*\\((.*)\\)\"".toRegex()
    }

    val description
        get() = "class '%s'"

    abstract fun typeIsPrimitive(name: String): Boolean

    abstract fun resolveClass(element: PsiElement, name: String): Array<ResolveResult>

    abstract fun classVariants(element: PsiElement): Array<Any>

    abstract fun resolveMethod(owner: PsiClass, element: PsiElement, name: String, parameterTypeNames: List<String>): Array<ResolveResult>

    abstract fun resolveField(owner: PsiClass, element: PsiElement, name: String, typeName: String): Array<ResolveResult>

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val range = ElementManipulators.getManipulator(element).getRangeInElement(element)

        if (DasmLanguage.KEYWORDS.contains(range.substring(element.text))) {
            return arrayOf()
        }

        val parent = element.parent
        if (parent is JsonProperty) {
            if (parent.name == "newName") {
                return arrayOf()
            }
        }

        // this -> this.property -> parent object -> parent property
        val parentPP = element.parent?.parent?.parent
        if (parentPP is JsonProperty) {
            when (parentPP.name) {
                "fieldRedirects" -> {
                    return fieldRedirectReferences(element)
                }
                "methodRedirects" -> {
                    return methodRedirectReferences(element)
                }
                "targetMethods" -> {
                    return targetMethodReferences(element)
                }
                "sets" -> {
                    return arrayOf()
                }
            }
        }
        val parentP = element.parent?.parent
        if (parentP is JsonProperty) {
            val ppName = parentP.name
            if (ppName == "defaultSets" ||
                ppName == "useSets") {
                return arrayOf()
            }
        }


        val lastDot = maxOf(0, element.text.lastIndexOf('.'))

        return arrayOf(TypeReference(element, TextRange(range.startOffset + lastDot, range.endOffset), range.startOffset))
    }

    private fun targetMethodReferences(element: PsiElement): Array<PsiReference> {
        val parent = element.parent
        if (parent is JsonProperty) {
            if (parent.children[0] == element) { // is key
                val find = TARGET_METHOD_REGEX.find(element.text)
                val references = ArrayList<PsiReference>()
                val primitives = ArrayList<TextRange>()

                if (find != null && find.groups.size == 4) {
                    val returnTypeRange = TextRange(find.groups[1]!!.range.first, find.groups[1]!!.range.last + 1)
                    if (!typeIsPrimitive(returnTypeRange.substring(element.text))) {
                        references.add(TypeReference(element, returnTypeRange, returnTypeRange.startOffset))
                    } else {
                        primitives.add(returnTypeRange)
                    }

                    val parametersRange = TextRange(find.groups[3]!!.range.first, find.groups[3]!!.range.last + 1)
                    val parametersText = parametersRange.substring(element.text)
                    val parameters = "(?:\\w|\\\$)+".toRegex().findAll(parametersText)
                    for (parameter in parameters.iterator()) {
                        val intRange = parameter.range.withOffset(parametersRange.startOffset)
                        val range = TextRange(intRange.first, intRange.last + 1) // inclusive -> exclusive
                        if (!typeIsPrimitive(range.substring(element.text))) {
                            references.add(TypeReference(element, range, range.startOffset))
                        } else {
                            primitives.add(range)
                        }
                    }

                    // METHOD NAME
                    val parentValue = parent.children[1]
                    val owner = if (parentValue is JsonObject && parentValue.findProperty("mappingsOwner") != null) {
                        // use mappings owner if present
                        val mappingsOwnerProperty = parentValue.findProperty("mappingsOwner")
                        if (mappingsOwnerProperty?.value is JsonStringLiteral) {
                            mappingsOwnerProperty.value
                        } else {
                            null
                        }
                    } else { // otherwise use target class as owner
                        element.parent?.parent?.parent?.parent?.parent?.children?.get(0)
                    }

                    if (owner != null) {
                        for (ownerReference in owner.references) {
                            if (ownerReference is TypeReference) {
                                val methodNameRange =
                                    TextRange(find.groups[2]!!.range.first, find.groups[2]!!.range.last + 1)
                                references.add(
                                    MethodReference(
                                        ownerReference,
                                        element,
                                        methodNameRange,
                                        parameters.map { it.value }.toList()
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // TODO: WHOLE ERROR
                }

                element.putUserData(PRIMITIVES_KEY, primitives)
                return references.toArray(arrayOf())
            }
        }
        return arrayOf()
    }

    private fun methodRedirectReferences(element: PsiElement): Array<PsiReference> {
        val parent = element.parent
        if (parent is JsonProperty) {
            if (parent.children[0] == element) { // is key
                val find = METHOD_REDIRECT_REGEX.find(element.text)
                val references = ArrayList<PsiReference>()
                val primitives = ArrayList<TextRange>()

                if (find != null && find.groups.size == 5) {
                    // OWNER
                    val ownerRange = TextRange(find.groups[1]!!.range.first, find.groups[1]!!.range.last + 1)
                    val ownerReference = TypeReference(element, ownerRange, 1)
                    references.add(ownerReference)


                    // RETURN TYPE
                    val returnTypeRange = TextRange(find.groups[2]!!.range.first, find.groups[2]!!.range.last + 1)
                    if (!typeIsPrimitive(returnTypeRange.substring(element.text))) {
                        references.add(TypeReference(element, returnTypeRange, returnTypeRange.startOffset))
                    } else {
                        primitives.add(returnTypeRange)
                    }

                    // PARAMETERS
                    val parametersRange = TextRange(find.groups[4]!!.range.first, find.groups[4]!!.range.last + 1)
                    val parametersText = parametersRange.substring(element.text)
                    val parameters = "(?:\\w|\\\$)+".toRegex().findAll(parametersText)
                    for (parameter in parameters.iterator()) {
                        val intRange = parameter.range.withOffset(parametersRange.startOffset)
                        val range = TextRange(intRange.first, intRange.last + 1) // inclusive -> exclusive
                        if (!typeIsPrimitive(range.substring(element.text))) {
                            references.add(TypeReference(element, range, range.startOffset))
                        } else {
                            primitives.add(range)
                        }
                    }

                    // METHOD NAME
                    val methodNameRange = TextRange(find.groups[3]!!.range.first, find.groups[3]!!.range.last + 1)
                    references.add(MethodReference(ownerReference, element, methodNameRange, parameters.map { it.value }.toList()))

                } else {
                    // TODO: WHOLE ERROR
                }

                element.putUserData(PRIMITIVES_KEY, primitives)
                return references.toArray(arrayOf())
            }
        }
        return arrayOf()
    }

    private fun fieldRedirectReferences(element: PsiElement): Array<PsiReference> {
        val parent = element.parent
        if (parent is JsonProperty) {
            if (parent.children[0] == element) { // is key
                val find = FIELD_REDIRECT_REGEX.find(element.text)
                val references = ArrayList<PsiReference>()
                val primitives = ArrayList<TextRange>()

                if (find != null && find.groups.size == 4) {
                    val ownerRange = TextRange(find.groups[1]!!.range.first, find.groups[1]!!.range.last + 1)
                    val ownerReference = TypeReference(element, ownerRange, 1)
                    references.add(ownerReference)

                    val typeRange = TextRange(find.groups[2]!!.range.first, find.groups[2]!!.range.last + 1)
                    if (!typeIsPrimitive(typeRange.substring(element.text))) {
                        references.add(TypeReference(element, typeRange, typeRange.startOffset))
                    } else {
                        primitives.add(typeRange)
                    }

                    val nameRange = TextRange(find.groups[3]!!.range.first, find.groups[3]!!.range.last + 1)
                    references.add(FieldReference(ownerReference, element, nameRange, typeRange))
                } else {
                    // TODO: WHOLE ERROR
                }

                element.putUserData(PRIMITIVES_KEY, primitives)
                return references.toArray(arrayOf())
            }
        }
        return arrayOf()
    }

    private inner class TypeReference(element: PsiElement, range: TextRange, start: Int) :
        PsiReferenceBase.Poly<PsiElement>(element, range, false), InspectionReference {

        override val description: String
            get() = this@ClassReferenceProvider.description
        override val unresolved: Boolean
            get() = multiResolve(false).isEmpty()
        override val referenceType: ReferenceType
            get() = ReferenceType.CLASS

        private val qualifiedRange = TextRange(start, range.endOffset)
        private val qualifiedName: String
            get() = qualifiedRange.substring(element.text)

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            return this@ClassReferenceProvider.resolveClass(this.element, this.qualifiedName)
        }

        override fun getVariants(): Array<Any> {
            return this@ClassReferenceProvider.classVariants(this.element)
        }
    }

    private inner class MethodReference(private val owner: TypeReference, element: PsiElement, range: TextRange, private val parameterTypeNames: List<String>) :
        PsiReferenceBase.Poly<PsiElement>(element, range, false), InspectionReference {

        override val description: String
            get() = this@ClassReferenceProvider.description
        override val unresolved: Boolean
            get() = multiResolve(false).isEmpty()
        override val referenceType: ReferenceType
            get() = ReferenceType.METHOD

        private val qualifiedRange = range
        private val qualifiedName: String
            get() = qualifiedRange.substring(element.text)

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            val ownerClass = this.owner.resolve() as? PsiClass?
            if (ownerClass != null) {
                return this@ClassReferenceProvider.resolveMethod(
                    ownerClass,
                    this.element,
                    this.qualifiedName,
                    this.parameterTypeNames
                )
            }
            return ResolveResult.EMPTY_ARRAY
        }
    }

    private inner class FieldReference(private val owner: TypeReference, element: PsiElement, range: TextRange, private val typeRange: TextRange) :
        PsiReferenceBase.Poly<PsiElement>(element, range, false), InspectionReference {

        override val description: String
            get() = this@ClassReferenceProvider.description
        override val unresolved: Boolean
            get() = multiResolve(false).isEmpty()
        override val referenceType: ReferenceType
            get() = ReferenceType.FIELD

        private val fieldRange = range
        private val fieldName: String
            get() = fieldRange.substring(element.text)

        private val qualifiedTypeName: String
            get() = typeRange.substring(element.text)

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            val ownerClass = this.owner.resolve() as? PsiClass?
            if (ownerClass != null) {
                return this@ClassReferenceProvider.resolveField(
                    ownerClass,
                    this.element,
                    this.fieldName,
                    qualifiedTypeName
                )
            }
            return ResolveResult.EMPTY_ARRAY
        }
    }
}