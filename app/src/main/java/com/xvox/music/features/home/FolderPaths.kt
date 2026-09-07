package com.xvox.music.features.home

/** Full paths, with boundary-aware recursive matching (Music must not match Music2). */
object FolderPaths {
    fun normalize(path: String): String = path.replace('\\', '/').replace(Regex("/+"), "/").trimEnd('/')
    fun contains(parent: String, child: String): Boolean {
        val p = normalize(parent)
        val c = normalize(child)
        return p.isNotEmpty() && (p == c || c.startsWith("$p/"))
    }
    fun isExcluded(path: String, name: String, excluded: Set<String>): Boolean = excluded.any {
        // Preserve old name-based preferences; newly selected folders always use full paths.
        if ('/' !in it) it == name else contains(it, path)
    }
}
