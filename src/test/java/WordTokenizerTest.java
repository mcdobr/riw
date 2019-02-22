import java.util.Map;

import me.mircea.riw.parser.WordTokenizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class WordTokenizerTest {
    @Test
    void countsOccurencesCorrectly() {
        String mockStr = "Ana are mere, pere, bere, iarasi mere si bere. Apoi vine guvernu si ia 24%";

        WordTokenizer tokenizer = new WordTokenizer();
        Map<String, Integer> occurences = tokenizer.countOccurences(mockStr);

        assertEquals(2, occurences.get("mere"));
        assertEquals(2, occurences.get("bere"));
        assertEquals(1, occurences.get("guvernu"));
    }
}
