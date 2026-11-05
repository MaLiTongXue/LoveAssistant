package com.loveassistant.tools;

import cn.hutool.core.io.FileUtil;
import com.loveassistant.constant.FileConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDF 生成工具单测：校验文件真实落盘，且内容确实是一个 PDF（而不是只判断返回值非空）。
 */
class PDFGenerationToolTest {

    private final PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();

    private final String fileName = "pdf-generation-tool-test.pdf";

    private final String filePath = FileConstant.FILE_SAVE_DIR + "/pdf/" + fileName;

    @AfterEach
    void cleanUp() {
        try {
            FileUtil.del(filePath);
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("生成的 PDF 必须落盘、非空，并以 %PDF 文件头开头")
    void generatePDFShouldWriteRealPdfFile() throws Exception {
        String content = "七夕约会计划：餐厅预订、活动流程、礼物清单";

        String result = pdfGenerationTool.generatePDF(fileName, content);
        assertTrue(result.startsWith("PDF generated successfully to:"), result);

        File pdf = new File(filePath);
        assertTrue(pdf.exists(), "PDF 文件应真实存在：" + filePath);
        assertTrue(pdf.length() > 200, "PDF 文件不应为空，实际大小：" + pdf.length() + " 字节");

        byte[] header = new byte[4];
        try (InputStream in = new FileInputStream(pdf)) {
            int read = in.read(header);
            assertTrue(read == 4, "应能读出 4 字节文件头");
        }
        assertTrue("%PDF".equals(new String(header, StandardCharsets.US_ASCII)),
                "文件应以 %PDF 开头，实际为：" + new String(header, StandardCharsets.US_ASCII));
    }

    @Test
    @DisplayName("文件名非法时应返回错误信息，而不是抛异常")
    void generatePDFWithIllegalFileNameShouldReturnErrorMessage() {
        String result = pdfGenerationTool.generatePDF("bad\0name.pdf", "content");

        assertTrue(result.startsWith("Error generating PDF:"), result);
    }
}
