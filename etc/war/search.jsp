<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.util.*,java.io.*,java.nio.file.*,java.nio.file.attribute.*" %>    
<%@ page import="org.apache.lucene.analysis.*,org.apache.lucene.analysis.standard.StandardAnalyzer" %>    
<%@ page import="org.apache.lucene.document.Document,org.apache.lucene.index.*" %>    
<%@ page import="org.apache.lucene.queryparser.classic.QueryParser,org.apache.lucene.search.*,org.apache.lucene.store.FSDirectory" %>    
<%@ page import="org.apache.commons.lang3.StringEscapeUtils" %>
<%!
	Path indexDir = null;
	
	public void jspInit() { 
		try {
			Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
			indexDir = Files.createTempDirectory(tmpDir, "index");  
			System.out.println("Copying index to " + indexDir);

			Set<String> resourcePaths = getServletContext().getResourcePaths("/WEB-INF/index");
			for (String resource : resourcePaths) {
				Path file = indexDir.resolve(Paths.get(resource).getFileName());
				InputStream is = getServletContext().getResourceAsStream(resource);
				Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
				is.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Error initializing JSP: " + e);
		}
	}

	public void jspDestroy() {
		if (indexDir != null && Files.exists(indexDir)) {
			try {
				Files.walkFileTree(indexDir, new SimpleFileVisitor<Path>() {
			        @Override
			        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			            Files.delete(file);
			            return FileVisitResult.CONTINUE;
			        }

			        @Override
			        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			            if (exc == null) {
			                Files.delete(dir);
			                return FileVisitResult.CONTINUE;
			            } else {
			                throw exc;
			            }
			        }
		        });
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

%>
<%
String searchTerms = request.getParameter("searchTerms");
String htmlSearchTerms = StringEscapeUtils.escapeHtml4(searchTerms);
%>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>{{searchResultsTitle}} <%= htmlSearchTerms %></title>
    <link rel="stylesheet" href="./styles/cuba.css">
    <link rel="stylesheet" href="./styles/chopper.css">
    <link rel="stylesheet" href="./styles/coderay-asciidoctor.css">
</head>
<body class="book toc2 toc-left">
<div id="header">
    <div id="toc" class="toc2">
        <form action="search.jsp" class="search">
            <input type="text" name="searchTerms" value="<%= htmlSearchTerms %>">
            <input type="submit" value="{{search}}">
        </form>
        {{toc}}
    </div>
    <div id="top">
        <div id="title">{{title}}</div>
        <div id="version">{{version}}</div>
        <div id="copyright">{{copyright}}</div>
    </div>
</div>
<div id="content">
    <div id="search-results">
<%
	if (searchTerms == null || searchTerms.trim().equals("")) {
		out.println("<p>{{searchTermIsEmpty}}</p>");
	} else {
		IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir));
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("contents", analyzer);
		Query query = parser.parse(parser.escape(searchTerms).trim() + "*");
		TopDocs results = searcher.search(query, 100);
		ScoreDoc[] hits = results.scoreDocs;
		out.println("<p>" + results.totalHits + " {{searchResultsMsg}} " + htmlSearchTerms + "</p>");
		out.println("</ul>");
		for (int i = 0; i < hits.length; i++) {
			Document doc = searcher.doc(hits[i].doc);
			%>
			<li>
				<a href="<%= doc.get("fileName") %>">
					<span class="path"><%= doc.get("captionPath") %></span>
					<span class="name"><%= doc.get("captionName") %></span>
				</a> 
				<span class="score"><%= hits[i].score %></span>
			</li>
			<%	
		}
		reader.close();
		out.println("</ul>");
	}
%>
	</div>
</div>
</body>
</html>