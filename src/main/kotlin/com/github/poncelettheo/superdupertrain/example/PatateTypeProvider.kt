package com.github.poncelettheo.superdupertrain.example


import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.*
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyBoolLiteralExpression
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.resolve.PyResolveContext

class PatateTypeProvider : PyTypeProviderBase() {

    override fun getReferenceExpressionType(referenceExpression: PyReferenceExpression, context: TypeEvalContext): PyType? {
//        val name = referenceExpression.name ?: return null
//        if (!name.endsWith("_patate")) return null
//
//        val target = referenceExpression.reference.resolve()?.navigationElement as? PyTargetExpression ?: return null
//
//        if (!isNullableDjangoField(target, context)) return null
//
//        val qualifier = referenceExpression.qualifier ?: return null
//        val qualType = context.getType(qualifier) ?: return null
//
//        val members = qualType.resolveMember(
//            name.removeSuffix("_patate"),
//            referenceExpression,
//            AccessDirection.READ,
//            PyResolveContext.defaultContext(context)
//        ) ?: return null
//
//        val memberTypes = members.mapNotNull { rr ->
//            (rr.element as? PyTargetExpression)?.let { context.getType(it) }
//        }
//
//        val valueType = when {
//            memberTypes.isEmpty() -> return null
//            memberTypes.size == 1 -> memberTypes.first()
//            else -> PyUnionType.union(memberTypes)
//        }
//
        return super.getReferenceExpressionType(referenceExpression, context)
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

}