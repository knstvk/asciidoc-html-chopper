package knstvk.chopper

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.util.*

fun main(args: Array<String>) {
//    val srcDir = File("""input\simple\en\html-single""")
//    val input = File(srcDir, "simple.html")
    val srcDir = File("""input\manual\en\html-single""")
    val input = File(srcDir, "manual.html")
    val doc = Jsoup.parse(input, "UTF-8")

    val rootSect = Section(doc, 0, null)
    rootSect.parse()

    val links: MutableMap<String, Section> = HashMap()
    collectLinks(rootSect, links)

    val outputDir = File("output")
    if (outputDir.exists()) {
        outputDir.deleteRecursively()
    }
    outputDir.mkdir()

    srcDir.listFiles { file: File -> file.isDirectory }.forEach {
        it.copyRecursively(File(outputDir, it.name))
    }

    val warTemplDir = File("templates", "war")
    warTemplDir.listFiles().forEach {
        it.copyRecursively(File(outputDir, it.name))
    }

    write(outputDir, doc, rootSect, links)
}

fun collectLinks(sect: Section, links: MutableMap<String, Section>) {
    if (sect.parent != null) {
        for (id in sect.ids) {
            links.put(id, sect)
        }
    }
    for (childSect in sect.children) {
        collectLinks(childSect, links)
    }
}

fun write(dir: File, doc: Document, rootSect: Section, links: MutableMap<String, Section>) {
    rootSect.replaceLinks(links)
    val tocEl = doc.body().getElementById("toc") ?: throw IllegalStateException("'toc' element is not found")
    val firstTocEl = tocEl.getElementsByClass("sectlevel1").first()
    firstTocEl.prepend("""<li><a href="index.html">Home</a></li>""")

    val file = File(dir, "index.html")
    file.writeText(doc.outerHtml(), "UTF-8")
    for (sect in rootSect.children) {
        sect.write(dir, links, tocEl)
    }
}
