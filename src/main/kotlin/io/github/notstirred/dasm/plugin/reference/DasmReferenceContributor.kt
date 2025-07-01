package io.github.notstirred.dasm.plugin.reference

import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar

class DasmReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(RefReference.ELEMENT_PATTERN, RefReference)

        registrar.registerReferenceProvider(MethodReference.PATTERN, MethodReference)
        registrar.registerReferenceProvider(ConstructorReference.PATTERN, ConstructorReference)

        registrar.registerReferenceProvider(FieldReference.FIELD_PATTERN, FieldReference)
        registrar.registerReferenceProvider(FieldReference.FIELD_TO_METHOD_PATTERN, FieldReference)
    }
}