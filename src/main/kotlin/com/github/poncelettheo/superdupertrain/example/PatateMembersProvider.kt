package com.github.poncelettheo.superdupertrain.example

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.codeInsight.PyCustomMember
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.types.PyClassMembersProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext

class PatateMembersProvider : PyClassMembersProviderBase() {
    override fun getMembers(
        clazz: PyClassType?,
        location: PsiElement?,
        context: TypeEvalContext
    ): Collection<PyCustomMember> {
        val pyClass: PyClass = clazz?.pyClass ?: return super.getMembers(clazz, location, context)

        if (!isDjangoModel(pyClass, context)) return super.getMembers(clazz, location, context)

        return listOf(PyCustomMember("patate"))
    }


    private fun isDjangoModel(pyClass: PyClass, context: TypeEvalContext): Boolean {
        return pyClass.isSubclass("django.db.models.Model", context) || pyClass.isSubclass("django.db.models.base.Model", context)
    }
}