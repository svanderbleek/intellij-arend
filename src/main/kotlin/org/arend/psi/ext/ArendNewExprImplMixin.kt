package org.arend.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.arend.naming.reference.TypedReferable
import org.arend.naming.scope.LazyScope
import org.arend.psi.*
import org.arend.term.abs.Abstract
import org.arend.term.abs.AbstractExpressionVisitor
import org.arend.resolving.util.ReferableExtractVisitor

abstract class ArendNewExprImplMixin(node: ASTNode) : ArendExprImplMixin(node), ClassReferenceHolder {
    abstract val appPrefix: ArendAppPrefix?

    abstract val lbrace: PsiElement?

    abstract val argumentAppExpr: ArendArgumentAppExpr?

    open val appExpr: ArendAppExpr?
        get() = null

    open val argumentList: List<ArendArgument>
        get() = emptyList()

    abstract val localCoClauseList: List<ArendLocalCoClause>

    open val withBody: ArendWithBody?
        get() = null

    fun isVariable() = false

    fun getExpression() = this

    override fun <P : Any?, R : Any?> accept(visitor: AbstractExpressionVisitor<in P, out R>, params: P?): R {
        val prefix = appPrefix
        val body = withBody
        val lbrace = lbrace
        if (prefix == null && lbrace == null && argumentList.isEmpty() && body == null) {
            val expr = appExpr ?: return visitor.visitInferHole(this, params)
            return expr.accept(visitor, params)
        }
        val evalKind = if (prefix == null) null else when {
            prefix.pevalKw != null -> Abstract.EvalKind.PEVAL
            prefix.evalKw != null -> Abstract.EvalKind.EVAL
            else -> null
        }
        return visitor.visitClassExt(this, prefix?.newKw != null, evalKind, if (prefix != null) argumentAppExpr else appExpr, lbrace, if (lbrace == null) null else localCoClauseList, argumentList, body, params)
    }

    private fun getClassReference(onlyClassRef: Boolean, withAdditionalInfo: Boolean): ClassReferenceData? {
        if (appPrefix?.pevalKw != null) {
            return null
        }

        val visitor = ReferableExtractVisitor(withAdditionalInfo)
        val expr = appExpr as? ArendArgumentAppExpr ?: argumentAppExpr
        val ref = visitor.findReferable(expr)
        val classRef = (if (expr != null) visitor.findClassReference(ref, LazyScope { expr.scope }) else null) ?: (if (!onlyClassRef && appPrefix?.newKw != null && ref is TypedReferable) ref.typeClassReference else null) ?: return null
        return ClassReferenceData(classRef, visitor.argumentsExplicitness, emptySet(), false)
    }

    override fun getClassReference() = getClassReference(onlyClassRef = false, withAdditionalInfo = false)?.classRef

    override fun getClassReferenceData(onlyClassRef: Boolean) = getClassReference(onlyClassRef, true)

    override fun getCoClauseElements(): List<ArendLocalCoClause> = if (appPrefix?.pevalKw != null) emptyList() else localCoClauseList
}
