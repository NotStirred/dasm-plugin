package io.github.notstirred.dasmplugin

object DasmLanguage {
    val KEYWORDS = setOf(
        "imports",
        "targets", "defaultSets", "useSets", "targetMethods", "newName", "mappingsOwner", "makeSyntheticAccessor",
        "sets", "typeRedirects", "fieldRedirects", "methodRedirects",
        "isDstInterface"
    )

    val PRIMITIVES = setOf(
        "void", "boolean", "char",
        "long", "int", "short", "byte",
        "double", "float"
    )
}