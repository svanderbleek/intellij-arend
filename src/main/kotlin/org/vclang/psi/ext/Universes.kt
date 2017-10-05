package org.vclang.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.jetbrains.jetpad.vclang.term.abs.Abstract
import com.jetbrains.jetpad.vclang.term.abs.AbstractExpressionVisitor
import org.vclang.psi.VcSetUniverseBinOp
import org.vclang.psi.VcTruncatedUniverseBinOp
import org.vclang.psi.VcUniverseAtom
import org.vclang.psi.VcUniverseBinOp


private fun <P : Any?, R : Any?> acceptSet(data: Any, setElem: PsiElement, pLevel: Abstract.LevelExpression?, visitor: AbstractExpressionVisitor<in P, out R>, params: P?): R =
    visitor.visitUniverse(data, setElem.text.substring("\\Set".length).toIntOrNull(), 0, pLevel, null, params)

private fun <P : Any?, R : Any?> acceptUniverse(data: Any, universeElem: PsiElement, pLevel: Abstract.LevelExpression?, hLevel: Abstract.LevelExpression?, visitor: AbstractExpressionVisitor<in P, out R>, params: P?): R =
    visitor.visitUniverse(data, universeElem.text.substring("\\Type".length).toIntOrNull(), null, pLevel, hLevel, params)

private fun <P : Any?, R : Any?> acceptTruncated(data: Any, truncatedElem: PsiElement, pLevel: Abstract.LevelExpression?, visitor: AbstractExpressionVisitor<in P, out R>, params: P?): R {
    val uniText = truncatedElem.text
    val index = uniText.indexOf('-')
    val hLevelNum = when {
        uniText.startsWith("\\oo-")      -> Abstract.INFINITY_LEVEL
        index >= 0 && uniText[0] == '\\' -> uniText.substring(1, index).toIntOrNull()
        else                             -> null
    }
    val pLevelNum = if (hLevelNum != null) uniText.substring(index + "-Type".length).toIntOrNull() else null
    return visitor.visitUniverse(data, pLevelNum, hLevelNum, pLevel, null, params)
}


abstract class VcSetUniverseBinOpImplMixin(node: ASTNode) : VcExprImplMixin(node), VcSetUniverseBinOp {
    override fun <P : Any?, R : Any?> accept(visitor: AbstractExpressionVisitor<in P, out R>, params: P?): R =
        acceptSet(this, set, atomLevelExpr, visitor, params)
}

abstract class VcTruncatedUniverseBinOpImplMixin(node: ASTNode) : VcExprImplMixin(node), VcTruncatedUniverseBinOp {
    override fun <P : Any?, R : Any?> accept(visitor: AbstractExpressionVisitor<in P, out R>, params: P?): R =
        acceptTruncated(this, truncatedUniverse, atomLevelExpr, visitor, params)
}

abstract class VcUniverseBinOpImplMixin(node: ASTNode) : VcExprImplMixin(node), VcUniverseBinOp {
    override fun <P : Any?, R : Any?> accept(visitor: AbstractExpressionVisitor<in P, out R>, params: P?): R {
        val levelExprs = atomLevelExprList
        return acceptUniverse(this, universe, levelExprs.getOrNull(0), levelExprs.getOrNull(1), visitor, params)
    }
}

abstract class VcUniverseAtomImplMixin(node: ASTNode) : VcExprImplMixin(node), VcUniverseAtom {
    override fun <P : Any?, R : Any?> accept(visitor: AbstractExpressionVisitor<in P, out R>, params: P?): R {
        set?.let { return acceptSet(this, it, null, visitor, params) }
        universe?.let { return acceptUniverse(this, it, null, null, visitor, params) }
        truncatedUniverse?.let { return acceptTruncated(this, it, null, visitor, params) }
        error("Incorrect expression: universe")
    }
}