package io.github.notstirred.dasmplugin.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import io.github.notstirred.dasmplugin.DasmConfigFileType

abstract class DasmConfigInspection : LocalInspectionTool() {
    private fun isDasmFileType(file: PsiFile): Boolean {
        return file.fileType === DasmConfigFileType
    }

    abstract fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (isDasmFileType(holder.file)) {
            return buildVisitor(holder)
        }
        return PsiElementVisitor.EMPTY_VISITOR
    }

    override fun processFile(file: PsiFile, manager: InspectionManager): List<ProblemDescriptor> {
        return if (isDasmFileType(file)) {
            return super.processFile(file, manager)
        } else {
            listOf()
        }
    }
}