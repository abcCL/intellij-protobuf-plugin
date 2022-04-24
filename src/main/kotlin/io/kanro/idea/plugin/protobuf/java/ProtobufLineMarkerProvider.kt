package io.kanro.idea.plugin.protobuf.java

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import io.kanro.idea.plugin.protobuf.Icons
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufIdentifier
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufRpcDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufServiceDefinition

class ProtobufLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val identifier = element as? ProtobufIdentifier ?: return
        if (!isJava(element)) return

        when (val owner = identifier.parent) {
            is ProtobufRpcDefinition -> {
                val parent = owner.parent.parent
                if (parent is ProtobufServiceDefinition) {
                    val implBaseName = parent.fullImplBaseName()?.toString()
                    val ostrichClass = getOstrichAnnotationClass(element, implBaseName)
                    val methodName = owner.methodName()
                    val resultMethod = mutableListOf<PsiMethod>()
                    if (ostrichClass.isNotEmpty()) {
                        val allMethods = ostrichClass[0].allMethods
                        for (method in allMethods) {
                            if (method.name == methodName) {
                                resultMethod.add(method)
                            }
                        }
                    }
                    val grpcClass = getGrpcAnnotiationClass(element, implBaseName)
                    if (grpcClass.isNotEmpty()) {
                        val allMethods = grpcClass[0].methods
                        for (method in allMethods) {
                            if (method.name == methodName) {
                                resultMethod.add(method)
                            }
                        }
                    }
                    if (resultMethod.isNotEmpty()) {
                        val builder: NavigationGutterIconBuilder<PsiElement> =
                            NavigationGutterIconBuilder.create(Icons.IMPLEMENTED_RPC)
                                .setTargets(resultMethod)
                                .setTooltipText("Implemented")
                        result.add(builder.createLineMarkerInfo(element))
                        return
                    }
                }

//                val methods = owner.toImplBaseMethod()?.let {
//                    OverridingMethodsSearch.search(it).toList()
//                } ?: listOf()
//                val ktMethods = owner.toCoroutineImplBaseMethod()?.let {
//                    OverridingMethodsSearch.search(it).toList()
//                } ?: listOf()
//                if (methods.isEmpty() && ktMethods.isEmpty()) return
//                val builder: NavigationGutterIconBuilder<PsiElement> =
//                    NavigationGutterIconBuilder.create(Icons.IMPLEMENTED_RPC)
//                        .setTargets(methods + ktMethods)
//                        .setTooltipText("Implemented")
//                result.add(builder.createLineMarkerInfo(element))
            }
            is ProtobufServiceDefinition -> {
                val implBaseName = owner.fullImplBaseName()?.toString()
                val ostrichClass = getOstrichAnnotationClass(element, implBaseName)
                if (ostrichClass.isNotEmpty()) {
                    val builder: NavigationGutterIconBuilder<PsiElement> =
                        NavigationGutterIconBuilder.create(Icons.IMPLEMENTED_SERVICE)
                            .setTargets(ostrichClass)
                            .setTooltipText("Implemented")
                    result.add(builder.createLineMarkerInfo(element))
                    return
                }
                val apis = owner.toImplBaseClass()?.let {
                    DirectClassInheritorsSearch.search(it).findAll().toList()
                } ?: listOf()
                val ktApis = owner.toCoroutineImplBaseClass()?.let {
                    DirectClassInheritorsSearch.search(it).findAll().toList()
                } ?: listOf()
                if (apis.isEmpty() && ktApis.isEmpty()) return
                val builder: NavigationGutterIconBuilder<PsiElement> =
                    NavigationGutterIconBuilder.create(Icons.IMPLEMENTED_SERVICE)
                        .setTargets(apis + ktApis)
                        .setTooltipText("Implemented")
                result.add(builder.createLineMarkerInfo(element))
            }
        }
    }

    val grpcService = "com.ypshengxian.ostrich.springboot.annotations.GrpcService"
    val ostrichService = "com.ypshengxian.ostrich.springboot.annotations.OstrichService"
    fun getOstrichAnnotationClass(element: PsiElement, baseName: String): List<PsiClass> {
        val files = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(element.project))
        val psiJavaFile = mutableListOf<PsiJavaFileImpl>()
        for (file in files) {
            val psiFile = PsiManager.getInstance(element.project).findFile(file)
            if (psiFile is PsiJavaFileImpl) {
                psiJavaFile.add(psiFile)
            }
        }
        for (javaFile in psiJavaFile) {
            val clazzName = javaFile.packageName + "." + javaFile.name
            val psiClass = findJavaClass(clazzName.substring(0, clazzName.length - 5), element.project)
            psiClass?.annotations ?: continue
            for (an in psiClass.annotations) {
                if (an.qualifiedName.equals(ostrichService)) {
                    if (an.findAttributeValue("value")!!
                            .firstChild.firstChild.reference!!
                            .canonicalText.equals(baseName)
                    )
                        return listOf<PsiClass>(psiClass)
                }
            }
        }
        return emptyList()
    }

    fun getGrpcAnnotiationClass(element: PsiElement, baseName: String): List<PsiClass> {
        val files = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(element.project))
        val psiJavaFile = mutableListOf<PsiJavaFileImpl>()
        for (file in files) {
            val psiFile = PsiManager.getInstance(element.project).findFile(file)
            if (psiFile is PsiJavaFileImpl) {
                psiJavaFile.add(psiFile)
            }
        }
        for (javaFile in psiJavaFile) {
            val clazzName = javaFile.packageName + "." + javaFile.name
            val psiClass = findJavaClass(clazzName.substring(0, clazzName.length - 5), element.project)
            psiClass?.annotations ?: continue
            for (an in psiClass.annotations) {
                if (an.qualifiedName.equals(grpcService)) {
                    val extendsList = psiClass.extendsListTypes
                    for (psiClassType in extendsList) {
                        if (baseName.endsWith(psiClassType.className)) {
                            return listOf<PsiClass>(psiClass)
                        }
                    }
                }
            }
        }
        return emptyList()
    }

    fun findJavaClass(name: String?, project: Project): PsiClass? {
        val className = name ?: return null
        return JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
    }
}
