package org.example.oneblock;

import com.destroystokyo.paper.ParticleBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class OneBlockPlugin extends JavaPlugin implements Listener {
    private boolean gameActive = false;
    private List<Player> players = new ArrayList<>();
    private Map<Player, Location> playerBlocks = new HashMap<>();
    private int borderSize;
    private int stage = 1;
    private Map<Integer, List<Material>> stageBlocks = new HashMap<>();
    private Map<Integer, List<EntityType>> stageAnimals = new HashMap<>();
    private BossBar stageProgressBar;
    private int stageProgress = 0;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        initializeStageMaterials();
        initializeStageAnimals();

        getCommand("startoneblock").setExecutor((sender, command, label, args) -> {
            if (sender.hasPermission("oneblock.start")) {
                if (!gameActive) {
                    startCountdown();
                    return true;
                } else {
                    sender.sendMessage(Component.text("Une partie de OneBlock est déjà en cours !").color(NamedTextColor.GREEN));
                    return false;
                }
            } else {
                sender.sendMessage(Component.text("Vous n'avez pas la permission d'utiliser cette commande.").color(NamedTextColor.RED));
                return false;
            }
        });

        getCommand("stoponeblock").setExecutor((sender, command, label, args) -> {
            if (sender.hasPermission("oneblock.stop")) {
                if (gameActive) {
                    endGame();
                    return true;
                } else {
                    sender.sendMessage(Component.text("Aucune partie n'est actuellement en cours !").color(NamedTextColor.GREEN));
                    return false;
                }
            } else {
                sender.sendMessage(Component.text("Vous n'avez pas la permission d'utiliser cette commande.").color(NamedTextColor.RED));
                return false;
            }
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameActive) {
                    updateStageProgress();
                }
            }
        }.runTaskTimer(this, 100, 100);

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
        stageBlocks.put(1, Arrays.asList(
                Material.STONE, Material.DIRT, Material.GRASS_BLOCK, Material.OAK_LOG,
                Material.GRAVEL, Material.OAK_LEAVES, Material.COBBLESTONE, Material.PUMPKIN,
                Material.MELON
        ));
        // Blocs du stage 1 et stage 2
        stageBlocks.put(2, Arrays.asList(
                Material.STONE, Material.DIRT, Material.GRASS_BLOCK, Material.OAK_LOG,
                Material.GRAVEL, Material.OAK_LEAVES, Material.COBBLESTONE, Material.PUMPKIN,
                Material.MELON, Material.IRON_ORE, Material.COAL_ORE, Material.OAK_PLANKS,
                Material.BIRCH_LOG, Material.CLAY, Material.WATER, Material.BIRCH_LEAVES,
                Material.BRICKS, Material.REDSTONE_BLOCK, Material.SANDSTONE
        ));
        // Blocs des stages 1 à 3
        stageBlocks.put(3, Arrays.asList(
                Material.STONE, Material.DIRT, Material.GRASS_BLOCK, Material.OAK_LOG,
                Material.GRAVEL, Material.OAK_LEAVES, Material.COBBLESTONE, Material.PUMPKIN,
                Material.MELON, Material.IRON_ORE, Material.COAL_ORE, Material.OAK_PLANKS,
                Material.BIRCH_LOG, Material.CLAY, Material.WATER, Material.BIRCH_LEAVES,
                Material.BRICKS, Material.REDSTONE_BLOCK, Material.SANDSTONE, Material.GOLD_ORE,
                Material.LAPIS_ORE, Material.SPRUCE_LOG, Material.STONE_BRICKS, Material.GLASS,
                Material.LAVA, Material.SPRUCE_LEAVES, Material.HAY_BLOCK, Material.MOSSY_COBBLESTONE,
                Material.ICE
        ));
        // Blocs des stages 1 à 4
        stageBlocks.put(4, Arrays.asList(
                Material.STONE, Material.DIRT, Material.GRASS_BLOCK, Material.OAK_LOG,
                Material.GRAVEL, Material.OAK_LEAVES, Material.COBBLESTONE, Material.PUMPKIN,
                Material.MELON, Material.IRON_ORE, Material.COAL_ORE, Material.OAK_PLANKS,
                Material.BIRCH_LOG, Material.CLAY, Material.WATER, Material.BIRCH_LEAVES,
                Material.BRICKS, Material.REDSTONE_BLOCK, Material.SANDSTONE, Material.GOLD_ORE,
                Material.LAPIS_ORE, Material.SPRUCE_LOG, Material.STONE_BRICKS, Material.GLASS,
                Material.LAVA, Material.SPRUCE_LEAVES, Material.HAY_BLOCK, Material.MOSSY_COBBLESTONE,
                Material.ICE, Material.DIAMOND_ORE, Material.REDSTONE_ORE, Material.JUNGLE_LOG,
                Material.NETHER_QUARTZ_ORE, Material.OBSIDIAN, Material.QUARTZ_BLOCK, Material.JUNGLE_LEAVES,
                Material.TERRACOTTA, Material.GLOWSTONE, Material.END_STONE
        ));
        // Blocs des stages 1 à 5
        stageBlocks.put(5, Arrays.asList(
                Material.STONE, Material.DIRT, Material.GRASS_BLOCK, Material.OAK_LOG,
                Material.GRAVEL, Material.OAK_LEAVES, Material.COBBLESTONE, Material.PUMPKIN,
                Material.MELON, Material.IRON_ORE, Material.COAL_ORE, Material.OAK_PLANKS,
                Material.BIRCH_LOG, Material.CLAY, Material.WATER, Material.BIRCH_LEAVES,
                Material.BRICKS, Material.REDSTONE_BLOCK, Material.SANDSTONE, Material.GOLD_ORE,
                Material.LAPIS_ORE, Material.SPRUCE_LOG, Material.STONE_BRICKS, Material.GLASS,
                Material.LAVA, Material.SPRUCE_LEAVES, Material.HAY_BLOCK, Material.MOSSY_COBBLESTONE,
                Material.ICE, Material.DIAMOND_ORE, Material.REDSTONE_ORE, Material.JUNGLE_LOG,
                Material.NETHER_QUARTZ_ORE, Material.OBSIDIAN, Material.QUARTZ_BLOCK, Material.JUNGLE_LEAVES,
                Material.TERRACOTTA, Material.GLOWSTONE, Material.END_STONE, Material.EMERALD_ORE,
                Material.ANCIENT_DEBRIS, Material.SHULKER_BOX, Material.NETHER_BRICKS, Material.SEA_LANTERN,
                Material.PRISMARINE, Material.DARK_PRISMARINE, Material.BLUE_ICE, Material.HONEY_BLOCK
        ));
    }


    private void initializeStageAnimals() {
        stageAnimals.put(1, Arrays.asList(EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN));
        stageAnimals.put(2, Arrays.asList(EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN));
        stageAnimals.put(3, Arrays.asList(EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN));
        stageAnimals.put(4, Arrays.asList(EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN));
        stageAnimals.put(5, Arrays.asList(EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN));
    }

    private void startCountdown() {
        int countdownTime = 5;

        Bukkit.broadcast(Component.text("The game will start in " + countdownTime + " seconds!").color(NamedTextColor.GREEN));

        new BukkitRunnable() {
            int timeLeft = countdownTime;

            @Override
            public void run() {
                if (timeLeft > 0) {
                    Bukkit.broadcast(Component.text("Démarrage dans " + timeLeft + " secondes...").color(NamedTextColor.YELLOW));
                    for (Player player : players) {
                        player.sendTitle(
                                PlainTextComponentSerializer.plainText().serialize(Component.text("Démarrage dans " + timeLeft + " secondes...").color(NamedTextColor.YELLOW)),
                                PlainTextComponentSerializer.plainText().serialize(Component.text("Bouge pas mdrr").color(NamedTextColor.RED)),
                                0, 20, 0
                        );
                    }
                    timeLeft--;
                } else {
                    Bukkit.broadcast(Component.text("TOP").color(NamedTextColor.GREEN));
                    for (Player player : players) {
                        player.sendTitle(
                                PlainTextComponentSerializer.plainText().serialize(Component.text("TOP !!!!11!").color(NamedTextColor.GREEN)),
                                PlainTextComponentSerializer.plainText().serialize(Component.text("gl").color(NamedTextColor.GOLD)),
                                0, 20, 0
                        );
                    }
                    initializeGame(); // Start the game
                    cancel(); // Stop the countdown task
                }
            }
        }.runTaskTimer(this, 0, 20); // Run every second
    }

    public void initializeGame() {
        gameActive = true;
        players = new ArrayList<>(Bukkit.getOnlinePlayers());
        stage = 1;
        World world = Bukkit.getWorlds().get(0);

        // Calculate border size based on player count and circle size
        int playerCount = players.size();
        int circleRadius = (int) Math.max(30 * playerCount / (2 * Math.PI), 50);
        borderSize = circleRadius * 2 + 100; // Add some extra space

        double angleIncrement = 360.0 / players.size();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            double angle = Math.toRadians(i * angleIncrement);
            int x = (int) (circleRadius * Math.cos(angle));
            int z = (int) (circleRadius * Math.sin(angle));
            Location blockLoc = new Location(world, x, 100, z);

            CompletableFuture<Chunk> chunkFuture = world.getChunkAtAsync(blockLoc);
            chunkFuture.thenAccept(chunk -> {
                blockLoc.getBlock().setType(Material.GRASS_BLOCK);
                playerBlocks.put(player, blockLoc);
                blockLoc.clone().add(0, -1, 0).getBlock().setType(Material.BEDROCK);
                player.teleportAsync(blockLoc.clone().add(0.5, 1, 0.5));
            });
        }

        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "worldborder set " + borderSize);

        initializeStageProgressBar();
        Bukkit.broadcast(Component.text("L'événement OneBlock a commencé !").color(NamedTextColor.GREEN));
    }

    private void initializeStageProgressBar() {
        stageProgressBar = BossBar.bossBar(
                Component.text("Progression vers le prochain stade: 0%"),
                0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
        );
        for (Player player : players) {
            player.showBossBar(stageProgressBar);
        }
    }

    private void updateStageProgress() {
        stageProgress += 3; // Augmente de 1% toutes les 5 secondes
        if (stageProgress >= 100) {
            advanceStage();
            stageProgress = 0;
        }
        float progress = stageProgress / 100f;
        stageProgressBar.progress(progress);
        stageProgressBar.name(Component.text("Progression vers le prochain stade: " + stageProgress + "%"));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!gameActive) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (playerBlocks.containsValue(block.getLocation())) {
            event.setCancelled(true);

            ItemStack tool = player.getInventory().getItemInMainHand();
            Collection<ItemStack> drops = block.getDrops(tool);

            for (ItemStack drop : drops) {
                block.getWorld().dropItemNaturally(player.getLocation(), drop);
            }

            Material newMaterial = getRandomMaterial();
            block.setType(newMaterial);
            new ParticleBuilder(Particle.EXPLOSION_LARGE)
                    .location(block.getLocation())
                    .count(1)
                    .spawn();
        }
    }

    private Material getRandomMaterial() {
        List<Material> materials = IntStream.rangeClosed(1, stage)
                .mapToObj(stageBlocks::get)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        return materials.get(new Random().nextInt(materials.size()));
    }

    private void advanceStage() {
        if (stage < 5) {
            stage++;
            spawnAnimals();
            Bukkit.broadcast(Component.text("Le stade " + stage + " a commencé ! De nouveaux blocs et animaux sont maintenant disponibles.").color(NamedTextColor.GREEN));
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
                Bukkit.broadcast(Component.text(winner.getName() + " a gagné la partie de OneBlock !").color(NamedTextColor.GOLD));
            } else {
                Bukkit.broadcast(Component.text("Il ne reste plus de joueurs ! La partie est terminée.").color(NamedTextColor.RED));
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
            player.hideBossBar(stageProgressBar);
        }
        Bukkit.broadcast(Component.text("La partie de OneBlock est terminée !").color(NamedTextColor.GREEN));
    }

    private void shrinkBorder() {
        int newSize = borderSize - 100;
        if (newSize > 50) {
            borderSize = newSize;
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "worldborder set " + newSize + " 300");
            Bukkit.broadcast(Component.text("La bordure rétrécit de " + newSize).color(NamedTextColor.GREEN));
        } else {
            Bukkit.broadcast(Component.text("La bordure a arrêté de rétrécir !").color(NamedTextColor.YELLOW));
        }
    }
}