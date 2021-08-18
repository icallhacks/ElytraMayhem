package me.rowanscripts.elytramayhem;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class game extends roundSetup {

    boolean setupInProgress = false;
    boolean gameInProgress = false;

    List<UUID> playersInGame = new ArrayList<>();

    BukkitScheduler scheduler = Bukkit.getScheduler();

    public boolean startGame(Player executor) {

        int amountOfChests = configuration.getInt("amountOfChests");

        Bukkit.getPluginManager().registerEvents(new eventListener(), JavaPlugin.getPlugin(Main.class));
        if (setupInProgress || gameInProgress)
            return false;

        setupInProgress = true;
        for(Player player : Bukkit.getOnlinePlayers()) {
            playersInGame.add(player.getUniqueId());
            player.getInventory().clear();
            player.setGameMode(GameMode.SPECTATOR);
        }

        Bukkit.broadcastMessage(ChatColor.GRAY + "Looking for an appropriate battle location..");
        this.findPossibleBorderLocation(executor);
        Bukkit.broadcastMessage(ChatColor.GREEN + "Successfully found an appropriate battle location!");
        this.teleportPlayersAboveLocation(executor);
        Bukkit.broadcastMessage(ChatColor.GRAY + "Generating loot chests..");
        this.spawnLootChests(executor);
        Bukkit.broadcastMessage(ChatColor.GREEN + "Successfully generated " + amountOfChests + " loot chests!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "Finishing up..");

        setupInProgress = false;
        gameInProgress = true;
        scheduler.scheduleSyncRepeatingTask(JavaPlugin.getPlugin(Main.class), () -> {
            for(Player player : Bukkit.getOnlinePlayers()) {
                if(playersInGame.contains(player.getUniqueId()))
                    player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 5, 1, false, false));
                else
                    player.setGameMode(GameMode.SPECTATOR);
            }

            if(playersInGame.size() == 1){
                playerVictory();
                endGame();
            }
        }, 0, 20);

        return true;
    }

    public void playerVictory(){
        Player playerWhoWon = Bukkit.getPlayer(playersInGame.get(0));
        for(Player player : Bukkit.getOnlinePlayers()){
            player.sendTitle(ChatColor.GOLD + ChatColor.BOLD.toString() + playerWhoWon.getName(), ChatColor.BOLD + "has won!", 10, 100, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 10, 1);
        }
    }

    public void endGame(){
        if (!gameInProgress && !setupInProgress)
            return;

        scheduler.cancelTasks(JavaPlugin.getPlugin(Main.class));
        playersInGame.clear();
        setupInProgress = false;
        gameInProgress = false;
    }

    public class eventListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void blockPlayerMovementDuringSetup(PlayerMoveEvent event){
            if (setupInProgress)
                event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void removePlayerFromListAfterDeath(PlayerDeathEvent event){
            Player player = event.getEntity();
            if (gameInProgress){
                playersInGame.remove(player.getUniqueId());
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void removePlayerFromListAfterLeave(PlayerQuitEvent event){
            Player player = event.getPlayer();
            if (gameInProgress){
                playersInGame.remove(player.getUniqueId());
            }
        }
    }

}