//3.File: PlayerVisibilityListener.kt
package winlyps.selectivePlayerVisibility

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.sql.PreparedStatement

class PlayerVisibilityListener(private val plugin: SelectivePlayerVisibility) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // Apply visibility settings to the joining player
        applyVisibilitySettings(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Logic to handle player visibility when a player quits
        plugin.server.onlinePlayers.forEach { player ->
            player.showPlayer(plugin, event.player)
        }
    }

    private fun applyVisibilitySettings(player: org.bukkit.entity.Player) {
        // Retrieve all players and their hidden players from the database
        val visibilityQuery = "SELECT player_uuid, hidden_players FROM player_visibility"
        val visibilityStatement = plugin.dbConnection?.prepareStatement(visibilityQuery)
        val visibilityResultSet = visibilityStatement?.executeQuery()

        while (visibilityResultSet?.next() == true) {
            val playerUuid = visibilityResultSet.getString("player_uuid")
            val hiddenPlayers = visibilityResultSet.getString("hidden_players")
            val onlinePlayer = plugin.server.getPlayer(java.util.UUID.fromString(playerUuid))

            if (onlinePlayer != null) {
                hiddenPlayers.split(",").forEach { uuid ->
                    val hiddenPlayer = plugin.server.getPlayer(java.util.UUID.fromString(uuid))
                    if (hiddenPlayer != null) {
                        onlinePlayer.hidePlayer(plugin, hiddenPlayer)
                    }
                }
            }
        }

        visibilityResultSet?.close()
        visibilityStatement?.close()

        // Retrieve the hidden players for the joining player from the database
        val query = "SELECT hidden_players FROM player_visibility WHERE player_uuid = ?"
        val statement = plugin.dbConnection?.prepareStatement(query)
        statement?.setString(1, player.uniqueId.toString())
        val resultSet = statement?.executeQuery()

        if (resultSet?.next() == true) {
            val hiddenPlayers = resultSet.getString("hidden_players")
            hiddenPlayers.split(",").forEach { uuid ->
                val hiddenPlayer = plugin.server.getPlayer(java.util.UUID.fromString(uuid))
                if (hiddenPlayer != null) {
                    player.hidePlayer(plugin, hiddenPlayer)
                }
            }
        }

        resultSet?.close()
        statement?.close()
    }
}