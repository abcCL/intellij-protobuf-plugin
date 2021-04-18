package io.kanro.idea.plugin.protobuf.lang.psi.stub.impl

import com.intellij.psi.stubs.StubElement
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufOneofDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.stub.ProtobufStub
import io.kanro.idea.plugin.protobuf.lang.psi.stub.primitive.ProtobufNamedStub
import io.kanro.idea.plugin.protobuf.lang.psi.stub.primitive.ProtobufVirtualScopeStub
import io.kanro.idea.plugin.protobuf.lang.psi.stub.type.ProtobufOneofStubType

class ProtobufOneofStub(
    data: Array<String>,
    parent: StubElement<*>?
) : ProtobufStubBase<ProtobufOneofDefinition>(data, parent, ProtobufOneofStubType),
    ProtobufStub<ProtobufOneofDefinition>,
    ProtobufNamedStub,
    ProtobufVirtualScopeStub {
    override fun name(): String? {
        return data(0).takeIf { it.isNotEmpty() }
    }
}