package org.vclang.psi.ext

import com.intellij.lang.ASTNode
import com.jetbrains.jetpad.vclang.naming.reference.LongUnresolvedReference
import com.jetbrains.jetpad.vclang.naming.reference.UnresolvedReference
import com.jetbrains.jetpad.vclang.term.abs.Abstract
import org.vclang.psi.VcLongName


abstract class VcLongNameImplMixin(node: ASTNode) : VcCompositeElementImpl(node), VcLongName {
    override fun getData() = this

    override fun getReferent(): UnresolvedReference =
        LongUnresolvedReference(this, prefixName.referenceName, refIdentifierList.map { it.referenceName })

    override fun getHeadReference(): Abstract.Reference = prefixName

    override fun getTailReferences(): List<Abstract.Reference> = refIdentifierList
}