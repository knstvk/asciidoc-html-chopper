package knstvk.chopper

import org.jsoup.nodes.Element
import java.io.File
import java.util.*
import java.util.regex.Pattern

class Section (element: Element, level: Int, parent: Section?) {

    val element: Element = element
    val level: Int = level
    val parent: Section? = parent
    val children: MutableList<Section> = ArrayList()
    val ids: MutableList<String> = ArrayList()

    lateinit var headerEl: Element
    lateinit var id: String
    lateinit var title: String
    lateinit var pageTitle: String

    private val SECTION_NUM_PATTERN = Pattern.compile("""(\d+\.)+ """)

    fun parse() {
        if (parent == null) {
            headerEl = element
            id = "index"
            title = "Home"
            pageTitle = "Home"
        } else {
            val hEls = element.getElementsByTag("h${level + 1}")
            if (hEls.isEmpty())
                throw IllegalStateException("'h${level + 1}' element is not found in section '${getBeginning()}'")
            this.headerEl = hEls[0]

            val id = headerEl.attr("id")
            if (id.isEmpty())
                throw IllegalStateException("No 'id' attribute in '${getBeginning()}'")
            this.id = id

            title = headerEl.ownText()

            pageTitle = SECTION_NUM_PATTERN.matcher(title).replaceFirst("")

            val ancorEls = headerEl.getElementsByClass("anchor")
            if (ancorEls.isNotEmpty()) {
                val href = ancorEls[0].attr("href")
                if (href.startsWith("#") && href.substring(1) == id) {
                    ancorEls[0].remove()
                }
            }
        }

        val nextLevel = this.level + 1
        val sectElements = element.getElementsByClass("sect$nextLevel")
        for (sectEl in sectElements) {
            val sect = Section(sectEl, nextLevel, this)
            children.add(sect)
            sect.parse()
            sectEl.remove()
        }

        ids.addAll(element.getElementsByAttribute("id").map { it.attr("id") })
    }

    private fun getBeginning(): String = element.outerHtml().substring(0, 100) + "..."

    private fun getHtmlContent(): String {
        val sb = StringBuilder()
        sb.append("""<!doctype html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>$pageTitle</title>
<link rel="stylesheet" href="styles/jquery.treeview.css">
<link rel="stylesheet" href="./styles/cuba.css">
<link rel="stylesheet" href="./styles/coderay-asciidoctor.css">
<style>
li.toc-item {
	padding: 3px 0pt 3px 16px;
}
ul.toc-root>li {
	padding: 3px 0pt 3px 0px;
}
#toc .toc-link {
    padding-left: 10px;
}
.toc-marker {
    display: inline-block;
    position: absolute;
    margin-top: 5px;
    border-style: solid;
    border-width: 0 0 6px 6px;
    border-color: transparent;
}
.toc-marker.open {
    border-width: 0 0 6px 6px;
    border-color: transparent transparent rgb(192, 192, 192) transparent;
}
.toc-marker.closed {
    border-width: 4px 0 4px 5px;
    border-color: transparent transparent transparent rgb(192, 192, 192);
}
</style
</head>
<body class="book toc2 toc-left">
 <div id="header">
  <div id="toc" class="toc2">
""")
        sb.append(createToc())
        sb.append("""
  </div>
 </div>
""")
        sb.append("""<div id="content">""")
        sb.append(element.outerHtml())
        sb.append("""
</div>
<script type="text/javascript" src="js/jquery-1.11.1.min.js"></script>
<script type="text/javascript" src="js/jquery.treeview.js"></script>
<script type="text/javascript" src="js/jquery.nearest.min.js"></script>
<script type="text/javascript" src="js/toc-controller-chunked.js"></script>
<a href="#" id="toc-position-marker">. . .</a>
</body>
</html>
""")
        return sb.toString()
    }

    private fun createToc(): String {
        val sb = StringBuilder()

        val parents: MutableList<Section> = getParentsFromTop()

        sb.append("\n<ul class='toc-root'>")
        val root = parents[0]
        sb.append("\n<li><div class='toc-marker'></div>${getTocA(root, false)}</li>");

        val myTopItem = if (parents.size > 1) parents[1] else this

        for (topItem in root.children) {
            if (parents.size > 1) {
                sb.append("\n<li>${getTocMarker(topItem, topItem == myTopItem)}${getTocA(topItem, false)}");
            }
            if (topItem == myTopItem) {
                if (parents.size > 2) {
                    for (p in parents.subList(2, parents.size)) {
                        sb.append("\n<ul>")
                        sb.append("\n<li class='toc-item'>${getTocMarker(p, true)}${getTocA(p, false)}");
                    }
                }

                if (parent != null) {
                    if (parents.size > 1) {
                        sb.append("\n<ul>")
                    }
                    // siblings
                    for (sibling in parent.children) {
                        sb.append("<li")
                        if (parent.parent != null) {
                            // not a first level item
                            sb.append(" class='toc-item'")
                        }
                        sb.append(">${getTocMarker(sibling, sibling === this)}${getTocA(sibling, sibling === this)}")
                        if (sibling === this) {
                            // children
                            if (!children.isEmpty()) {
                                sb.append("\n<ul>")
                                for (child in children) {
                                    sb.append("<li class='toc-item'>${getTocMarker(child, false)}${getTocA(child, false)}</li>")
                                }
                                sb.append("\n</ul>")
                            }
                        }
                        sb.append("</li>")
                    }
                    if (parents.size > 1) {
                        sb.append("\n</ul>")
                    }
                }

                if (parents.size > 2) {
                    for (p in parents.subList(2, parents.size)) {
                        sb.append("\n</li>\n</ul>")
                    }
                }
            }
            sb.append("</li>")
        }
        return sb.toString()
    }

    private fun getTocMarker(section: Section, open: Boolean): String {
        val sb = StringBuilder()
        sb.append("<div class='toc-marker")
        if (!section.children.isEmpty()) {
            if (open)
                sb.append(" open")
            else
                sb.append(" closed")
        }
        sb.append("'></div>")
        return sb.toString()
    }

    private fun getTocA(section: Section, active: Boolean): String {
        val sb = StringBuilder()
        sb.append("<div class='toc-link'><a href='${section.id}.html'")
        if (active)
            sb.append(" class='toc-highlighted'")
        sb.append(">${section.pageTitle}</a></div>")
        return sb.toString()
    }

    private fun getParentsFromTop(): MutableList<Section> {
        val parents: MutableList<Section> = ArrayList()
        var p = parent
        while (p != null) {
            parents.add(p)
            p = p.parent
        }
        parents.reverse()
        return parents
    }

    fun write(dir: File, links: MutableMap<String, Section>, tocEl: Element) {
        replaceLinks(links)

        val file = File(dir, id + ".html")
        file.writeText(getHtmlContent(), "UTF-8")
        for (childSect in children) {
            childSect.write(dir, links, tocEl)
        }
    }

    fun replaceLinks(links: MutableMap<String, Section>) {
        val aElements = element.getElementsByTag("a")
        for (aEl in aElements) {
            val href = aEl.attr("href")
            if (href.startsWith("#")) {
                val ref = href.substring(1)
                val section = links[ref]
                if (section == null) {
                    println("WARNING: unknown link: $href")
                    continue
                }
                if (section !== this) {
                    val newRef = if (ref == section.id) "${section.id}.html" else "${section.id}.html#$ref"
                    aEl.attr("href", newRef)
                }
            }
        }
    }

}
