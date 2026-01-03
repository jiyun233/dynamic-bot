package top.bilibili.config

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import java.io.File
import com.charleskorn.kaml.Yaml

/**
 * 配置管理器
 */
object ConfigManager {
    private val logger = LoggerFactory.getLogger(ConfigManager::class.java)
    private val yaml = Yaml.default

    private val configDir = File("config")
    private val botConfigFile = File(configDir, "bot.yml")

    lateinit var botConfig: BotConfig
        private set

    /** 初始化配置 */
    fun init() {
        if (!configDir.exists()) {
            configDir.mkdirs()
            logger.info("创建配置目录: ${configDir.absolutePath}")
        }

        // 加载或创建 bot 配置
        if (botConfigFile.exists()) {
            try {
                val content = botConfigFile.readText()
                botConfig = yaml.decodeFromString<BotConfig>(content)
                logger.info("成功加载配置文件: ${botConfigFile.name}")
            } catch (e: Exception) {
                logger.error("加载配置文件失败，使用默认配置: ${e.message}", e)
                botConfig = BotConfig()
                saveConfig()
            }
        } else {
            logger.info("配置文件不存在，创建默认配置")
            botConfig = BotConfig()
            saveConfig()
        }

        // 验证配置
        if (!botConfig.napcat.validate()) {
            logger.warn("NapCat 配置无效，请检查配置文件")
        }
    }

    /** 保存配置 */
    fun saveConfig() {
        try {
            val content = yaml.encodeToString(botConfig)
            botConfigFile.writeText(content)
            logger.info("配置已保存到: ${botConfigFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("保存配置失败: ${e.message}", e)
        }
    }

    /** 重新加载配置 */
    fun reload() {
        logger.info("正在重新加载配置...")
        init()
    }
}
