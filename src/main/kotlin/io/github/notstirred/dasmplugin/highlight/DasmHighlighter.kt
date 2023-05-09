package io.github.notstirred.dasmplugin.highlight

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType.HighlightInfoTypeImpl
import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import io.github.notstirred.dasmplugin.DasmConfigFileType
import io.github.notstirred.dasmplugin.reference.ClassReferenceProvider
import io.github.notstirred.dasmplugin.reference.InspectionReference
import io.github.notstirred.dasmplugin.reference.ReferenceType

class DasmHighlighter : HighlightVisitor {
    companion object {
        val CLASS_HIGHLIGHT = HighlightInfoTypeImpl(
            HighlightSeverity.INFORMATION,
            DefaultLanguageHighlighterColors.CLASS_NAME
        )

        val KEYWORD_HIGHLIGHT = HighlightInfoTypeImpl(
            HighlightSeverity.INFORMATION,
            DefaultLanguageHighlighterColors.KEYWORD
        )

        val METHOD_HIGHLIGHT = HighlightInfoTypeImpl(
            HighlightSeverity.INFORMATION,
            DefaultLanguageHighlighterColors.INSTANCE_METHOD
        )

        val FIELD_HIGHLIGHT = HighlightInfoTypeImpl(
            HighlightSeverity.INFORMATION,
            DefaultLanguageHighlighterColors.INSTANCE_FIELD
        )
    }
    private var holder : HighlightInfoHolder? = null

    override fun suitableForFile(file: PsiFile): Boolean {
        return file.fileType == DasmConfigFileType
    }

    override fun visit(element: PsiElement) {
        if (holder == null) {
            return
        }

        val primitiveRanges = element.getUserData(ClassReferenceProvider.PRIMITIVES_KEY)
        if (primitiveRanges != null) {
            for (primRange in primitiveRanges) {
                holder!!.add(HighlightInfo.newHighlightInfo(KEYWORD_HIGHLIGHT).range(primRange.shiftRight(element.startOffset)).create())
            }
        }

        for (reference in element.references) {
            if (reference is InspectionReference) {
                if (!reference.unresolved) {
                    when (reference.referenceType) {
                        ReferenceType.CLASS -> holder!!.add(HighlightInfo.newHighlightInfo(CLASS_HIGHLIGHT).range(reference.absoluteRange).create())
                        ReferenceType.FIELD -> holder!!.add(HighlightInfo.newHighlightInfo(FIELD_HIGHLIGHT).range(reference.absoluteRange).create())
                        ReferenceType.METHOD -> holder!!.add(HighlightInfo.newHighlightInfo(METHOD_HIGHLIGHT).range(reference.absoluteRange).create())
                    }
                }
            }
        }
    }

    override fun analyze(
        file: PsiFile,
        updateWholeFile: Boolean,
        holder: HighlightInfoHolder,
        action: Runnable
    ): Boolean {
        this.holder = holder;
        action.run()
        return true
    }

    override fun clone(): HighlightVisitor {
        return DasmHighlighter()
    }
}