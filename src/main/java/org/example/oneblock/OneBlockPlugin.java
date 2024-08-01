package org.example.oneblock;

import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class OneBlockPlugin extends JavaPlugin implements Listener {

    private boolean gameActive = false;
    private List<Player> players = new ArrayList<>();
    private Map<Player, Location> playerBlocks = new HashMap<>();
    private int borderSize = 1000;
    private int stage = 1;
    private Map<Integer, List<Material>> stageBlocks = new HashMap<>();
    private Map<Integer, List<EntityType>> stageAnimals = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        initializeStageMaterials();
        initializeStageAnimals();

        getCommand("startoneblock").setExecutor((sender, command, label, args) -> {
            if (!gameActive) {
                initializeGame();
                return true;
            } else {
                sender.sendMessage(Component.text("OneBlock game is already active!").color(NamedTextColor.GREEN));
                return false;
            }
        });

        getCommand("stoponeblock").setExecutor((sender, command, label, args) -> {
            if (gameActive) {
                endGame();
                return true;
            } else {
                sender.sendMessage(Component.text("No game is currently active!").color(NamedTextColor.GREEN));
                return false;
            }
        });


        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameActive) {
                    advanceStage();
                }
            }
        }.runTaskTimer(this, 3600, 3600);


        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameActive) {
                    shrinkBorder();
                }
            }
        }.runTaskTimer(this, 6000, 6000);
    }

    private void initializeStageMaterials() {
        stageBlocks.put(1, Arrays.asList(Material.STONE, Material.DIRT, Material.GRASS_BLOCK, Material.OAK_LOG, Material.GRAVEL, Material.OAK_LEAVES, Material.COBBLESTONE, Material.PUMPKIN, Material.MELON));
        stageBlocks.put(2, Arrays.asList(Material.IRON_ORE, Material.COAL_ORE, Material.OAK_PLANKS, Material.BIRCH_LOG, Material.CLAY, Material.WATER, Material.BIRCH_LEAVES, Material.BRICKS, Material.REDSTONE_BLOCK, Material.SANDSTONE));
        stageBlocks.put(3, Arrays.asList(Material.GOLD_ORE, Material.LAPIS_ORE, Material.SPRUCE_LOG, Material.STONE_BRICKS, Material.GLASS, Material.LAVA, Material.SPRUCE_LEAVES, Material.HAY_BLOCK, Material.MOSSY_COBBLESTONE, Material.ICE));
        stageBlocks.put(4, Arrays.asList(Material.DIAMOND_ORE, Material.REDSTONE_ORE, Material.JUNGLE_LOG, Material.NETHER_QUARTZ_ORE, Material.OBSIDIAN, Material.QUARTZ_BLOCK, Material.JUNGLE_LEAVES, Material.TERRACOTTA, Material.GLOWSTONE, Material.END_STONE));
        stageBlocks.put(5, Arrays.asList(Material.EMERALD_ORE, Material.ANCIENT_DEBRIS, Material.END_STONE, Material.SHULKER_BOX, Material.NETHER_BRICKS, Material.SEA_LANTERN, Material.PRISMARINE, Material.DARK_PRISMARINE, Material.BLUE_ICE, Material.HONEY_BLOCK));
    }

    private void initializeStageAnimals() {
        stageAnimals.put(1, Arrays.asList(EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN));
        stageAnimals.put(2, Arrays.asList(EntityType.HORSE, EntityType.LLAMA, EntityType.WOLF, EntityType.OCELOT));
        stageAnimals.put(3, Arrays.asList(EntityType.PANDA, EntityType.PARROT, EntityType.FOX, EntityType.TURTLE));
        stageAnimals.put(4, Arrays.asList(EntityType.DOLPHIN, EntityType.BEE, EntityType.CAT, EntityType.RABBIT));
        stageAnimals.put(5, Arrays.asList(EntityType.POLAR_BEAR, EntityType.MUSHROOM_COW, EntityType.SQUID, EntityType.STRIDER));
    }

    public void initializeGame() {
        gameActive = true;
        players = new ArrayList<>(Bukkit.getOnlinePlayers());
        borderSize = 1000;
        stage = 1;

        World world = Bukkit.getWorlds().get(0);
        double angleIncrement = 360.0 / players.size();

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            double angle = Math.toRadians(i * angleIncrement);
            int x = (int) (50 * Math.cos(angle));
            int z = (int) (50 * Math.sin(angle));
            Location blockLoc = new Location(world, x, 100, z);

            CompletableFuture<Chunk> chunkFuture = world.getChunkAtAsync(blockLoc);
            chunkFuture.thenAccept(chunk -> {
                blockLoc.getBlock().setType(Material.GRASS_BLOCK);
                playerBlocks.put(player, blockLoc);
                player.teleportAsync(blockLoc.clone().add(0.5, 1, 0.5));
            });
        }

        Bukkit.broadcast(Component.text("L'event a commencé").color(NamedTextColor.GREEN));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!gameActive) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (playerBlocks.containsValue(block.getLocation())) {
            event.setCancelled(true);

            ItemStack droppedItem = new ItemStack(block.getType());
            block.getWorld().dropItemNaturally(block.getLocation(), droppedItem);

            Material newMaterial = getRandomMaterial();
            block.setType(newMaterial);

            new ParticleBuilder(Particle.EXPLOSION_LARGE)
                    .location(block.getLocation())
                    .count(1)
                    .spawn();
        }
    }

    private Material getRandomMaterial() {
        List<Material> materials = stageBlocks.get(stage);
        return materials.get(new Random().nextInt(materials.size()));
    }

    private void advanceStage() {
        if (stage < 5) {
            stage++;
            spawnAnimals();
            Bukkit.broadcast(Component.text("Stage " + stage + " has begun! New blocks and animals are now available.").color(NamedTextColor.GREEN));
        }
    }

    private void spawnAnimals() {
        List<EntityType> animals = stageAnimals.get(stage);
        for (Map.Entry<Player, Location> entry : playerBlocks.entrySet()) {
            Location spawnLoc = entry.getValue().clone().add(0, 1, 0);
            EntityType animalType = animals.get(new Random().nextInt(animals.size()));
            spawnLoc.getWorld().spawnEntity(spawnLoc, animalType);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gameActive) return;

        Player player = event.getPlayer();
        players.remove(player);
        playerBlocks.remove(player);
        player.setGameMode(GameMode.SPECTATOR);
        checkWinner();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!gameActive) return;

        Player player = event.getPlayer();
        players.remove(player);
        playerBlocks.remove(player);
        checkWinner();
    }

    private void checkWinner() {
        if (players.size() <= 1) {
            if (players.size() == 1) {
                Player winner = players.get(0);
                Bukkit.broadcast(Component.text(winner.getName() + " has won the OneBlock game!").color(NamedTextColor.GOLD));
            } else {
                Bukkit.broadcast(Component.text("No players left! The game has ended.").color(NamedTextColor.RED));
            }
            endGame();
        }
    }

    public void endGame() {
        gameActive = false;
        players.clear();
        playerBlocks.clear();
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "worldborder set 1000");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.teleportAsync(player.getWorld().getSpawnLocation());
        }
        Bukkit.broadcast(Component.text("The OneBlock game has ended!").color(NamedTextColor.GREEN));
    }

    private void shrinkBorder() {
        int newSize = borderSize - 100;
        if (newSize > 50) {
            borderSize = newSize;
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "worldborder set " + newSize + " 300");
        } else {
            Bukkit.broadcast(Component.text("The border has stopped shrinking!").color(NamedTextColor.YELLOW));
        }
    }
}