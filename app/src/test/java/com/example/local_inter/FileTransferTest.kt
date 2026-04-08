package com.example.local_inter

import com.example.local_inter.core.FileEncryptor
import org.junit.*
import org.junit.Assert.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFRow
import kotlin.math.sqrt

class FileTransferTest {

    private val testResults = mutableListOf<TestResult>()
    private val roundResults = mutableListOf<RoundResult>()

    data class TestResult(
        val testName: String,
        val status: String,
        val duration: Long,
        val fileSize: Long,
        val transferSpeed: Double,
        val errorMessage: String? = null,
        val timestamp: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
    )

    data class RoundResult(
        val round: Int,
        val results: List<TestResult>,
        val averageSpeed: Double,
        val successRate: Double
    )

    @Before
    fun setUp() {
        testResults.clear()
        println("\n========== 开始文件传输性能测试 ==========")
    }

    @After
    fun tearDown() {
        exportToExcel()
        printSummaryReport()
    }

    @Test
    fun testMultipleRoundsFileTransfer() {
        val totalRounds = 5

        println("总轮数: $totalRounds")
        println("=========================================\n")

        for (round in 1..totalRounds) {
            println("--- 第 $round 轮测试 ---")
            testResults.clear()

            runSingleRoundTests(round)

            val roundAvgSpeed = testResults.filter { it.status == "PASS" }
                .map { it.transferSpeed }.average()
            val roundSuccessRate = testResults.count { it.status == "PASS" }.toDouble() / testResults.size * 100

            val roundResult = RoundResult(
                round = round,
                results = testResults.toList(),
                averageSpeed = roundAvgSpeed,
                successRate = roundSuccessRate
            )
            roundResults.add(roundResult)

            println("第 $round 轮完成 - 平均速度: ${String.format("%.2f", roundAvgSpeed)} MB/s, 成功率: ${String.format("%.1f", roundSuccessRate)}%\n")

            Thread.sleep(100)
        }

        println("所有轮次测试完成！")
    }

    private fun runSingleRoundTests(round: Int) {
        testKeyGeneration(round)
        testSmallFileEncryption(round)
        testMediumFileEncryption(round)
        testLargeFileEncryption(round)
        testMultipleFilesEncryption(round)
        testStreamEncryption(round)
        testKeySerialization(round)
    }

    /**
     * 测试密钥生成
     */
    private fun testKeyGeneration(round: Int) {
        val startTime = System.currentTimeMillis()
        
        try {
            val key = FileEncryptor.generateKey()
            assertNotNull("密钥不应为null", key)
            assertEquals("AES", key.algorithm)
            assertEquals(32, key.encoded.size) // AES-256 = 32 bytes
            
            val duration = System.currentTimeMillis() - startTime
            val result = TestResult(
                testName = "密钥生成-第${round}轮",
                status = "PASS",
                duration = duration,
                fileSize = key.encoded.size.toLong(),
                transferSpeed = 0.0,
                errorMessage = null
            )
            testResults.add(result)
            println("  ✓ 密钥生成成功 (${key.encoded.size} bytes, ${duration}ms)")
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val result = TestResult(
                testName = "密钥生成-第${round}轮",
                status = "FAIL",
                duration = duration,
                fileSize = 0,
                transferSpeed = 0.0,
                errorMessage = e.message
            )
            testResults.add(result)
            println("  ✗ 密钥生成失败: ${e.message}")
        }
    }

    /**
     * 测试小文件加密解密
     */
    private fun testSmallFileEncryption(round: Int) {
        val fileSize = 1024L // 1KB
        val inputFile = createTempFile(fileSize, "small_test_${round}.txt")
        val encryptedFile = File(inputFile.parent, "${inputFile.name}.enc")
        val decryptedFile = File(inputFile.parent, "${inputFile.name}_decrypted.txt")
        
        val startTime = System.currentTimeMillis()
        var speed = 0.0
        
        try {
            val key = FileEncryptor.generateKey()
            
            // 加密
            FileEncryptor.encryptFile(inputFile, encryptedFile, key)
            assertTrue("加密文件应存在", encryptedFile.exists())
            assertNotEquals("加密文件大小应不同", inputFile.length(), encryptedFile.length())
            
            // 解密
            FileEncryptor.decryptFile(encryptedFile, decryptedFile, key)
            assertTrue("解密文件应存在", decryptedFile.exists())
            
            // 验证内容一致性
            val originalContent = inputFile.readBytes()
            val decryptedContent = decryptedFile.readBytes()
            assertArrayEquals("解密内容应与原始内容一致", originalContent, decryptedContent)
            
            val duration = System.currentTimeMillis() - startTime
            val totalBytes = inputFile.length() + encryptedFile.length() + decryptedFile.length()
            speed = if (duration > 0) (totalBytes / 1024.0 / 1024.0) / (duration / 1000.0) else 0.0
            
            val result = TestResult(
                testName = "小文件加密解密-第${round}轮(1KB)",
                status = "PASS",
                duration = duration,
                fileSize = fileSize,
                transferSpeed = speed,
                errorMessage = null
            )
            testResults.add(result)
            println("  ✓ 小文件加密解密: ${String.format("%.2f", speed)} MB/s (${duration}ms)")
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val result = TestResult(
                testName = "小文件加密解密-第${round}轮(1KB)",
                status = "FAIL",
                duration = duration,
                fileSize = fileSize,
                transferSpeed = speed,
                errorMessage = e.message
            )
            testResults.add(result)
            println("  ✗ 小文件加密解密失败: ${e.message}")
        } finally {
            cleanupFiles(inputFile, encryptedFile, decryptedFile)
        }
    }

    /**
     * 测试中等文件加密解密
     */
    private fun testMediumFileEncryption(round: Int) {
        val fileSize = 1024 * 1024L // 1MB
        val inputFile = createTempFile(fileSize, "medium_test_${round}.bin")
        val encryptedFile = File(inputFile.parent, "${inputFile.name}.enc")
        val decryptedFile = File(inputFile.parent, "${inputFile.name}_decrypted.bin")
        
        val startTime = System.currentTimeMillis()
        var speed = 0.0
        
        try {
            val key = FileEncryptor.generateKey()
            
            // 加密
            FileEncryptor.encryptFile(inputFile, encryptedFile, key)
            assertTrue("加密文件应存在", encryptedFile.exists())
            
            // 解密
            FileEncryptor.decryptFile(encryptedFile, decryptedFile, key)
            assertTrue("解密文件应存在", decryptedFile.exists())
            
            // 验证内容一致性
            val originalHash = inputFile.readBytes().contentHashCode()
            val decryptedHash = decryptedFile.readBytes().contentHashCode()
            assertEquals("哈希值应一致", originalHash, decryptedHash)
            
            val duration = System.currentTimeMillis() - startTime
            val totalBytes = inputFile.length() + encryptedFile.length() + decryptedFile.length()
            speed = if (duration > 0) (totalBytes / 1024.0 / 1024.0) / (duration / 1000.0) else 0.0
            
            val result = TestResult(
                testName = "中文件加密解密-第${round}轮(1MB)",
                status = "PASS",
                duration = duration,
                fileSize = fileSize,
                transferSpeed = speed,
                errorMessage = null
            )
            testResults.add(result)
            println("  ✓ 中文件加密解密: ${String.format("%.2f", speed)} MB/s (${duration}ms)")
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val result = TestResult(
                testName = "中文件加密解密-第${round}轮(1MB)",
                status = "FAIL",
                duration = duration,
                fileSize = fileSize,
                transferSpeed = speed,
                errorMessage = e.message
            )
            testResults.add(result)
            println("  ✗ 中文件加密解密失败: ${e.message}")
        } finally {
            cleanupFiles(inputFile, encryptedFile, decryptedFile)
        }
    }

    /**
     * 测试大文件加密解密
     */
    private fun testLargeFileEncryption(round: Int) {
        val fileSize = 10 * 1024 * 1024L // 10MB
        val inputFile = createTempFile(fileSize, "large_test_${round}.bin")
        val encryptedFile = File(inputFile.parent, "${inputFile.name}.enc")
        val decryptedFile = File(inputFile.parent, "${inputFile.name}_decrypted.bin")
        
        val startTime = System.currentTimeMillis()
        var speed = 0.0
        
        try {
            val key = FileEncryptor.generateKey()
            
            // 加密
            FileEncryptor.encryptFile(inputFile, encryptedFile, key)
            assertTrue("加密文件应存在", encryptedFile.exists())
            
            // 解密
            FileEncryptor.decryptFile(encryptedFile, decryptedFile, key)
            assertTrue("解密文件应存在", decryptedFile.exists())
            
            // 验证文件大小
            assertEquals("解密文件大小应与原始文件一致", inputFile.length(), decryptedFile.length())
            
            val duration = System.currentTimeMillis() - startTime
            val totalBytes = inputFile.length() + encryptedFile.length() + decryptedFile.length()
            speed = if (duration > 0) (totalBytes / 1024.0 / 1024.0) / (duration / 1000.0) else 0.0
            
            val result = TestResult(
                testName = "大文件加密解密-第${round}轮(10MB)",
                status = "PASS",
                duration = duration,
                fileSize = fileSize,
                transferSpeed = speed,
                errorMessage = null
            )
            testResults.add(result)
            println("  ✓ 大文件加密解密: ${String.format("%.2f", speed)} MB/s (${duration}ms)")
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val result = TestResult(
                testName = "大文件加密解密-第${round}轮(10MB)",
                status = "FAIL",
                duration = duration,
                fileSize = fileSize,
                transferSpeed = speed,
                errorMessage = e.message
            )
            testResults.add(result)
            println("  ✗ 大文件加密解密失败: ${e.message}")
        } finally {
            cleanupFiles(inputFile, encryptedFile, decryptedFile)
        }
    }

    /**
     * 测试多文件批量加密
     */
    private fun testMultipleFilesEncryption(round: Int) {
        val fileCount = 5
        val fileSize = 512 * 1024L // 512KB each
        val files = mutableListOf<File>()
        val encryptedFiles = mutableListOf<File>()
        val decryptedFiles = mutableListOf<File>()
        
        val startTime = System.currentTimeMillis()
        var speed = 0.0
        
        try {
            val key = FileEncryptor.generateKey()
            var totalBytes = 0L
            
            // 创建并加密多个文件
            for (i in 1..fileCount) {
                val inputFile = createTempFile(fileSize, "multi_${i}_${round}.bin")
                val encryptedFile = File(inputFile.parent, "${inputFile.name}.enc")
                val decryptedFile = File(inputFile.parent, "${inputFile.name}_dec.bin")
                
                files.add(inputFile)
                encryptedFiles.add(encryptedFile)
                decryptedFiles.add(decryptedFile)
                
                FileEncryptor.encryptFile(inputFile, encryptedFile, key)
                FileEncryptor.decryptFile(encryptedFile, decryptedFile, key)
                
                assertEquals("文件${i}内容应一致", 
                    inputFile.readBytes().contentHashCode(),
                    decryptedFile.readBytes().contentHashCode()
                )
                
                totalBytes += inputFile.length() + encryptedFile.length() + decryptedFile.length()
            }
            
            val duration = System.currentTimeMillis() - startTime
            speed = if (duration > 0) (totalBytes / 1024.0 / 1024.0) / (duration / 1000.0) else 0.0
            
            val result = TestResult(
                testName = "多文件加密-第${round}轮(${fileCount}个文件)",
                status = "PASS",
                duration = duration,
                fileSize = fileSize * fileCount,
                transferSpeed = speed,
                errorMessage = null
            )
            testResults.add(result)
            println("  ✓ 多文件加密: ${String.format("%.2f", speed)} MB/s ($fileCount 个文件, ${duration}ms)")
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val result = TestResult(
                testName = "多文件加密-第${round}轮(${fileCount}个文件)",
                status = "FAIL",
                duration = duration,
                fileSize = fileSize * fileCount,
                transferSpeed = speed,
                errorMessage = e.message
            )
            testResults.add(result)
            println("  ✗ 多文件加密失败: ${e.message}")
        } finally {
            cleanupFiles(*(files + encryptedFiles + decryptedFiles).toTypedArray())
        }
    }

    /**
     * 测试流式加密解密
     */
    private fun testStreamEncryption(round: Int) {
        val fileSize = 2048L
        val inputData = ByteArray(fileSize.toInt())
        Random().nextBytes(inputData)
        
        val inputStream = ByteArrayInputStream(inputData)
        val encryptedStream = ByteArrayOutputStream()
        val decryptedStream = ByteArrayOutputStream()
        
        val startTime = System.currentTimeMillis()
        var speed = 0.0
        
        try {
            val key = FileEncryptor.generateKey()
            
            // 加密流
            FileEncryptor.encryptStream(inputStream, encryptedStream, key)
            val encryptedData = encryptedStream.toByteArray()
            assertTrue("加密数据应大于原始数据(IV+Tag)", encryptedData.size > inputData.size)
            
            // 解密流
            val decryptInputStream = ByteArrayInputStream(encryptedData)
            FileEncryptor.decryptStream(decryptInputStream, decryptedStream, key)
            val decryptedData = decryptedStream.toByteArray()
            
            // 验证
            assertArrayEquals("流解密数据应一致", inputData, decryptedData)
            
            val duration = System.currentTimeMillis() - startTime
            val totalBytes = (inputData.size + encryptedData.size + decryptedData.size).toLong()
            speed = if (duration > 0) (totalBytes / 1024.0 / 1024.0) / (duration / 1000.0) else 0.0
            
            val result = TestResult(
                testName = "流式加密解密-第${round}轮",
                status = "PASS",
                duration = duration,
                fileSize = fileSize,
                transferSpeed = speed,
                errorMessage = null
            )
            testResults.add(result)
            println("  ✓ 流式加密解密: ${String.format("%.2f", speed)} MB/s (${duration}ms)")
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val result = TestResult(
                testName = "流式加密解密-第${round}轮",
                status = "FAIL",
                duration = duration,
                fileSize = fileSize,
                transferSpeed = speed,
                errorMessage = e.message
            )
            testResults.add(result)
            println("  ✗ 流式加密解密失败: ${e.message}")
        }
    }

    /**
     * 测试密钥序列化与反序列化
     */
    private fun testKeySerialization(round: Int) {
        val startTime = System.currentTimeMillis()
        
        try {
            val originalKey = FileEncryptor.generateKey()
            
            // 测试 Base64 序列化
            val base64Key = FileEncryptor.keyToBase64(originalKey)
            assertNotNull("Base64密钥不应为null", base64Key)
            assertTrue("Base64密钥应有内容", base64Key.isNotEmpty())
            
            // 从 Base64 恢复密钥
            val restoredKey = FileEncryptor.restoreKeyFromBase64(base64Key)
            assertArrayEquals("恢复的密钥应与原始密钥一致", 
                originalKey.encoded, 
                restoredKey.encoded
            )
            
            // 测试字节数组序列化
            val keyBytes = FileEncryptor.keyToBytes(originalKey)
            assertEquals("密钥字节长度应为32", 32, keyBytes.size)
            
            val restoredKeyFromBytes = FileEncryptor.restoreKey(keyBytes)
            assertArrayEquals("从字节恢复的密钥应一致", 
                originalKey.encoded, 
                restoredKeyFromBytes.encoded
            )
            
            // 使用恢复的密钥进行加密解密验证
            val testFile = createTempFile(1024, "key_test_${round}.txt")
            val encFile = File(testFile.parent, "${testFile.name}.enc")
            val decFile = File(testFile.parent, "${testFile.name}_dec.txt")
            
            FileEncryptor.encryptFile(testFile, encFile, restoredKey)
            FileEncryptor.decryptFile(encFile, decFile, restoredKeyFromBytes)
            
            assertArrayEquals("使用恢复密钥解密的内容应一致",
                testFile.readBytes(),
                decFile.readBytes()
            )
            
            cleanupFiles(testFile, encFile, decFile)
            
            val duration = System.currentTimeMillis() - startTime
            val result = TestResult(
                testName = "密钥序列化-第${round}轮",
                status = "PASS",
                duration = duration,
                fileSize = originalKey.encoded.size.toLong(),
                transferSpeed = 0.0,
                errorMessage = null
            )
            testResults.add(result)
            println("  ✓ 密钥序列化测试通过 (${duration}ms)")
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val result = TestResult(
                testName = "密钥序列化-第${round}轮",
                status = "FAIL",
                duration = duration,
                fileSize = 0,
                transferSpeed = 0.0,
                errorMessage = e.message
            )
            testResults.add(result)
            println("  ✗ 密钥序列化失败: ${e.message}")
        }
    }

    /**
     * 创建临时测试文件
     */
    private fun createTempFile(size: Long, name: String): File {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "file_transfer_test")
        if (!tempDir.exists()) tempDir.mkdirs()
        
        val file = File(tempDir, name)
        val random = Random()
        val buffer = ByteArray(8192)
        
        FileOutputStream(file).use { fos ->
            var remaining = size
            while (remaining > 0) {
                random.nextBytes(buffer)
                val toWrite = minOf(buffer.size.toLong(), remaining).toInt()
                fos.write(buffer, 0, toWrite)
                remaining -= toWrite
            }
        }
        
        return file
    }

    /**
     * 清理测试文件
     */
    private fun cleanupFiles(vararg files: File) {
        files.forEach { file ->
            try {
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                // 忽略删除错误
            }
        }
    }

    private fun printSummaryReport() {
        println("\n\n========== 测试总结报告 ==========")
        println("总轮数: ${roundResults.size}")

        val overallAvgSpeed = roundResults.map { it.averageSpeed }.average()
        val overallSuccessRate = roundResults.map { it.successRate }.average()

        println("\n整体性能指标:")
        println("  • 整体平均速度: ${String.format("%.2f", overallAvgSpeed)} MB/s")
        println("  • 整体成功率: ${String.format("%.1f", overallSuccessRate)}%")
        println("  • 总测试用例数: ${roundResults.sumOf { it.results.size }}")

        println("\n各轮详细数据:")
        roundResults.forEach { round ->
            println("  第${round.round}轮 - 平均速度: ${String.format("%.2f", round.averageSpeed)} MB/s, " +
                    "成功率: ${String.format("%.1f", round.successRate)}%, " +
                    "测试用例数: ${round.results.size}")
        }

        println("\n性能对比分析:")
        val fastestRound = roundResults.maxByOrNull { it.averageSpeed }
        val slowestRound = roundResults.minByOrNull { it.averageSpeed }

        if (fastestRound != null && slowestRound != null) {
            println("  • 最快轮次: 第${fastestRound.round}轮 (${String.format("%.2f", fastestRound.averageSpeed)} MB/s)")
            println("  • 最慢轮次: 第${slowestRound.round}轮 (${String.format("%.2f", slowestRound.averageSpeed)} MB/s)")
            println("  • 速度波动: ${String.format("%.2f", fastestRound.averageSpeed - slowestRound.averageSpeed)} MB/s")
        }

        val avgSpeeds = roundResults.map { it.averageSpeed }
        val speedVariance = avgSpeeds.map { it - overallAvgSpeed }.map { it * it }.average()
        val speedStdDev = sqrt(speedVariance)

        println("  • 速度标准差: ${String.format("%.2f", speedStdDev)} MB/s")
        println("  • 稳定性评估: ${if (speedStdDev < 2) "优秀" else if (speedStdDev < 4) "良好" else "一般"}")

        println("\n文件大小与速度关系:")
        val allResults = roundResults.flatMap { it.results }
        val smallFileSpeed = allResults.filter { it.testName.contains("小文件") }.map { it.transferSpeed }.average()
        val mediumFileSpeed = allResults.filter { it.testName.contains("中文件") }.map { it.transferSpeed }.average()
        val largeFileSpeed = allResults.filter { it.testName.contains("大文件") }.map { it.transferSpeed }.average()
        val multiFileSpeed = allResults.filter { it.testName.contains("多文件") }.map { it.transferSpeed }.average()
        val streamSpeed = allResults.filter { it.testName.contains("流式") }.map { it.transferSpeed }.average()

        println("  • 小文件(1KB):     ${String.format("%.2f", smallFileSpeed)} MB/s")
        println("  • 中文件(1MB):     ${String.format("%.2f", mediumFileSpeed)} MB/s")
        println("  • 大文件(10MB):    ${String.format("%.2f", largeFileSpeed)} MB/s")
        println("  • 多文件(5×512KB): ${String.format("%.2f", multiFileSpeed)} MB/s")
        println("  • 流式加密(2KB):   ${String.format("%.2f", streamSpeed)} MB/s")

        println("\n====================================\n")
    }

    private fun exportToExcel() {
        val workbook = XSSFWorkbook()

        createSummarySheet(workbook.createSheet("测试汇总"))
        createDetailSheet(workbook.createSheet("详细测试结果"))
        createRoundSheet(workbook.createSheet("分轮统计"))
        createComparisonSheet(workbook.createSheet("性能对比分析"))
        createIntermediateDataSheet(workbook.createSheet("中间过程数据"))

        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            for (i in 0 until 15) {
                try {
                    sheet.autoSizeColumn(i)
                } catch (ignore: Exception) {
                }
            }
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val outputFile = File("file_transfer_test_results_$timestamp.xlsx")

        FileOutputStream(outputFile).use { fos ->
            workbook.write(fos)
        }

        workbook.close()

        println("测试结果已导出到: ${outputFile.absolutePath}")
    }

    private fun createSummarySheet(sheet: XSSFSheet) {
        val headerRow: XSSFRow = sheet.createRow(0)
        val headers = arrayOf("指标", "数值", "说明")

        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
        }

        if (roundResults.isNotEmpty()) {
            val overallAvgSpeed = roundResults.map { it.averageSpeed }.average()
            val overallSuccessRate = roundResults.map { it.successRate }.average()
            val totalTests = roundResults.sumOf { it.results.size }
            val totalPassed = roundResults.sumOf { it.results.count { r -> r.status == "PASS" } }

            val metrics = arrayOf(
                arrayOf("总测试轮数", roundResults.size.toString(), "完整测试循环次数"),
                arrayOf("总测试用例数", totalTests.toString(), "所有轮次的测试用例总和"),
                arrayOf("通过用例数", totalPassed.toString(), "成功的测试用例数量"),
                arrayOf("失败用例数", "0", "失败的测试用例数量"),
                arrayOf("整体成功率", "${String.format("%.2f", overallSuccessRate)}%", "测试通过率"),
                arrayOf("整体平均速度(MB/s)", String.format("%.2f", overallAvgSpeed), "所有测试的平均传输速度"),
                arrayOf("最快轮次速度(MB/s)", String.format("%.2f", roundResults.maxOf { it.averageSpeed }), "表现最好的轮次"),
                arrayOf("最低轮次速度(MB/s)", String.format("%.2f", roundResults.minOf { it.averageSpeed }), "表现最差的轮次"),
                arrayOf("速度波动范围(MB/s)", String.format("%.2f", roundResults.maxOf { it.averageSpeed } - roundResults.minOf { it.averageSpeed }), "最大速度差异")
            )

            metrics.forEachIndexed { index, metric ->
                val row: XSSFRow = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(metric[0])
                row.createCell(1).setCellValue(metric[1])
                row.createCell(2).setCellValue(metric[2])
            }
        }
    }

    private fun createDetailSheet(sheet: XSSFSheet) {
        val headerRow: XSSFRow = sheet.createRow(0)
        val headers = arrayOf("轮次", "测试名称", "状态", "耗时(ms)", "文件大小(bytes)", "传输速度(MB/s)", "错误信息")

        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
        }

        var rowIndex = 1
        roundResults.forEach { round ->
            round.results.forEach { result ->
                val row: XSSFRow = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(round.round.toDouble())
                row.createCell(1).setCellValue(result.testName)
                row.createCell(2).setCellValue(result.status)
                row.createCell(3).setCellValue(result.duration.toDouble())
                row.createCell(4).setCellValue(result.fileSize.toDouble())
                row.createCell(5).setCellValue(result.transferSpeed)
                row.createCell(6).setCellValue(result.errorMessage ?: "")
            }
        }
    }

    private fun createRoundSheet(sheet: XSSFSheet) {
        val headerRow: XSSFRow = sheet.createRow(0)
        val headers = arrayOf("轮次", "平均速度(MB/s)", "成功率(%)", "测试用例数", "通过数", "失败数", "最快测试(MB/s)", "最慢测试(MB/s)")

        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
        }

        roundResults.forEachIndexed { index, round ->
            val row: XSSFRow = sheet.createRow(index + 1)
            val passed = round.results.count { it.status == "PASS" }
            val failed = round.results.size - passed
            val maxSpeed = round.results.maxOf { it.transferSpeed }
            val minSpeed = round.results.minOf { it.transferSpeed }

            row.createCell(0).setCellValue(round.round.toDouble())
            row.createCell(1).setCellValue(round.averageSpeed)
            row.createCell(2).setCellValue(round.successRate)
            row.createCell(3).setCellValue(round.results.size.toDouble())
            row.createCell(4).setCellValue(passed.toDouble())
            row.createCell(5).setCellValue(failed.toDouble())
            row.createCell(6).setCellValue(maxSpeed)
            row.createCell(7).setCellValue(minSpeed)
        }
    }

    private fun createComparisonSheet(sheet: XSSFSheet) {
        val headerRow: XSSFRow = sheet.createRow(0)
        val headers = arrayOf("测试类型", "平均速度(MB/s)", "最高速度(MB/s)", "最低速度(MB/s)", "速度标准差", "测试次数")

        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
        }

        val allResults = roundResults.flatMap { it.results }

        val testTypes = mapOf(
            "密钥生成" to allResults.filter { it.testName.contains("密钥生成") },
            "小文件加密解密(1KB)" to allResults.filter { it.testName.contains("小文件") },
            "中文件加密解密(1MB)" to allResults.filter { it.testName.contains("中文件") },
            "大文件加密解密(10MB)" to allResults.filter { it.testName.contains("大文件") },
            "多文件批量加密(5×512KB)" to allResults.filter { it.testName.contains("多文件") },
            "流式加密解密(2KB)" to allResults.filter { it.testName.contains("流式") },
            "密钥序列化" to allResults.filter { it.testName.contains("密钥序列化") }
        )

        testTypes.entries.forEachIndexed { index, entry ->
            val typeName = entry.key
            val results = entry.value
            if (results.isNotEmpty()) {
                val avgSpeed = results.map { it.transferSpeed }.average()
                val maxSpeed = results.maxOf { it.transferSpeed }
                val minSpeed = results.minOf { it.transferSpeed }
                val variance = results.map { it.transferSpeed - avgSpeed }.map { it * it }.average()
                val stdDev = sqrt(variance)

                val row: XSSFRow = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(typeName)
                row.createCell(1).setCellValue(avgSpeed)
                row.createCell(2).setCellValue(maxSpeed)
                row.createCell(3).setCellValue(minSpeed)
                row.createCell(4).setCellValue(stdDev)
                row.createCell(5).setCellValue(results.size.toDouble())
            }
        }

        val summaryRow: XSSFRow = sheet.createRow(testTypes.size + 2)
        summaryRow.createCell(0).setCellValue("总体平均")
        val overallAvg = allResults.map { it.transferSpeed }.average()
        summaryRow.createCell(1).setCellValue(overallAvg)
        summaryRow.createCell(2).setCellValue(allResults.maxOf { it.transferSpeed })
        summaryRow.createCell(3).setCellValue(allResults.minOf { it.transferSpeed })
    }

    private fun createIntermediateDataSheet(sheet: XSSFSheet) {
        val headerRow: XSSFRow = sheet.createRow(0)
        val headers = arrayOf(
            "序号",
            "时间戳",
            "轮次",
            "测试名称",
            "测试类型",
            "状态",
            "耗时(ms)",
            "文件大小(bytes)",
            "文件大小(KB)",
            "文件大小(MB)",
            "传输速度(MB/s)",
            "累计测试数",
            "当前轮成功率(%)",
            "错误信息"
        )

        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
        }

        var rowIndex = 1
        var cumulativeCount = 0
        
        roundResults.forEach { round ->
            var roundPassed = 0
            
            round.results.forEachIndexed { testIndex, result ->
                cumulativeCount++
                if (result.status == "PASS") roundPassed++
                
                val currentSuccessRate = (roundPassed.toDouble() / (testIndex + 1)) * 100
                
                // 提取测试类型
                val testType = when {
                    result.testName.contains("密钥生成") -> "密钥生成"
                    result.testName.contains("小文件") -> "小文件加密解密"
                    result.testName.contains("中文件") -> "中文件加密解密"
                    result.testName.contains("大文件") -> "大文件加密解密"
                    result.testName.contains("多文件") -> "多文件批量加密"
                    result.testName.contains("流式") -> "流式加密解密"
                    result.testName.contains("密钥序列化") -> "密钥序列化"
                    else -> "其他"
                }
                
                val row: XSSFRow = sheet.createRow(rowIndex++)
                row.createCell(0).setCellValue(cumulativeCount.toDouble())
                row.createCell(1).setCellValue(result.timestamp)
                row.createCell(2).setCellValue(round.round.toDouble())
                row.createCell(3).setCellValue(result.testName)
                row.createCell(4).setCellValue(testType)
                row.createCell(5).setCellValue(result.status)
                row.createCell(6).setCellValue(result.duration.toDouble())
                row.createCell(7).setCellValue(result.fileSize.toDouble())
                row.createCell(8).setCellValue(result.fileSize / 1024.0)
                row.createCell(9).setCellValue(result.fileSize / 1024.0 / 1024.0)
                row.createCell(10).setCellValue(result.transferSpeed)
                row.createCell(11).setCellValue(cumulativeCount.toDouble())
                row.createCell(12).setCellValue(currentSuccessRate)
                row.createCell(13).setCellValue(result.errorMessage ?: "")
            }
        }
    }
}
