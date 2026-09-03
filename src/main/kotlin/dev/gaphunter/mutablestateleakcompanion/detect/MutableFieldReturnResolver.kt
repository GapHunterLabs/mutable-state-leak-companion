package dev.gaphunter.mutablestateleakcompanion.detect

import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.PsiThisExpression
import com.intellij.psi.PsiType

/**
 * Resolves whether [method]'s ENTIRE body is exactly
 * `return this.field;`/`return field;` for a field whose type is a
 * mutable collection or an array -- the real `EI_EXPOSE_REP` shape
 * (SpotBugs' own name for this bug class), checked structurally
 * rather than by bytecode pattern.
 */
object MutableFieldReturnResolver {

    private val MUTABLE_TYPE_SIMPLE_NAMES = setOf(
        "List", "ArrayList", "LinkedList",
        "Map", "HashMap", "TreeMap", "LinkedHashMap",
        "Set", "HashSet", "TreeSet", "LinkedHashSet",
        "Collection",
    )

    fun mutableFieldReturnedBy(method: PsiMethod): PsiField? {
        val body = method.body ?: return null
        val statements = body.statements
        if (statements.size != 1) return null
        val returnStatement = statements[0] as? PsiReturnStatement ?: return null
        val returnValue = returnStatement.returnValue as? PsiReferenceExpression ?: return null

        val qualifier = returnValue.qualifierExpression
        if (qualifier != null && qualifier !is PsiThisExpression) return null

        val field = returnValue.resolve() as? PsiField ?: return null
        if (field.containingClass != method.containingClass) return null
        return if (isMutableCollectionOrArray(field.type)) field else null
    }

    private fun isMutableCollectionOrArray(type: PsiType): Boolean {
        if (type is PsiArrayType) return true
        return (type as? PsiClassType)?.className in MUTABLE_TYPE_SIMPLE_NAMES
    }
}
