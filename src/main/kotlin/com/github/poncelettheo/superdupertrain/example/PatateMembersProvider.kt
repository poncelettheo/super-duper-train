package com.github.poncelettheo.superdupertrain.example

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.codeInsight.PyCustomMember
import com.jetbrains.python.psi.PyBoolLiteralExpression
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.types.PyClassMembersProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext
import kotlin.math.exp

class PatateMembersProvider : PyClassMembersProviderBase() {
    override fun getMembers(
        clazz: PyClassType?,
        location: PsiElement?,
        context: TypeEvalContext
    ): Collection<PyCustomMember> {
        val pyClass: PyClass = clazz?.pyClass ?: return super.getMembers(clazz, location, context)

        if (!isDjangoModel(pyClass, context)) return super.getMembers(clazz, location, context)

        if (pyClass.qualifiedName == "blog.models.article.Article") {
            pyClass.classAttributes.filter {isNullableDjangoField(it,context)}
        }


        return listOf(PyCustomMember("patate"))
    }

    private fun isNullableDjangoField(expression: PyTargetExpression, context: TypeEvalContext): Boolean {
        val call = expression.findAssignedValue() as? PyCallExpression ?: return false
        if (!isDjangoFieldCall(call,context)) return false
        if (!isNullable(call)) return false
        return true
    }

    private fun isNullable(call: PyCallExpression): Boolean {
        val value = call.getKeywordArgument("null") ?: return false
        return (value as? PyBoolLiteralExpression)?.text == "True"
    }

    private fun isDjangoFieldCall(call: PyCallExpression, context: TypeEvalContext): Boolean {
        val callee = call.callee ?: return false
        val calleeClass = callee.reference?.resolve() as? PyClass ?: return false

        return (calleeClass.isSubclass("django.db.models.Field", context)
            || calleeClass.isSubclass("django.db.models.fields.Field", context))

    }


    private fun isDjangoModel(pyClass: PyClass, context: TypeEvalContext): Boolean {
        return pyClass.isSubclass("django.db.models.Model", context) || pyClass.isSubclass("django.db.models.base.Model", context)
    }
}