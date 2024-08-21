//1.File: SelectivePlayerVisibility.kt
package winlyps.selectivePlayerVisibility

import org.bukkit.plugin.java.JavaPlugin
import java.sql.Connection
import java.sql.DriverManager
import java.io.File

class SelectivePlayerVisibility : JavaPlugin() {

    var dbConnection: Connection? = null

    override fun onEnable() {
        // Ensure the plugin's data folder exists
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        // Initialize database
        try {
            initializeDatabase()
        } catch (e: Exception) {
            logger.severe("Failed to initialize database: ${e.message}")
            this.server.pluginManager.disablePlugin(this)
            return
        }

        // Register command and listener
        getCommand("playervisibility")?.setExecutor(PlayerVisibilityCommand(this))
        server.pluginManager.registerEvents(PlayerVisibilityListener(this), this)
    }

    override fun onDisable() {
        // Close database connection if it was initialized
        dbConnection?.close()
    }

    private fun initializeDatabase() {
        val dbFile = File(dataFolder, "player_visibility.db")
        val dbUrl = "jdbc:sqlite:${dbFile.absolutePath}"
        dbConnection = DriverManager.getConnection(dbUrl)

        // Create table if not exists
        val statement = dbConnection?.createStatement()
        statement?.executeUpdate("""
            CREATE TABLE IF NOT EXISTS player_visibility (
                player_uuid TEXT PRIMARY KEY,
                hidden_players TEXT
            )
        """.trimIndent())
        statement?.close()
    }
}
