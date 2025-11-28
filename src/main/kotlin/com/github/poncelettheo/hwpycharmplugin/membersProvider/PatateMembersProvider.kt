package com.github.poncelettheo.hwpycharmplugin.membersProvider

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.codeInsight.PyCustomMember
import com.jetbrains.python.psi.PyBoolLiteralExpression
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.stubs.PyClassNameIndex
import com.jetbrains.python.psi.types.PyClassMembersProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext

class PatateMembersProvider : PyClassMembersProviderBase() {
    override fun getMembers(
        clazz: PyClassType?,
        location: PsiElement?,
        context: TypeEvalContext
    ): Collection<PyCustomMember> {
        /**
         * For each nullable field of a django model, adds a non-nullable version member of this field
         * to the attributes of the model.
         * Reference the non-nullable field through an attribute named <field_name>_patate.
         * Point the non-nullable field to the original nullable field definition and type.
         */
        val pyClass: PyClass = clazz?.pyClass ?: return super.getMembers(clazz, location, context)

        if (!isDjangoModel(pyClass, context)) return super.getMembers(clazz, location, context)

        val name = pyClass.name

        if (name == null) return super.getMembers(clazz, location, context)
        if (!name.endsWith("Provider")) return super.getMembers(clazz, location, context)

        val members = mutableListOf<PyCustomMember>()

        findAllDjangoModels(pyClass, context).map{it.classAttributes.filter{isOneToOnePointingToModel(it,pyClass, context)}}.flatten().mapNotNull{getReverseAccessorName(it)}

        members.addAll(pyClass.classAttributes.filter{isNullableDjangoField(it,context)}.map{getCustomPatateMember(it)})

        return members
    }

    private fun findAllDjangoModels(currentClass: PyClass, context: TypeEvalContext): List<PyClass> {
        val project = currentClass.project

        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val models = computeAllDjangoModels(project, context)
            CachedValueProvider.Result.create(
                models,
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }

    private fun computeAllDjangoModels(project: Project, context: TypeEvalContext): List<PyClass> {
        val models = mutableListOf<PyClass>()
        val scope = GlobalSearchScope.projectScope(project)

        StubIndex.getInstance().processAllKeys(
            PyClassNameIndex.KEY,
            project
        ) { className ->
            PyClassNameIndex.find(className, project, scope).forEach { pyClass ->
                if (isDjangoModel(pyClass, context)) {
                    models.add(pyClass)
                }
            }
            true
        }

        return models
    }

    private fun isOneToOnePointingToModel(expression: PyTargetExpression, targetModel: PyClass, context: TypeEvalContext): Boolean {
        val call = expression.findAssignedValue() as? PyCallExpression ?: return false
        if (!isOneToOneField(call,context)) return false
        if (!pointsToModel(call,targetModel, context)) return false
        return true
    }

    private fun isOneToOneField(call: PyCallExpression, context: TypeEvalContext): Boolean {
        val callee = call.callee ?: return false
        val calleeClass = callee.reference?.resolve() as? PyClass ?: return false

        return (calleeClass.isSubclass("django.db.models.OneToOneField", context)
                || calleeClass.isSubclass("django.db.models.fields.related.OneToOneField", context))
    }

    private fun pointsToModel(call: PyCallExpression, targetModel: PyClass, context: TypeEvalContext): Boolean {
        val arg = call.getKeywordArgument("to") ?: call.arguments.firstOrNull() ?: return false

        if (arg is PyStringLiteralExpression) {
            return arg.stringValue.endsWith(targetModel.name ?: "")
        }

        val referencedClass = arg.reference?.resolve() as? PyClass ?: return false
        return referencedClass == targetModel
    }

    private fun getReverseAccessorName(field: PyTargetExpression): PyCustomMember? {
        val containingClass = field.containingClass ?: return null

        val call = field.findAssignedValue() as? PyCallExpression

        if (call != null) {
            val relatedName = call.getKeywordArgument("related_name")
            if (relatedName is PyStringLiteralExpression) {
                return PyCustomMember(relatedName.stringValue + "_patate", field.containingClass)
            }
        }

        val className = containingClass.name?.lowercase() ?: return null

        println(className)

        return PyCustomMember(className + "_patate", field.containingClass)

    }

    private fun getCustomPatateMember(expression: PyTargetExpression): PyCustomMember {
        return PyCustomMember(expression.text + "_patate", expression)
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