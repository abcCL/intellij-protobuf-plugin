package io.kanro.idea.plugin.protobuf.lang.psi.stub.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.QualifiedName
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufServiceDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.stub.ProtobufStub
import io.kanro.idea.plugin.protobuf.lang.psi.stub.primitive.ProtobufNamedStub
import io.kanro.idea.plugin.protobuf.lang.psi.stub.primitive.ProtobufScopeStub
import io.kanro.idea.plugin.protobuf.lang.psi.stub.type.ProtobufServiceStubType

class ProtobufServiceStub(
    data: Array<String>,
    parent: StubElement<*>?
) : ProtobufStubBase<ProtobufServiceDefinition>(data, parent, ProtobufServiceStubType),
    ProtobufStub<ProtobufServiceDefinition>,
    ProtobufNamedStub,
    ProtobufScopeStub {
    override fun name(): String? {
        return data(0).takeIf { it.isNotEmpty() }
    }

    override fun scope(): QualifiedName? {
        return qualifiedName()
    }
}