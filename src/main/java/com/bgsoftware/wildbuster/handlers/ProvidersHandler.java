package com.bgsoftware.wildbuster.handlers;

import com.bgsoftware.wildbuster.WildBusterPlugin;
import com.bgsoftware.wildbuster.api.objects.BlockData;
import com.bgsoftware.wildbuster.hooks.BlockBreakProvider;
import com.bgsoftware.wildbuster.hooks.BlockBreakProvider_GriefPrevention;
import com.bgsoftware.wildbuster.hooks.BlockBreakProvider_Lands;
import com.bgsoftware.wildbuster.hooks.BlockBreakProvider_RedProtect;
import com.bgsoftware.wildbuster.hooks.BlockBreakProvider_WorldGuard;
import com.bgsoftware.wildbuster.hooks.FactionsProvider;
import com.bgsoftware.wildbuster.hooks.FactionsProvider_Default;
import com.bgsoftware.wildbuster.hooks.FactionsProvider_FactionsUUID;
import com.bgsoftware.wildbuster.hooks.FactionsProvider_FactionsX;
import com.bgsoftware.wildbuster.hooks.FactionsProvider_MassiveCore;
import com.bgsoftware.wildbuster.hooks.listener.IBusterBlockListener;
import com.bgsoftware.wildbuster.utils.threads.Executor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class ProvidersHandler {

    private final WildBusterPlugin plugin;

    private final Set<BlockBreakProvider> blockBreakProviders = new HashSet<>();
    private final Set<IBusterBlockListener> busterBlockListeners = new HashSet<>();
    private FactionsProvider factionsProvider;

    public ProvidersHandler(WildBusterPlugin plugin) {
        this.plugin = plugin;

        Executor.sync(() -> {
            WildBusterPlugin.log("Loading providers started...");
            long startTime = System.currentTimeMillis();

            WildBusterPlugin.log(" - Using " + plugin.getNMSAdapter().getVersion() + " adapter.");

            loadGeneralHooks();
            loadFactionProvider();
            loadBlockBreakProviders();

            WildBusterPlugin.log("Loading providers done (Took " + (System.currentTimeMillis() - startTime) + "ms)");
        }, 1L);

    }

    public FactionsProvider getFactionsProvider() {
        return factionsProvider;
    }

    public boolean canBuild(OfflinePlayer player, Block block) {
        return blockBreakProviders.stream().allMatch(p -> p.canBuild(player, block));
    }

    public void registerBusterBlockListener(IBusterBlockListener busterBlockListener) {
        this.busterBlockListeners.add(busterBlockListener);
    }

    public void notifyBusterBlockListeners(OfflinePlayer offlinePlayer, Location location, BlockData blockData,
                                           IBusterBlockListener.Action action) {
        this.busterBlockListeners.forEach(busterBlockListener -> busterBlockListener
                .recordBlockAction(offlinePlayer, location, blockData, action));
    }

    private void loadFactionProvider() {
        if (Bukkit.getPluginManager().isPluginEnabled("FactionsX")) {
            factionsProvider = new FactionsProvider_FactionsX();
            WildBusterPlugin.log(" - Using FactionsX as FactionsProvider.");
        } else if (Bukkit.getPluginManager().isPluginEnabled("Factions")) {
            if (!Bukkit.getPluginManager().getPlugin("Factions").getDescription().getAuthors().contains("drtshock")) {
                factionsProvider = new FactionsProvider_MassiveCore();
                WildBusterPlugin.log(" - Using MassiveCore as FactionsProvider.");
            } else {
                factionsProvider = new FactionsProvider_FactionsUUID();
                WildBusterPlugin.log(" - Using FactionsUUID as FactionsProvider.");
            }
        } else {
            factionsProvider = new FactionsProvider_Default();
            WildBusterPlugin.log(" - Couldn't find any factions providers, using default one.");
        }
    }

    private void loadBlockBreakProviders() {
        blockBreakProviders.clear();

        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            blockBreakProviders.add(new BlockBreakProvider_WorldGuard());
            WildBusterPlugin.log(" - Using WorldGuard as BlockBreakProvider.");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            blockBreakProviders.add(new BlockBreakProvider_GriefPrevention());
            WildBusterPlugin.log(" - Using GriefPrevention as BlockBreakProvider.");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("Lands")) {
            blockBreakProviders.add(new BlockBreakProvider_Lands());
            WildBusterPlugin.log(" - Using Lands as BlockBreakProvider.");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("RedProtect")) {
            blockBreakProviders.add(new BlockBreakProvider_RedProtect());
            WildBusterPlugin.log(" - Using RedProtect as BlockBreakProvider.");
        }
    }

    private void loadGeneralHooks() {
        if (Bukkit.getPluginManager().isPluginEnabled("CoreProtect")) {
            registerHook("CoreProtectHook");
        }
    }

    private void registerHook(String className) {
        try {
            Class<?> clazz = Class.forName("com.bgsoftware.superiorskyblock.hooks.support." + className);
            Method registerMethod = clazz.getMethod("register", WildBusterPlugin.class);
            registerMethod.invoke(null, plugin);
        } catch (Exception ignored) {
        }
    }

    private <T> Optional<T> createInstance(String className) {
        try {
            Class<?> clazz = Class.forName("com.bgsoftware.superiorskyblock.hooks.provider." + className);
            try {
                Method compatibleMethod = clazz.getDeclaredMethod("isCompatible");
                if (!(boolean) compatibleMethod.invoke(null))
                    return Optional.empty();
            } catch (Exception ignored) {
            }

            try {
                Constructor<?> constructor = clazz.getConstructor(WildBusterPlugin.class);
                // noinspection unchecked
                return Optional.of((T) constructor.newInstance(plugin));
            } catch (Exception error) {
                // noinspection unchecked
                return Optional.of((T) clazz.newInstance());
            }
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        } catch (Exception error) {
            error.printStackTrace();
            return Optional.empty();
        }
    }

}
