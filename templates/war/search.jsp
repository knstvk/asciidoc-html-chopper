<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
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
    <link rel="stylesheet" href="./styles/chopper.css">
    <link rel="stylesheet" href="./styles/coderay-asciidoctor.css">
    <style>
    </style>
</head>
<body class="book toc2 toc-left">
<%!
	Path indexDir = null;
	
	public void jspInit() { 
		try {
			Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
			indexDir = Files.createTempDirectory(tmpDir, "index");  
			System.out.println("Copying index to " + indexDir);

			Set<String> resourcePaths = getServletContext().getResourcePaths("/index");
			for (String resource : resourcePaths) {
				Path file = indexDir.resolve(Paths.get(resource).getFileName());
				InputStream is = getServletContext().getResourceAsStream(resource);
				Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error initializing JSP: " + e);
		}
	}

%>
<div id="toc" class="toc2">
    <form action="search.jsp" class="search">
        <input type="text" name="searchTerms" value="<%= request.getParameter("searchTerms") %>">
        <input type="submit" value="Search">
    </form>
    {toc}
</div>
<div id="top">
    <div id="title">CUBA Platform. Developer’s Manual</div>
    <div id="version">Version 6.0</div>
    <div id="copyright">Copyright (c) 2016 <a href="http://www.haulmont.com" target="_blank">Haulmont</a></div>
</div>
<div id="content">
    <div id="search-results">
<%
	IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir));
	IndexSearcher searcher = new IndexSearcher(reader);
	Analyzer analyzer = new StandardAnalyzer();
	QueryParser parser = new QueryParser("contents", analyzer);
	Query query = parser.parse(request.getParameter("searchTerms"));
	TopDocs results = searcher.search(query, 100);
	ScoreDoc[] hits = results.scoreDocs;
	out.println("<p>" + results.totalHits + " results for: " + request.getParameter("searchTerms") + "</p>");
	for (int i = 0; i < hits.length; i++) {
		Document doc = searcher.doc(hits[i].doc);
		String fileName = doc.get("fileName");
		String caption = doc.get("caption");
		%>
		<p><a href="<%= fileName %>"><%= caption %></a> <span class="score"><%= hits[i].score %></span></p>
		<%	
	}
	reader.close();
%>
	</div>
</div>
</body>
</html>