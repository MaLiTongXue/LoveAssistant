package com.loveassistant.tools;

import cn.hutool.core.io.FileUtil;
import com.loveassistant.constant.FileConstant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件读写工具单测：不依赖 Spring 容器、不依赖网络，直接校验读写结果。
 */
class FileOperationToolTest {

    private final FileOperationTool fileOperationTool = new FileOperationTool();

    private final String fileName = "file-operation-tool-test.txt";

    private final String filePath = FileConstant.FILE_SAVE_DIR + "/file/" + fileName;

    @AfterEach
    void cleanUp() {
        try {
            FileUtil.del(filePath);
        } catch (Exception ignored) {
            // 清理失败不影响断言结果
        }
    }

    @Test
    @DisplayName("写入后再读取，读到的内容必须与写入内容完全一致")
    void writeThenReadShouldReturnSameContent() {
        String content = "恋爱助手文件读写测试\n第二行内容";

        String writeResult = fileOperationTool.writeFile(fileName, content);
        assertTrue(writeResult.startsWith("File written successfully to:"), writeResult);
        assertTrue(FileUtil.exist(filePath), "写入成功后文件应真实存在：" + filePath);

        String readResult = fileOperationTool.readFile(fileName);
        assertEquals(content, readResult, "读回的内容应与写入内容一致");
    }

    @Test
    @DisplayName("写入会覆盖已有内容，不会残留旧数据")
    void writeShouldOverwriteExistingContent() {
        fileOperationTool.writeFile(fileName, "旧内容");
        fileOperationTool.writeFile(fileName, "新内容");

        assertEquals("新内容", fileOperationTool.readFile(fileName));
    }

    @Test
    @DisplayName("读取不存在的文件应返回错误信息，而不是抛异常")
    void readMissingFileShouldReturnErrorMessage() {
        String result = fileOperationTool.readFile("no-such-file-" + System.nanoTime() + ".txt");

        assertTrue(result.startsWith("Error reading file:"), result);
    }
}
