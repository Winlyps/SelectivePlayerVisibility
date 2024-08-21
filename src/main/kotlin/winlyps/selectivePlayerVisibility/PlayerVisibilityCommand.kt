//2.File: PlayerVisibilityCommand.kt
package winlyps.selectivePlayerVisibility

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.sql.PreparedStatement

class PlayerVisibilityCommand(private val plugin: SelectivePlayerVisibility) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return false

        if (args.size < 2) return false

        val action = args[0]
        val target = args[1]

        when (action.toLowerCase()) {
            "hide" -> {
                if (target.equals("all", ignoreCase = true)) {
                    Bukkit.getOnlinePlayers().forEach { player ->
                        sender.hidePlayer(plugin, player)
                        updateDatabase(sender, player, true)
                    }
                } else {
                    val player = Bukkit.getPlayer(target)
                    if (player != null) {
                        sender.hidePlayer(plugin, player)
                        updateDatabase(sender, player, true)
                    }
                }
            }
            "show" -> {
                if (target.equals("all", ignoreCase = true)) {
                    Bukkit.getOnlinePlayers().forEach { player ->
                        sender.showPlayer(plugin, player)
                        updateDatabase(sender, player, false)
                    }
                } else {
                    val player = Bukkit.getPlayer(target)
                    if (player != null) {
                        sender.showPlayer(plugin, player)
                        updateDatabase(sender, player, false)
                    }
                }
            }
            else -> return false
        }
        return true
    }

    private fun updateDatabase(player: Player, target: Player, hide: Boolean) {
        val query = "SELECT hidden_players FROM player_visibility WHERE player_uuid = ?"
        val statement = plugin.dbConnection?.prepareStatement(query)
        statement?.setString(1, player.uniqueId.toString())
        val resultSet = statement?.executeQuery()

        var hiddenPlayers = ""
        if (resultSet?.next() == true) {
            hiddenPlayers = resultSet.getString("hidden_players")
        }

        resultSet?.close()
        statement?.close()

        val hiddenPlayersList = hiddenPlayers.split(",").toMutableList()

        if (hide) {
            if (!hiddenPlayersList.contains(target.uniqueId.toString())) {
                hiddenPlayersList.add(target.uniqueId.toString())
            }
        } else {
            hiddenPlayersList.remove(target.uniqueId.toString())
        }

        val updatedHiddenPlayers = hiddenPlayersList.joinToString(",")

        val updateQuery = "INSERT OR REPLACE INTO player_visibility (player_uuid, hidden_players) VALUES (?, ?)"
        val updateStatement = plugin.dbConnection?.prepareStatement(updateQuery)
        updateStatement?.setString(1, player.uniqueId.toString())
        updateStatement?.setString(2, updatedHiddenPlayers)
        updateStatement?.executeUpdate()
        updateStatement?.close()
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (sender !is Player) return emptyList()

        if (args.size == 1) {
            return listOf("hide", "show")
        } else if (args.size == 2) {
            val onlinePlayers = Bukkit.getOnlinePlayers().map { it.name }.toMutableList()
            onlinePlayers.remove(sender.name) // Remove the sender's name from the list
            return onlinePlayers + "all"
        }
        return emptyList()
    }
}