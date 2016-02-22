package chopper

import java.io.File
import java.util.*

data class Context(
        val outputDir: File,
        val etcDir: File,
        val links: Map<String, Section>,
        val vars: Properties,
        val indexContent: StringBuilder
)
