package dev.gaphunter.mutablestateleakcompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiArrayAccessExpression
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import dev.gaphunter.mutablestateleakcompanion.model.MutableLeakHit

/**
 * Finds `var local = obj.getXxx();` where `getXxx` (real PSI method
 * resolution -- naturally crosses into ANY other class/file in the
 * project, no custom whole-project cache needed for this one-hop
 * shape) returns one of ITS OWN mutable fields directly
 * ([MutableFieldReturnResolver]), then scans the REST of the SAME
 * containing method for a real mutation of `local`
 * (`.add`/`.put`/`.remove`/`.clear`/etc., or an array-index write) --
 * PROOF the leaked reference is actually exploited, not just a
 * getter shaped in a risky way (the whole gap SpotBugs' own
 * `EI_EXPOSE_REP` heuristic never verifies).
 */
object MutableStateLeakFinder {

    private val MUTATING_METHOD_NAMES = setOf("add", "addAll", "put", "putAll", "remove", "removeAll", "removeIf", "clear", "set", "sort", "retainAll")

    fun findAll(file: PsiFile): List<MutableLeakHit> {
        val hits = mutableListOf<MutableLeakHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                val body = method.body ?: return
                hits += hitsInMethodBody(body)
            }
        })
        return hits
    }

    private fun hitsInMethodBody(body: PsiCodeBlock): List<MutableLeakHit> {
        val hits = mutableListOf<MutableLeakHit>()
        body.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitLocalVariable(variable: PsiLocalVariable) {
                super.visitLocalVariable(variable)
                val initializer = variable.initializer as? PsiMethodCallExpression ?: return
                val calleeMethod = initializer.resolveMethod() ?: return
                val field = MutableFieldReturnResolver.mutableFieldReturnedBy(calleeMethod) ?: return
                val declaringClassName = calleeMethod.containingClass?.name ?: return
                val fieldName = field.name ?: return

                val mutationAnchor = findMutationOf(variable, body) ?: return
                hits += MutableLeakHit(mutationAnchor, declaringClassName, fieldName)
            }
        })
        return hits
    }

    private fun findMutationOf(variable: PsiLocalVariable, scope: PsiElement): PsiElement? {
        var found: PsiElement? = null
        scope.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
                if (found != null) return
                super.visitMethodCallExpression(call)
                val methodName = call.methodExpression.referenceName ?: return
                if (methodName !in MUTATING_METHOD_NAMES) return
                val qualifier = call.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return
                if (qualifier.resolve() == variable) found = call.methodExpression.referenceNameElement ?: call.methodExpression
            }

            override fun visitArrayAccessExpression(expression: PsiArrayAccessExpression) {
                if (found != null) return
                super.visitArrayAccessExpression(expression)
                val arrayRef = expression.arrayExpression as? PsiReferenceExpression ?: return
                if (arrayRef.resolve() != variable) return
                val assignment = expression.parent as? PsiAssignmentExpression ?: return
                if (assignment.lExpression == expression) found = expression
            }
        })
        return found
    }
}
