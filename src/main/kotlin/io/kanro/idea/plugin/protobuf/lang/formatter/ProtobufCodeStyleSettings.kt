package io.kanro.idea.plugin.protobuf.lang.formatter

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings

class ProtobufCodeStyleSettings(settings: CodeStyleSettings) :
    CustomCodeStyleSettings("ProtobufCodeStyleSettings", settings) {

    @JvmField
    var BLANK_LINES_AFTER_SYNTAX = 1
    @JvmField
    var BLANK_LINES_AFTER_FILE_OPTIONS = 1

    @JvmField
    var KEEP_BLANK_LINES_BETWEEN_IMPORTS = 1
    @JvmField
    var KEEP_BLANK_LINES_BETWEEN_FILE_OPTIONS = 1
}
