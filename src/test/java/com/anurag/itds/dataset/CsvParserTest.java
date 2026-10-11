package com.anurag.itds.dataset;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvParserTest {

    @Test
    void handlesQuotesEscapedQuotesEmbeddedNewlinesAndCrlf() {
        List<List<String>> rows = CsvParser.parse("a,b,c\r\n\"x, y\",\"he said \"\"hi\"\"\",\"line1\nline2\"\r\n1,,3");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(1)).containsExactly("x, y", "he said \"hi\"", "line1\nline2");
        assertThat(rows.get(2)).containsExactly("1", "", "3");
    }

    @Test
    void stripsBomTrimsHeadersSkipsBlankLinesAndPadsShortRows() throws Exception {
        String csv = "\uFEFF name , phone\n\nAsha,123\nRavi\n";
        List<Map<String, String>> records = CsvParser.parseWithHeader(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        assertThat(records).hasSize(2);
        assertThat(records.get(0)).containsEntry("name", "Asha").containsEntry("phone", "123");
        assertThat(records.get(1)).containsEntry("name", "Ravi").containsEntry("phone", "");
    }

    @Test
    void emptyInputGivesNoRecords() throws Exception {
        assertThat(CsvParser.parseWithHeader(new ByteArrayInputStream(new byte[0]))).isEmpty();
    }
}
