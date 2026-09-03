package dev.gaphunter.mutablestateleakcompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import dev.gaphunter.mutablestateleakcompanion.detect.MutableStateLeakFinder
import dev.gaphunter.mutablestateleakcompanion.model.MutableLeakHit
import dev.gaphunter.mutablestateleakcompanion.review.ReviewPrompt

/** Flags a real mutation of a getter's directly-returned mutable field -- a confirmed `EI_EXPOSE_REP`-shaped encapsulation break, proven exploited. See [MutableStateLeakFinder]. */
class MutableStateLeakInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null

        val hits = MutableStateLeakFinder.findAll(file)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                messageFor(hit),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber:${hit.declaringClassName}.${hit.fieldName}")
            }
        }

        return problems.toTypedArray()
    }

    private fun messageFor(hit: MutableLeakHit): String =
        "This mutates the value returned by a getter that hands back '${hit.declaringClassName}.${hit.fieldName}' directly -- " +
            "you are mutating ${hit.declaringClassName}'s own internal state from the outside (CWE-374/375), a confirmed " +
            "encapsulation break, not just a theoretical risk"
}
