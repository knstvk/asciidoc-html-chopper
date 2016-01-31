<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.*,java.io.*,java.nio.file.*" %>    
<%@ page import="org.apache.lucene.analysis.*,org.apache.lucene.analysis.standard.StandardAnalyzer" %>    
<%@ page import="org.apache.lucene.document.Document,org.apache.lucene.index.*" %>    
<%@ page import="org.apache.lucene.queryparser.classic.QueryParser,org.apache.lucene.search.*,org.apache.lucene.store.FSDirectory" %>    
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Search Results</title>
    <link rel="stylesheet" href="./styles/cuba.css">
    <link rel="stylesheet" href="./styles/toc.css">
    <link rel="stylesheet" href="./styles/coderay-asciidoctor.css">
    <style>
    </style>
</head>
<body class="book toc2 toc-left">
<div id="toc" class="toc2">
    <form action="search.jsp" class="search">
        <input type="text" name="searchTerms" value="<%= request.getParameter("searchTerms") %>">
        <input type="submit" value="Search">
    </form>
    {toc}
</div>
<div id="content">
    {content}
<%
Path tmpDir = Paths.get(System.getProperty("catalina.home") + "/work/index");
System.out.println(tmpDir);
Files.createDirectories(tmpDir);

Set<String> resourcePaths = getServletContext().getResourcePaths("/index");
for (String resource : resourcePaths) {
	Path file = tmpDir.resolve(Paths.get(resource).getFileName());
	InputStream is = getServletContext().getResourceAsStream(resource);
	Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
}
IndexReader reader = DirectoryReader.open(FSDirectory.open(tmpDir));
IndexSearcher searcher = new IndexSearcher(reader);
Analyzer analyzer = new StandardAnalyzer();
QueryParser parser = new QueryParser("contents", analyzer);
Query query = parser.parse(request.getParameter("searchTerms"));
TopDocs results = searcher.search(query, 100);
ScoreDoc[] hits = results.scoreDocs;
out.println(results.totalHits + " matching documents:<br>");
for (int i = 0; i < hits.length; i++) {
	Document doc = searcher.doc(hits[i].doc);
	String fileName = doc.get("fileName");
	out.println(fileName);
	out.println("<br>");
}
reader.close();
%>
</div>
</body>
</html>