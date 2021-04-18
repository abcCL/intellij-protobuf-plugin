package io.kanro.idea.plugin.protobuf.string.case

object PascalCaseFormatter : BaseCaseFormatter() {
    override fun formatWord(index: Int, word: CharSequence): CharSequence {
        return buildString {
            append(word.first().toUpperCase())
            append(word.substring(1).toLowerCase())
        }
    }
}