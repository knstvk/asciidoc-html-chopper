package chopper

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.InputStreamReader
import java.util.*

fun main(args: Array<String>) {
    var inputFileArg: String? = null
    var outputDirArg: String? = null
    var locale: String = ""
    for ((idx, arg) in args.withIndex()) {
        if (arg.equals("-inputFile")) {
            inputFileArg = getArg(args, idx)
        } else if (arg.equals("-outputDir")) {
            outputDirArg = getArg(args, idx)
        } else if (arg.equals("-loc")) {
            locale = getArg(args, idx)
        }
    }
    if (inputFileArg == null || outputDirArg == null) {
        printUsageAndExit()
    }

    var etcDir = File("etc")
    if (!etcDir.exists()) {
        etcDir = File("../etc")
        if (!etcDir.exists()) {
            println("'etc' directory is not found")
            System.exit(-1)
        }
    }

    val startTime = System.currentTimeMillis()

    val input = File(inputFileArg)
    val srcDir = input.parentFile
    val doc = Jsoup.parse(input, "UTF-8")

    val vars = loadProperties(etcDir, locale, doc)

    val headerEl = doc.getElementById("header") ?: throw IllegalStateException("Element with id='header' is not found")
    val contentEl = doc.getElementById("content") ?: throw IllegalStateException("Element with id='content' is not found")
    doc.getElementById("toc")?.remove()
    val rootSect = Section(contentEl, 0, null)
    rootSect.headerEl = headerEl

    val docTitle = headerEl.getElementsByTag("h1").first()?.ownText()
    rootSect.title = docTitle ?: "Index"
    rootSect.pageTitle = rootSect.title
    rootSect.tocItem = vars.getProperty("home")
    rootSect.parse()

    val links: MutableMap<String, Section> = HashMap()
    collectLinks(rootSect, links)

    val outputDir = prepareOutputDir(outputDirArg, rootSect, srcDir, etcDir, vars)

    val indexWriter = createIndexWriter(outputDir)

    rootSect.write(Context(outputDir, etcDir, links, vars, indexWriter))

    indexWriter.close()

    println("""Done in ${(System.currentTimeMillis() - startTime) / 1000}s""")
}

private fun getArg(args: Array<String>, i: Int): String {
    if (i > args.size - 2) {
        printUsageAndExit()
    }
    return args[i + 1];
}

private fun printUsageAndExit() {
    println("""Arguments:
-inputFile    path to the source root HTML file
-outputDir    path to the output directory""")
    System.exit(-1)
}

private fun loadProperties(etcDir: File, locale: String, doc: Document): Properties {
    val vars = Properties()

    // load from file
    val file = File(etcDir, if (locale.isEmpty()) "var.properties" else "var_$locale.properties")
    if (file.exists()) {
        val inputStream = file.inputStream()
        vars.load(InputStreamReader(inputStream, "UTF-8"))
        inputStream.close()
    }

    // add system properties
    for (name in System.getProperties().stringPropertyNames()) {
        if (name.startsWith("chopper.")) {
            vars.setProperty(name.substring("chopper.".length), System.getProperty(name))
        }
    }

    // add some properties from the source HTML file
    val titleEl = doc.body().getElementsByTag("h1").first()
    if (titleEl != null && !vars.containsKey("title")) {
        vars.setProperty("title", titleEl.ownText());
    }
    val versionEl = doc.body().getElementById("revnumber")
    if (versionEl != null && !vars.containsKey("version")) {
        vars.setProperty("version", versionEl.ownText());
    }
    val copyrightEl = doc.body().getElementById("revremark")
    if (copyrightEl != null && !vars.containsKey("copyright")) {
        vars.setProperty("copyright", copyrightEl.ownText());
    }

    return vars
}

private fun collectLinks(sect: Section, links: MutableMap<String, Section>) {
    if (sect.parent != null) {
        for (id in sect.ids) {
            links.put(id, sect)
        }
    }
    for (childSect in sect.children) {
        collectLinks(childSect, links)
    }
}

private fun prepareOutputDir(outputDirArg: String?, rootSect: Section, srcDir: File,
                             etcDir: File, vars: Properties): File {
    val outputDir = File(outputDirArg)
    if (outputDir.exists()) {
        outputDir.deleteRecursively()
    }
    outputDir.mkdir()

    srcDir.listFiles { file: File -> file.isDirectory }.forEach {
        it.copyRecursively(File(outputDir, it.name))
    }

    val warTemplDir = File(etcDir, "war")
    for (file in warTemplDir.walk()) {
        val relPath = warTemplDir.toPath().relativize(file.toPath())
        val dstFile = File(outputDir, relPath.toString())
        if (file.isDirectory) {
            dstFile.mkdir()
        } else {
            if (arrayOf("jsp", "html", "css").contains(file.extension)) {
                var text = file.readText("UTF-8")
                vars.setProperty("toc", rootSect.createToc())
                for (name in vars.stringPropertyNames()) {
                    text = text.replace("{{" + name + "}}", vars.getProperty(name))
                }
                dstFile.writeText(text, "UTF-8")
            } else {
                file.copyTo(dstFile)
            }
        }
    }
    return outputDir
}

private fun createIndexWriter(outputDir: File): IndexWriter {
    val dir = FSDirectory.open(File(outputDir, "index").toPath())
    val analyzer = StandardAnalyzer()
    val iwc = IndexWriterConfig(analyzer)
    iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
    val indexWriter = IndexWriter(dir, iwc)
    return indexWriter
}
