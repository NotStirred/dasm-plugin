package io.github.notstirred.dasm.plugin.inspection

import com.demonwav.mcdev.util.createLiteralExpression
import com.demonwav.mcdev.util.descriptor
import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.psi.*
import io.github.notstirred.dasm.plugin.parseRef

class DeprecatedDasmSigInspection : AbstractBaseJavaLocalInspectionTool() {
    override fun getStaticDescription() = "Reports unresolves references in Dasm annotations"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = Visitor(holder, isOnTheFly)

    private class Visitor(private val holder: ProblemsHolder, private val isOnTheFly: Boolean) : JavaElementVisitor() {

        override fun visitNameValuePair(pair: PsiNameValuePair) {
            if (pair.value !is PsiAnnotation)
                return
            when ((pair.value as PsiAnnotation).nameReferenceElement?.text) {
                "FieldSig" -> {
                    holder.registerProblem(
                        pair,
                        "Unresolved reference",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                        object : LocalQuickFix {
                            override fun getFamilyName(): @IntentionFamilyName String {
                                return "Dasm deprecation"
                            }

                            override fun applyFix(
                                project: Project,
                                descriptor: ProblemDescriptor
                            ) {
                                val pair = descriptor.psiElement as PsiNameValuePair
                                if (pair.value !is PsiAnnotation) {
                                    return
                                }

                                val attributes = (pair.value as PsiAnnotation).parameterList.attributes
                                val name = attributes.find { it -> it.name == "name" }?.let {
                                    (it.value as PsiLiteralExpression).value as String
                                }
                                val typeFqn = attributes.find { it -> it.name == "type" }?.let {
                                    val psiType = parseRef(it.value as PsiAnnotation, project)
                                    return@let psiType?.descriptor
                                }
                                if (name != null && typeFqn != null) {
                                    pair.value?.replace(
                                        JavaPsiFacade.getInstance(project).elementFactory.createLiteralExpression("$name:$typeFqn")
                                    )

                                    System.err.println(isOnTheFly)
                                    ProjectInspectionProfileManager.getInstance(project).fireProfileChanged()
                                }
                            }
                        }
                    )
                }

                "MethodSig" -> {
                    holder.registerProblem(
                        pair,
                        "Unresolved reference",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                        object : LocalQuickFix {
                            override fun getFamilyName(): @IntentionFamilyName String {
                                return "Dasm deprecation"
                            }

                            override fun applyFix(
                                project: Project,
                                descriptor: ProblemDescriptor
                            ) {
                                val pair = descriptor.psiElement as PsiNameValuePair
                                if (pair.value!!.lastChild.children.size < 2) {
                                    return
                                }

                                val stringPart = pair.value!!.lastChild.children.get(1).firstChild.copy()

                                pair.value?.replace(stringPart)

                                ProjectInspectionProfileManager.getInstance(project).fireProfileChanged()
                            }
                        }
                    )
                }

                "ConstructorMethodSig" -> {
                    holder.registerProblem(
                        pair,
                        "Unresolved reference",
                        ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                        object : LocalQuickFix {
                            override fun getFamilyName(): @IntentionFamilyName String {
                                return "Dasm deprecation"
                            }

                            override fun applyFix(
                                project: Project,
                                descriptor: ProblemDescriptor
                            ) {
                                val pair = descriptor.psiElement as PsiNameValuePair
                                if (pair.value !is PsiAnnotation) {
                                    return
                                }

                                val attributes = (pair.value as PsiAnnotation).parameterList.attributes
                                val args = attributes.find { it -> it.name == "args" }?.let {
                                    (it.value as PsiArrayInitializerMemberValue).initializers
                                }
                                val text = StringBuilder().append("<init>(")
                                args?.map {
                                    val psiType = parseRef(it as PsiAnnotation, project)
                                    if (psiType == null) {
                                        return@applyFix;
                                    }
                                    psiType.descriptor
                                }?.forEach {
                                    text.append(it)
                                }
                                text.append(")V")

                                pair.value?.replace(
                                    JavaPsiFacade.getInstance(project).elementFactory.createLiteralExpression(text.toString())
                                )

                                ProjectInspectionProfileManager.getInstance(project).fireProfileChanged()
                            }
                        }
                    )
                }
            }
        }
    }
}
