package io.github.notstirred.dasmplugin.inspection

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElementVisitor
import io.github.notstirred.dasmplugin.reference.InspectionReference

class UnresolvedReferenceInspection : DasmConfigInspection() {
    override fun getStaticDescription() = "Reports invalid imports in DASM configuration files"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JsonElementVisitor() {
        override fun visitStringLiteral(literal: JsonStringLiteral) {
            for (reference in literal.references) {
                if (reference !is InspectionReference) {
                    continue
                }

                if (reference.unresolved) {
                    holder.registerProblem(
                        literal,
                        "Cannot resolve ${reference.description}".format(reference.canonicalText),
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                        reference.rangeInElement
                    )
                }
            }
        }
        override fun visitProperty(prop: JsonProperty) {
            return
        }
    }
}