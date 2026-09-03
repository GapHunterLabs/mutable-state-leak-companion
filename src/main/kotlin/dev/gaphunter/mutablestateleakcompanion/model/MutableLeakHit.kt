package dev.gaphunter.mutablestateleakcompanion.model

import com.intellij.psi.PsiElement

/** A confirmed encapsulation break, PROVEN exploited (not just theoretically possible): a getter of [declaringClassName] returns its own [fieldName] field directly, and this caller (a completely different class/method) assigns the result to a local variable and then mutates it at [anchor]. */
data class MutableLeakHit(val anchor: PsiElement, val declaringClassName: String, val fieldName: String)
