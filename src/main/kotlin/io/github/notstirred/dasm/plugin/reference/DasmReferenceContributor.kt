package io.github.notstirred.dasm.plugin.reference

import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class DasmReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(RefReference.ELEMENT_PATTERN, RefReference)

        registrar.registerReferenceProvider(MethodReference.METHOD_REDIRECT_PATTERN, MethodReference)
        registrar.registerReferenceProvider(MethodReference.ADD_METHOD_TO_SETS_PATTERN, MethodReference)
        registrar.registerReferenceProvider(MethodReference.TRANSFORM_METHOD_PATTERN, MethodReference)
        registrar.registerReferenceProvider(MethodReference.TRANSFORM_FROM_METHOD_PATTERN, MethodReference)

        registrar.registerReferenceProvider(ConstructorReference.PATTERN, ConstructorReference)

        registrar.registerReferenceProvider(FieldReference.FIELD_PATTERN, FieldReference)
        registrar.registerReferenceProvider(FieldReference.ADD_FIELD_TO_SETS_PATTERN, FieldReference)
        registrar.registerReferenceProvider(FieldReference.FIELD_TO_METHOD_PATTERN, FieldReference)
        registrar.registerReferenceProvider(FieldReference.ADD_FIELD_TO_METHOD_TO_SETS_PATTERN, FieldReference)
    }
}