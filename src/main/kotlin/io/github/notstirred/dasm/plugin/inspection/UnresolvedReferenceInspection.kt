package io.github.notstirred.dasm.plugin.inspection

import com.demonwav.mcdev.util.annotationFromNameValuePair
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameValuePair
import io.github.notstirred.dasm.plugin.DasmConstants.ADD_FIELD_TO_METHOD_TO_SETS
import io.github.notstirred.dasm.plugin.DasmConstants.ADD_FIELD_TO_SETS
import io.github.notstirred.dasm.plugin.DasmConstants.ADD_METHOD_TO_SETS
import io.github.notstirred.dasm.plugin.DasmConstants.CONSTRUCTOR_TO_FACTORY_REDIRECT
import io.github.notstirred.dasm.plugin.DasmConstants.FIELD_REDIRECT
import io.github.notstirred.dasm.plugin.DasmConstants.FIELD_TO_METHOD_REDIRECT
import io.github.notstirred.dasm.plugin.DasmConstants.METHOD_REDIRECT
import io.github.notstirred.dasm.plugin.DasmConstants.REF
import io.github.notstirred.dasm.plugin.DasmConstants.TRANSFORM_FROM_METHOD
import io.github.notstirred.dasm.plugin.DasmConstants.TRANSFORM_METHOD
import io.github.notstirred.dasm.plugin.reference.ConstructorReference
import io.github.notstirred.dasm.plugin.reference.FieldReference
import io.github.notstirred.dasm.plugin.reference.MethodReference
import io.github.notstirred.dasm.plugin.reference.RefReference

class UnresolvedReferenceInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun getStaticDescription() = "Reports unresolves references in Dasm annotations"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitNameValuePair(pair: PsiNameValuePair) {
            when (pair.annotationFromNameValuePair?.qualifiedName) {
                METHOD_REDIRECT, ADD_METHOD_TO_SETS, TRANSFORM_METHOD, TRANSFORM_FROM_METHOD -> MethodReference
                CONSTRUCTOR_TO_FACTORY_REDIRECT -> ConstructorReference
                FIELD_REDIRECT, ADD_FIELD_TO_SETS, FIELD_TO_METHOD_REDIRECT, ADD_FIELD_TO_METHOD_TO_SETS -> FieldReference
                REF -> RefReference
                else -> return
            }

            if (pair.value?.references?.all { it.resolve() != null } == true) {
                return
            }
            holder.registerProblem(
                pair,
                "Unresolved reference",
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
            )
        }
    }
}