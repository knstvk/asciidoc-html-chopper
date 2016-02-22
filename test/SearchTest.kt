import chopper.search.Search
import org.junit.Assert
import org.junit.Test
import java.nio.charset.StandardCharsets

/**
 *
 */
class SearchTest {

    @Test
    fun testSearch() {
        val search = Search(this.javaClass.getResourceAsStream("index.txt"))
        val start = System.currentTimeMillis()
        val results = search.search("information")
        println("Done in ${System.currentTimeMillis() - start}ms")
        Assert.assertFalse(results.isEmpty())
    }

}