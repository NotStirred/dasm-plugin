package io.github.notstirred.dasm.plugin

import io.github.notstirred.dasm.api.annotations.Dasm
import io.github.notstirred.dasm.api.annotations.redirect.redirects.*
import io.github.notstirred.dasm.api.annotations.redirect.sets.InterOwnerContainer
import io.github.notstirred.dasm.api.annotations.redirect.sets.IntraOwnerContainer
import io.github.notstirred.dasm.api.annotations.selector.Ref
import io.github.notstirred.dasm.api.annotations.transform.AddUnusedParam
import io.github.notstirred.dasm.api.annotations.transform.TransformFromClass
import io.github.notstirred.dasm.api.annotations.transform.TransformFromMethod
import io.github.notstirred.dasm.api.annotations.transform.TransformMethod

object DasmConstants {
    val DASM = Dasm::class.qualifiedName!!

    val REF = Ref::class.qualifiedName!!

    val INTER_OWNER_CONTAINER = InterOwnerContainer::class.qualifiedName!!
    val INTRA_OWNER_CONTAINER = IntraOwnerContainer::class.qualifiedName!!
    val TYPE_REDIRECT = TypeRedirect::class.qualifiedName!!
    val METHOD_REDIRECT = MethodRedirect::class.qualifiedName!!
    val FIELD_REDIRECT = FieldRedirect::class.qualifiedName!!
    val FIELD_TO_METHOD_REDIRECT = FieldToMethodRedirect::class.qualifiedName!!
    val CONSTRUCTOR_TO_FACTORY_REDIRECT = ConstructorToFactoryRedirect::class.qualifiedName!!

    val ADD_METHOD_TO_SETS = AddMethodToSets::class.qualifiedName!!
    val ADD_FIELD_TO_SETS = AddFieldToSets::class.qualifiedName!!
    val ADD_FIELD_TO_METHOD_TO_SETS = AddFieldToMethodToSets::class.qualifiedName!!
    val ADD_TRANSFORM_TO_SETS = AddTransformToSets::class.qualifiedName!!

    val TRANSFORM_METHOD = TransformMethod::class.qualifiedName!!
    val TRANSFORM_FROM_METHOD = TransformFromMethod::class.qualifiedName!!
    val TRANSFORM_FROM_CLASS = TransformFromClass::class.qualifiedName!!

    val ADD_UNUSED_PARAM = AddUnusedParam::class.qualifiedName!!
}