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
        private val METHOD_REDIRECT_REGEX = "\"((?:\\w|\\\$)+)\\s*\\|\\s*(\\S+)\\s+((?:\\w|\\\$)+)\\s*\\((.*)\\)\"".toRegex()
        private val TARGET_METHOD_REGEX = "\"\\s*(\\S+)\\s+((?:\\w|\\\$)+)\\s*\\((.*)\\)\"".toRegex()
    }

    val description
        get() = "class '%s'"

    abstract fun typeIsPrimitive(name: String): Boolean

    abstract fun resolveClass(element: PsiElement, name: String): Array<ResolveResult>

    abstract fun typeVariants(element: PsiElement): Array<Any>

    abstract fun resolveMethod(owner: PsiClass, element: PsiElement, name: String, parameterTypeNames: List<String>): Array<ResolveResult>

    abstract fun resolveField(owner: PsiClass, element: PsiElement, name: String, typeName: String): Array<ResolveResult>

    abstract fun fieldVariants(element: PsiElement, owner: PsiClass): Array<Any>

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

        return arrayOf(TypeReference(element, TextRange(range.startOffset + lastDot, range.endOffset)))
    }

    private fun targetMethodReferences(element: PsiElement): Array<PsiReference> {
        val parent = element.parent
        if (parent is JsonProperty) {
            if (parent.children[0] == element) { // is key
                val references = ArrayList<PsiReference>()
                val primitives = ArrayList<TextRange>()

                val range = ElementManipulators.getManipulator(element).getRangeInElement(element)
                val methodRedirectText = range.substring(element.text)

                // Method return type part
                val methodSeparatorRange = "\\s+".toRegex().find(methodRedirectText)?.range
                val methodSeparatorStart = methodSeparatorRange?.first ?: methodRedirectText.length
                val methodSeparatorEnd = methodSeparatorRange?.last?.plus(1) ?: methodRedirectText.length

                val typeRange = TextRange(range.startOffset, range.startOffset + methodSeparatorStart)
                if (!typeIsPrimitive(typeRange.substring(element.text))) {
                    references.add(TypeReference(element, typeRange))
                } else {
                    primitives.add(typeRange)
                }

                // METHOD OWNER
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

                // PARAMETERS
                val indexOfParametersStart = element.text.indexOf('(')
                if (indexOfParametersStart > 0) {
                    val parameters = getAndAddParameterReferences(indexOfParametersStart, element, references, primitives)

                    // METHOD NAME
                    if (owner != null) {
                        for (ownerReference in owner.references) {
                            if (ownerReference is TypeReference) {
                                addMethodReference(element, range.startOffset + methodSeparatorEnd, references, ownerReference, parameters.map { it.value }.toList())
                                break
                            }
                        }
                    }
                } else {
                    // METHOD NAME
                    // couldn't find a `(` so assume the method name span is to the end
                    if (owner != null) {
                        for (ownerReference in owner.references) {
                            if (ownerReference is TypeReference) {
                                addMethodReferenceOrAtEnd(element, range, methodSeparatorEnd, references, ownerReference)
                                break
                            }
                        }
                    }

                }

                // TODO: WHOLE ERROR

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
                val references = ArrayList<PsiReference>()
                val primitives = ArrayList<TextRange>()

                val range = ElementManipulators.getManipulator(element).getRangeInElement(element)
                val methodRedirectText = range.substring(element.text)

                // Owner type part
                val ownerSeparatorRange = "\\s*\\|\\s*".toRegex().find(methodRedirectText)?.range
                val ownerSeparatorStartIdx = ownerSeparatorRange?.first ?: methodRedirectText.length
                val ownerSeparatorEndIdx = ownerSeparatorRange?.last?.plus(1) ?: methodRedirectText.length

                val ownerRange = TextRange(range.startOffset, range.startOffset + ownerSeparatorStartIdx)
                val ownerReference = TypeReference(element, ownerRange)
                references.add(ownerReference)

                // Method return type part
                val methodSeparatorRange = "\\s".toRegex().find(methodRedirectText, ownerSeparatorEndIdx)?.range
                val methodSeparatorStart = methodSeparatorRange?.first ?: methodRedirectText.length
                val methodSeparatorEnd = methodSeparatorRange?.last?.plus(1) ?: methodRedirectText.length

                val typeRange = TextRange(range.startOffset + ownerSeparatorEndIdx, range.startOffset + methodSeparatorStart)
                if (!typeIsPrimitive(typeRange.substring(element.text))) {
                    references.add(TypeReference(element, typeRange))
                } else {
                    primitives.add(typeRange)
                }

                // PARAMETERS & METHOD
                val parentValue = parent.children[1]
                val owner = if (parentValue is JsonObject && parentValue.findProperty("mappingsOwner") != null) {
                    // use mappings owner if present
                    val mappingsOwnerProperty = parentValue.findProperty("mappingsOwner")
                    if (mappingsOwnerProperty?.value is JsonStringLiteral) {
                        (mappingsOwnerProperty.value as JsonStringLiteral).references
                    } else {
                        null
                    }
                } else { // otherwise use target class as owner
                    arrayOf(ownerReference)
                }

                val indexOfParametersStart = element.text.indexOf('(')
                if (indexOfParametersStart > 0) {
                    val parameters = getAndAddParameterReferences(indexOfParametersStart, element, references, primitives)

                    if (owner != null) {
                        for (mappingsOwnerReference in owner) {
                            if (mappingsOwnerReference is TypeReference) {
                                addMethodReference(element, range.startOffset + methodSeparatorEnd, references, mappingsOwnerReference, parameters.map { it.value }.toList())
                                break
                            }
                        }
                    }
                } else { // couldn't find a `(` so assume the method name span is to the end
                    if (owner != null) {
                        for (mappingsOwnerReference in owner) {
                            if (mappingsOwnerReference is TypeReference) {
                                addMethodReferenceOrAtEnd(element, range, methodSeparatorEnd, references, mappingsOwnerReference)
                                break
                            }
                        }
                    }
                }

                // TODO: WHOLE ERROR

                element.putUserData(PRIMITIVES_KEY, primitives)
                return references.toArray(arrayOf())
            }
        }
        return arrayOf()
    }

    /**
     * Adds a method reference for the method name text, or at the end of the text if there is no match (for autocomplete)
     */
    private fun addMethodReferenceOrAtEnd(
        element: PsiElement,
        range: TextRange,
        methodSeparatorEnd: Int,
        references: ArrayList<PsiReference>,
        ownerReference: TypeReference
    ) {
        val methodName = "((?:\\w|\\\$)+|\$)+".toRegex().find(element.text, range.startOffset + methodSeparatorEnd)
        if (methodName != null) {
            if (methodName.range.first >= element.text.length - 1) {
                // ^ if the range starts at the end of the text, add a reference to just the end characters (for autocomplete)
                if (range.startOffset + methodSeparatorEnd <= element.text.length - 1) {
                    references.add(
                        MethodReference(
                            ownerReference,
                            element,
                            TextRange(range.startOffset + methodSeparatorEnd, element.text.length - 1),
                            ArrayList()
                        )
                    )
                }
            } else {
                // normal method reference
                references.add(
                    MethodReference(
                        ownerReference,
                        element,
                        TextRange(methodName.range.first, methodName.range.last + 1),
                        ArrayList()
                    )
                )
            }
        }
    }

    private fun addMethodReference(
        element: PsiElement,
        startIdx: Int,
        references: ArrayList<PsiReference>,
        ownerReference: TypeReference,
        parameters: List<String>
    ) {
        val methodName = "((?:\\w|\\\$)+|\$)".toRegex().find(element.text, startIdx)
        if (methodName != null) {
            references.add(
                MethodReference(
                    ownerReference,
                    element,
                    TextRange(methodName.range.first, methodName.range.last + 1),
                    parameters
                )
            )
        }
    }

    private fun getAndAddParameterReferences(
        indexOfParametersStart: Int,
        element: PsiElement,
        references: ArrayList<PsiReference>,
        primitives: ArrayList<TextRange>
    ): Sequence<MatchResult> {
        val parametersRange =
            TextRange(indexOfParametersStart + 1, maxOf(element.text.indexOf(')'), element.text.length))
        val parametersText = parametersRange.substring(element.text)
        val parameters = "(?:\\w|\\\$)+".toRegex().findAll(parametersText)
        for (parameter in parameters.iterator()) {
            val intRange = parameter.range.withOffset(parametersRange.startOffset)
            val range = TextRange(intRange.first, intRange.last + 1) // inclusive -> exclusive
            if (!typeIsPrimitive(range.substring(element.text))) {
                references.add(TypeReference(element, range))
            } else {
                primitives.add(range)
            }
        }
        return parameters
    }

    private fun fieldRedirectReferences(element: PsiElement): Array<PsiReference> {
        val parent = element.parent
        if (parent is JsonProperty) {
            if (parent.children[0] == element) { // is key
                val references = ArrayList<PsiReference>()
                val primitives = ArrayList<TextRange>()

                val range = ElementManipulators.getManipulator(element).getRangeInElement(element)
                val fieldRedirectText = range.substring(element.text)

                // Owner type part
                val ownerSeparatorRange = "\\s*\\|\\s*".toRegex().find(fieldRedirectText)?.range
                val ownerSeparatorStartIdx = ownerSeparatorRange?.first ?: fieldRedirectText.length
                val ownerSeparatorEndIdx = ownerSeparatorRange?.last?.plus(1) ?: fieldRedirectText.length

                val ownerRange = TextRange(range.startOffset, range.startOffset + ownerSeparatorStartIdx)
                val ownerReference = TypeReference(element, ownerRange)
                references.add(ownerReference)

                // Field type part
                val fieldSeparatorRange = "\\s+".toRegex().find(fieldRedirectText, ownerSeparatorEndIdx)?.range
                val fieldSeparatorStart = fieldSeparatorRange?.first ?: fieldRedirectText.length
                val fieldSeparatorEnd = fieldSeparatorRange?.last?.plus(1) ?: fieldRedirectText.length

                val typeRange = TextRange(range.startOffset + ownerSeparatorEndIdx, range.startOffset + fieldSeparatorStart)
                if (!typeIsPrimitive(typeRange.substring(element.text))) {
                    references.add(TypeReference(element, typeRange))
                } else {
                    primitives.add(typeRange)
                }

                val nameRange = TextRange(range.startOffset + fieldSeparatorEnd, range.startOffset + fieldRedirectText.length)
                references.add(FieldReference(ownerReference, element, nameRange, typeRange))

                // TODO: WHOLE ERROR

                element.putUserData(PRIMITIVES_KEY, primitives)
                return references.toArray(arrayOf())
            }
        }
        return arrayOf()
    }

    private inner class TypeReference(element: PsiElement, range: TextRange) :
        PsiReferenceBase.Poly<PsiElement>(element, range, false), InspectionReference {

        override val description: String
            get() = this@ClassReferenceProvider.description
        override val unresolved: Boolean
            get() = multiResolve(false).isEmpty()
        override val referenceType: ReferenceType
            get() = ReferenceType.CLASS

        private val qualifiedRange = range
        private val qualifiedName: String
            get() = qualifiedRange.substring(element.text)

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            return this@ClassReferenceProvider.resolveClass(this.element, this.qualifiedName)
        }

        override fun getVariants(): Array<Any> {
            return this@ClassReferenceProvider.typeVariants(this.element)
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

        override fun getVariants(): Array<Any> {
            val ownerClass = this.owner.resolve() as? PsiClass?
            if (ownerClass != null) {
                return this@ClassReferenceProvider.fieldVariants(this.element, ownerClass)
            }
            return arrayOf()
        }
    }
}