package chopper

import org.apache.lucene.index.IndexWriter
import java.io.File
import java.util.*

data class Context(
        val outputDir: File,
        val etcDir: File,
        val links: Map<String, Section>,
        val vars: Properties,
        val indexWriter: IndexWriter
)
