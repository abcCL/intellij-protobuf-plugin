package io.kanro.idea.plugin.protobuf.java

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.QualifiedName
import io.kanro.idea.plugin.protobuf.Icons
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufRpcDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufServiceDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.primitive.ProtobufElement
import io.kanro.idea.plugin.protobuf.lang.root.ProtobufRootResolver
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

class JavaLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val identifier = element.toUElementOfType<UIdentifier>() ?: return
        if (!isJava(element)) return
        when (val parent = identifier.uastParent) {
            is UClass -> {
                val service = findServiceProtobufDefinition(parent) ?: return
                val builder: NavigationGutterIconBuilder<PsiElement> =
                    NavigationGutterIconBuilder.create(Icons.IMPLEMENTING_SERVICE)
                        .setTargets(service)
                        .setTooltipText("Implementing")
                result.add(builder.createLineMarkerInfo(element))
            }
            is UMethod -> {
                val method = findMethodProtobufDefinition(parent) ?: return
                val builder: NavigationGutterIconBuilder<PsiElement> =
                    NavigationGutterIconBuilder.create(Icons.IMPLEMENTING_RPC)
                        .setTargets(method)
                        .setTooltipText("Implementing")
                result.add(builder.createLineMarkerInfo(element))
            }
        }
    }

    private val javaImplBase = QualifiedName.fromDottedString("io.grpc.BindableService")

    val grpcService = "com.ypshengxian.ostrich.springboot.annotations.GrpcService"
    val ostrichService = "com.ypshengxian.ostrich.springboot.annotations.OstrichService"
    fun findServiceProtobufDefinition(clazz: UClass): ProtobufServiceDefinition? {
        val sourceClazz = clazz.sourcePsi ?: return null
        val bindableService = sourceClazz.findJavaClass(javaImplBase) ?: return null
        if (!clazz.javaPsi.isInheritor(bindableService, true)) return null

        return CachedValuesManager.getCachedValue(sourceClazz) {
            val scope = ProtobufRootResolver.searchScope(sourceClazz)
            for (it in clazz.uastSuperTypes) {
                val qualifiedName = it.getQualifiedName() ?: continue
                val element = StubIndex.getElements(
                    JavaNameIndex.key,
                    qualifiedName,
                    sourceClazz.project,
                    scope,
                    ProtobufElement::class.java
                ).firstIsInstanceOrNull<ProtobufServiceDefinition>()

                if (element != null) {
                    return@getCachedValue CachedValueProvider.Result.create(element, sourceClazz)
                }
            }
            return@getCachedValue CachedValueProvider.Result.create(null, sourceClazz)
        }
        for (an in clazz.uAnnotations) {
            an.qualifiedName ?: continue
            when (an.qualifiedName.toString()) {
                grpcService -> {
                    if (!clazz.javaPsi.isInheritor(bindableService, true)) return null
                    return CachedValuesManager.getCachedValue(sourceClazz) {
                        val scope = FileResolver.searchScope(sourceClazz)
                        for (it in clazz.uastSuperTypes) {
                            val qualifiedName = it.getQualifiedName() ?: continue
                            val element = findServiceElement(qualifiedName, sourceClazz.project, scope)
                            if (element != null) {
                                return@getCachedValue CachedValueProvider.Result.create(element, sourceClazz)
                            }
                        }
                        return@getCachedValue CachedValueProvider.Result.create(null, sourceClazz)
                    }
                }
                ostrichService -> {
                    val parentClazz = sourceClazz.findJavaClass(QualifiedName.fromDottedString(an.attributeValues.get(0).expression.toString()))
                            ?: return null
                    if (!parentClazz.isInheritor(bindableService, true)) return null
                    return CachedValuesManager.getCachedValue(sourceClazz) {
                        val scope = FileResolver.searchScope(sourceClazz)
                        val qualifiedName = parentClazz.qualifiedName
                                ?: return@getCachedValue CachedValueProvider.Result.create(null, sourceClazz)
                        val element = findServiceElement(qualifiedName, sourceClazz.project, scope)
                        if (element != null) {
                            return@getCachedValue CachedValueProvider.Result.create(element, sourceClazz)
                        }
                        return@getCachedValue CachedValueProvider.Result.create(null, sourceClazz)
                    }
                }
            }
        }
        return null
    }

    fun findServiceElement(name: String, project: Project, scope: GlobalSearchScope): ProtobufServiceDefinition? {
        return StubIndex.getElements(
                JavaNameIndex.key,
                name,
                project,
                scope,
                ProtobufElement::class.java
        ).firstIsInstanceOrNull<ProtobufServiceDefinition>()
    }

    fun findMethodProtobufDefinition(method: UMethod): ProtobufElement? {
        val sourcePsi = method.sourcePsi ?: return null
        val clazz = method.uastParent as? UClass ?: return null
        val bindableService = sourcePsi.findJavaClass(javaImplBase) ?: return null
        if (!clazz.javaPsi.isInheritor(bindableService, true)) return null

        return CachedValuesManager.getCachedValue(sourcePsi) {
            val scope = ProtobufRootResolver.searchScope(sourcePsi)
            for (it in clazz.uastSuperTypes) {
                val methodName = "${it.getQualifiedName()}.${method.name}"
                val element = StubIndex.getElements(
                    JavaNameIndex.key,
                    methodName,
                    sourcePsi.project,
                    scope,
                    ProtobufElement::class.java
                ).firstIsInstanceOrNull<ProtobufRpcDefinition>()
                if (element != null) {
                    return@getCachedValue CachedValueProvider.Result.create(element, sourcePsi)
                }
            }
            return@getCachedValue CachedValueProvider.Result.create(null, sourcePsi)
        }
        for (an in clazz.uAnnotations) {
            an.qualifiedName ?: continue
            when (an.qualifiedName.toString()) {
                grpcService -> {
                    if (!clazz.javaPsi.isInheritor(bindableService, true)) continue;
                    return CachedValuesManager.getCachedValue(sourcePsi) {
                        val scope = FileResolver.searchScope(sourcePsi)
                        for (it in clazz.uastSuperTypes) {
                            val methodName = "${it.getQualifiedName()}.${method.name}"
                            val element = findMethodElement(methodName, sourcePsi.project, scope)
                            if (element != null) {
                                return@getCachedValue CachedValueProvider.Result.create(element, sourcePsi)
                            }
                        }
                        return@getCachedValue CachedValueProvider.Result.create(null, sourcePsi)
                    }
                }
                ostrichService -> {
                    val parentClazz = clazz.findJavaClass(QualifiedName.fromDottedString(an.attributeValues.get(0).expression.toString()))
                            ?: return null
                    if (!parentClazz.isInheritor(bindableService, true)) return null
                    return CachedValuesManager.getCachedValue(sourcePsi) {
                        val scope = FileResolver.searchScope(sourcePsi)
                        val methodName = "${parentClazz.qualifiedName}.${method.name}"
                        val element = findMethodElement(methodName, sourcePsi.project, scope)
                        if (element != null) {
                            return@getCachedValue CachedValueProvider.Result.create(element, sourcePsi)
                        }
                        return@getCachedValue CachedValueProvider.Result.create(null, sourcePsi)
                    }
                }
            }
        }
        return null
    }

    fun findMethodElement(name: String, project: Project, scope: GlobalSearchScope): ProtobufRpcDefinition? {
        return StubIndex.getElements(
                JavaNameIndex.key,
                name,
                project,
                scope,
                ProtobufElement::class.java
        ).firstIsInstanceOrNull<ProtobufRpcDefinition>()
    }
}
