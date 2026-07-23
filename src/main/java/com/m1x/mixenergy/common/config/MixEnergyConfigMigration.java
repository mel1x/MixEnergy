package com.m1x.mixenergy.common.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

final class MixEnergyConfigMigration {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String LEGACY_BREAKING_KEY =
            "general.energyCostForBreakingBlocks";
    private static final String LEGACY_ATTACKS_KEY =
            "general.energyCostForAttacks";
    private static final String LEGACY_POSITION_KEY =
            "general.energyBarPosition";
    private static final String LEGACY_REGEN_COOLDOWN_KEY =
            "general.energyRegenCooldown";
    private static final String REGEN_COOLDOWN_TICKS_KEY =
            "general.energyRegenCooldownTicks";

    private MixEnergyConfigMigration() {
    }

    static void migrate() {
        Path commonPath = FMLPaths.CONFIGDIR.get().resolve(MixEnergyConfig.COMMON_FILE_NAME);
        if (!Files.isRegularFile(commonPath)) {
            return;
        }

        try (CommentedFileConfig commonConfig = CommentedFileConfig.builder(commonPath).sync().build()) {
            commonConfig.load();
            boolean changed = migrateGameplayValues(commonConfig);
            changed |= migrateRegenCooldown(commonConfig);

            if (FMLEnvironment.dist == Dist.CLIENT && commonConfig.contains(LEGACY_POSITION_KEY)) {
                Object legacyPosition = commonConfig.get(LEGACY_POSITION_KEY);
                migrateClientPosition(legacyPosition);
            }

            changed |= removeIfPresent(commonConfig, LEGACY_BREAKING_KEY);
            changed |= removeIfPresent(commonConfig, LEGACY_ATTACKS_KEY);
            changed |= removeIfPresent(commonConfig, LEGACY_POSITION_KEY);
            changed |= removeIfPresent(commonConfig, LEGACY_REGEN_COOLDOWN_KEY);

            if (changed) {
                commonConfig.save();
                LOGGER.info("Migrated legacy MixEnergy gameplay settings to the new config schema");
            }
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Could not migrate the legacy MixEnergy config; Forge will validate it normally",
                    exception
            );
        }
    }

    private static boolean migrateGameplayValues(CommentedFileConfig config) {
        boolean changed = false;

        if (config.contains(LEGACY_BREAKING_KEY)) {
            boolean legacyValue = Boolean.TRUE.equals(config.get(LEGACY_BREAKING_KEY));
            changed |= setIfMissing(config, "energy_sources.breakingBlocks", legacyValue);
            changed |= setIfMissing(config, "energy_sources.placingBlocks", legacyValue);
        }

        if (config.contains(LEGACY_ATTACKS_KEY)) {
            boolean legacyValue = Boolean.TRUE.equals(config.get(LEGACY_ATTACKS_KEY));
            changed |= setIfMissing(config, "energy_sources.attacks", legacyValue);
        }

        return changed;
    }

    private static void migrateClientPosition(Object legacyPosition) {
        if (!(legacyPosition instanceof String position)) {
            return;
        }

        Path clientPath = FMLPaths.CONFIGDIR.get().resolve(MixEnergyConfig.CLIENT_FILE_NAME);
        try (CommentedFileConfig clientConfig = CommentedFileConfig.builder(clientPath).sync().build()) {
            if (Files.isRegularFile(clientPath)) {
                clientConfig.load();
            }

            if (!clientConfig.contains("hud.energyBarPosition")) {
                clientConfig.set("hud.energyBarPosition", position);
                clientConfig.save();
                LOGGER.info("Migrated the legacy MixEnergy HUD position to the client config");
            }
        }
    }

    private static boolean migrateRegenCooldown(CommentedFileConfig config) {
        if (config.contains(REGEN_COOLDOWN_TICKS_KEY)
                || !config.contains(LEGACY_REGEN_COOLDOWN_KEY)) {
            return false;
        }

        Object legacyValue = config.get(LEGACY_REGEN_COOLDOWN_KEY);
        if (!(legacyValue instanceof Number milliseconds)) {
            return false;
        }

        int ticks = Math.max(0, Math.min(200, Math.round(milliseconds.floatValue() / 50.0f)));
        config.set(REGEN_COOLDOWN_TICKS_KEY, ticks);
        return true;
    }

    private static boolean setIfMissing(
            CommentedFileConfig config,
            String path,
            boolean value
    ) {
        if (config.contains(path)) {
            return false;
        }
        config.set(path, value);
        return true;
    }

    private static boolean removeIfPresent(CommentedFileConfig config, String path) {
        if (!config.contains(path)) {
            return false;
        }
        config.remove(path);
        return true;
    }
}
