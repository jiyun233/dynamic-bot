package top.bilibili

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

/**
 * 配置和数据管理器
 * 负责加载和保存 BiliConfig 和 BiliData
 */
object BiliConfigManager {
    private val logger = LoggerFactory.getLogger(BiliConfigManager::class.java)

    lateinit var config: BiliConfig
        private set

    lateinit var data: BiliData
        private set

    // 配置文件和数据文件路径
    private val configDir = Paths.get("config").toFile()
    private val dataDir = Paths.get("data").toFile()

    private val configFile = File(configDir, "BiliConfig.yml")
    private val dataFile = File(dataDir, "BiliData.yml")

    // YAML 序列化器（忽略未知属性以支持旧配置文件）
    private val yaml = Yaml(
        configuration = Yaml.default.configuration.copy(
            strictMode = false
        )
    )

    /**
     * 初始化配置和数据
     * 如果文件不存在，则创建默认配置
     */
    fun init() {
        // 确保目录存在
        configDir.mkdirs()
        dataDir.mkdirs()

        // 加载配置
        config = loadConfig()
        logger.info("配置加载完成")

        // 加载数据
        data = loadData()
        logger.info("数据加载完成")
    }

    /**
     * 加载配置文件
     * 如果文件不存在，创建默认配置
     */
    private fun loadConfig(): BiliConfig {
        return try {
            if (configFile.exists()) {
                val content = configFile.readText()
                yaml.decodeFromString(BiliConfig.serializer(), content)
            } else {
                logger.info("配置文件不存在，创建默认配置")
                val defaultConfig = BiliConfig()
                saveConfig(defaultConfig)
                defaultConfig
            }
        } catch (e: Exception) {
            logger.error("加载配置文件失败，使用默认配置", e)
            BiliConfig()
        }
    }

    /**
     * 加载数据文件
     * 如果文件不存在，创建默认数据
     */
    private fun loadData(): BiliData {
        return try {
            if (dataFile.exists()) {
                val content = dataFile.readText()
                yaml.decodeFromString(BiliData.serializer(), content)
            } else {
                logger.info("数据文件不存在，创建默认数据")
                val defaultData = BiliData
                saveData(defaultData)
                defaultData
            }
        } catch (e: Exception) {
            logger.error("加载数据文件失败，使用默认数据", e)
            BiliData
        }
    }

    /**
     * 保存配置到文件
     */
    fun saveConfig(configToSave: BiliConfig = config) {
        try {
            val yamlContent = yaml.encodeToString(BiliConfig.serializer(), configToSave)
            configFile.writeText(yamlContent)
            logger.debug("配置已保存")
        } catch (e: Exception) {
            logger.error("保存配置文件失败", e)
        }
    }

    /**
     * 保存数据到文件
     */
    fun saveData(dataToSave: BiliData = data) {
        try {
            val yamlContent = yaml.encodeToString(BiliData.serializer(), dataToSave)
            dataFile.writeText(yamlContent)
            logger.debug("数据已保存")
        } catch (e: Exception) {
            logger.error("保存数据文件失败", e)
        }
    }

    /**
     * 保存所有配置和数据
     */
    fun saveAll() {
        saveConfig()
        saveData()
    }

    /**
     * 重新加载配置
     */
    fun reloadConfig() {
        config = loadConfig()
        logger.info("配置已重新加载")
    }

    /**
     * 重新加载数据
     */
    fun reloadData() {
        data = loadData()
        logger.info("数据已重新加载")
    }

    /**
     * 重新加载所有配置和数据
     */
    fun reloadAll() {
        reloadConfig()
        reloadData()
    }
}
