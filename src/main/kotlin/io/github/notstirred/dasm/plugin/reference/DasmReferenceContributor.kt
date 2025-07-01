package io.github.notstirred.dasm.plugin.reference

import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class DasmReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(RefReference.ELEMENT_PATTERN, RefReference)

        registrar.registerReferenceProvider(MethodReference.METHOD_REDIRECT_PATTERN, MethodReference)
        registrar.registerReferenceProvider(ConstructorReference.PATTERN, ConstructorReference)

        registrar.registerReferenceProvider(ContainerOwningMethodReference.ADD_METHOD_TO_SETS_PATTERN, ContainerOwningMethodReference)
        registrar.registerReferenceProvider(OwnerOwningMethodReference.TRANSFORM_METHOD_PATTERN, OwnerOwningMethodReference)
        registrar.registerReferenceProvider(OwnerOwningMethodReference.TRANSFORM_FROM_METHOD_PATTERN, OwnerOwningMethodReference)

        registrar.registerReferenceProvider(FieldReference.FIELD_PATTERN, FieldReference)
        registrar.registerReferenceProvider(FieldReference.FIELD_TO_METHOD_PATTERN, FieldReference)
        registrar.registerReferenceProvider(AddFieldToSetsReference.ADD_FIELD_TO_SETS_PATTERN, AddFieldToSetsReference)
        registrar.registerReferenceProvider(AddFieldToSetsReference.ADD_FIELD_TO_METHOD_TO_SETS_PATTERN, AddFieldToSetsReference)
    }
}