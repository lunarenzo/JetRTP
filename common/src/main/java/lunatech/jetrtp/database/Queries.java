package lunatech.jetrtp.database;

import lunatech.jetrtp.cooldown.CooldownType;
import lunatech.jetrtp.cooldown.Cooldowns;
import lunatech.jetrtp.database.handler.DatabaseType;
import lunatech.jetrtp.database.schema.tables.records.CooldownsRecord;
import lunatech.jetrtp.messaging.message.BidirectionalMessage;
import lunatech.jetrtp.messaging.message.Message;
import lunatech.jetrtp.messaging.message.OutgoingMessage;
import lunatech.jetrtp.utility.DB;
import lunatech.jetrtp.utility.Logger;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static lunatech.jetrtp.database.QueryUtils.UUIDUtil;
import static lunatech.jetrtp.database.schema.Tables.*;
import static org.jooq.impl.DSL.*;

/**
 * A class providing access to all SQL queries.
 */
@SuppressWarnings({"LoggingSimilarMessage", "StringConcatenationArgumentToLogCall"})
public final class Queries {

    /**
     * Holds all queries related to using the database as a messaging service.
     */
    @ApiStatus.Internal
    public static final class Sync {
        /**
         * Fetch the latest (greatest) message ID from the database.
         *
         * @return the message id or empty if no messages are queued
         */
        public static Optional<Integer> fetchLatestMessageId() {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                return context
                    .select(max(MESSAGING.ID))
                    .from(MESSAGING)
                    .fetchOptional(0, Integer.class);
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!" + e);
                return Optional.empty();
            }
        }

        /**
         * Adds a message to the database.
         *
         * @param message the outgoing message to send
         * @return the new message id or empty if insert failed
         */
        public static <T> Optional<Integer> send(OutgoingMessage<T> message) {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                return context
                    .insertInto(MESSAGING, MESSAGING.TIMESTAMP, MESSAGING.MESSAGE)
                    .values(
                        currentLocalDateTime(),
                        val(message.encodeAsString())
                    )
                    .returningResult(MESSAGING.ID)
                    .fetchOptional(0, Integer.class);
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!" + e);
                return Optional.empty();
            }
        }

        /**
         * Fetch all messages from the database.
         *
         * @param latestSyncId    the currently synced to message id
         * @param cleanupInterval the configured cleanup interval
         * @return the messages
         */
        public static Map<Integer, Message<?>> receive(int latestSyncId, long cleanupInterval) {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                return context
                    .selectFrom(MESSAGING)
                    .where(MESSAGING.ID.greaterThan(latestSyncId)
                        .and(MESSAGING.TIMESTAMP.greaterOrEqual(localDateTimeSub(currentLocalDateTime(), cleanupInterval / 1000, DatePart.SECOND))) // Checks TIMESTAMP >= now() - cleanupInterval
                    )
                    .orderBy(MESSAGING.ID.asc())
                    .fetch()
                    .intoMap(MESSAGING.ID, r -> BidirectionalMessage.from(r.getMessage()));
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!" + e);
                return Map.of();
            }
        }

        /**
         * Deletes all outdate messages from the database.
         *
         * @param cleanupInterval the configured cleanup interval
         */
        public static void cleanup(long cleanupInterval) {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                context
                    .deleteFrom(MESSAGING)
                    .where(MESSAGING.TIMESTAMP.lessThan(localDateTimeSub(currentLocalDateTime(), cleanupInterval / 1000, DatePart.SECOND))) // Checks TIMESTAMP < now() - cleanupInterval
                    .execute();
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!" + e);
            }
        }
    }

    /**
     * Wrapper class to organize cooldown-related queries.
     */
    public static final class Cooldown {
        public static Map<CooldownType, Instant> load(OfflinePlayer player) {
            return load(player.getUniqueId());
        }

        public static Map<CooldownType, Instant> load(UUID uuid) {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                final Result<CooldownsRecord> cooldownsRecords = context
                    .selectFrom(COOLDOWNS)
                    .where(COOLDOWNS.UUID.eq(UUIDUtil.toBytes(uuid)))
                    .fetch();

                return cooldownsRecords.stream()
                    .collect(Collectors.toMap(
                        r -> CooldownType.valueOf(r.getCooldownType()),
                        r -> QueryUtils.InstantUtil.fromDateTime(r.getCooldownTime())
                    ));
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!", e);
            }
            return Collections.emptyMap();
        }

        public static void save(OfflinePlayer player) {
            save(player.getUniqueId());
        }

        public static void save(UUID uuid) {
            try (
                Connection con = DB.getConnection()
            ) {
                DSLContext context = DB.getContext(con);

                context.transaction(config -> {
                    DSLContext ctx = config.dsl();

                    // Delete old cooldowns
                    ctx.deleteFrom(COOLDOWNS)
                        .where(COOLDOWNS.UUID.eq(UUIDUtil.toBytes(uuid)))
                        .execute();

                    // Insert new cooldowns
                    final List<CooldownsRecord> cooldownsRecords = new ArrayList<>();

                    for (CooldownType cooldownType : CooldownType.values()) {
                        if (!Cooldowns.has(uuid, cooldownType))
                            continue;

                        cooldownsRecords.add(new CooldownsRecord(
                            UUIDUtil.toBytes(uuid),
                            cooldownType.name(),
                            QueryUtils.InstantUtil.toDateTime(Cooldowns.get(uuid, cooldownType))
                        ));
                    }

                    if (!cooldownsRecords.isEmpty())
                        ctx.batchInsert(cooldownsRecords).execute();
                });
            } catch (SQLException e) {
                Logger.get().error("SQL Query threw an error!", e);
            }
        }
    }

    /**
     * Holds all queries related to location cache serialization.
     */
    public static final class LocationCache {
        public static void save(List<lunatech.jetrtp.model.CachedLocation> locations) {
            if (!DB.isStarted()) return;
            try (Connection con = DB.getConnection()) {
                DSLContext context = DB.getContext(con);
                context.deleteFrom(table(name("location_cache"))).execute();
                if (locations.isEmpty()) return;

                List<org.jooq.Query> queries = new ArrayList<>();
                for (var loc : locations) {
                    queries.add(
                        context.insertInto(table(name("location_cache")),
                            field(name("profile_name")),
                            field(name("world_name")),
                            field(name("x")),
                            field(name("y")),
                            field(name("z")),
                            field(name("yaw")),
                            field(name("pitch"))
                        ).values(
                            loc.profileName(),
                            loc.worldName(),
                            loc.x(),
                            loc.y(),
                            loc.z(),
                            loc.yaw(),
                            loc.pitch()
                        )
                    );
                }
                context.batch(queries).execute();
            } catch (SQLException e) {
                Logger.get().error("Failed to save location cache to database: " + e.getMessage());
            }
        }

        public static List<lunatech.jetrtp.model.CachedLocation> load() {
            if (!DB.isStarted()) return Collections.emptyList();
            List<lunatech.jetrtp.model.CachedLocation> list = new ArrayList<>();
            try (Connection con = DB.getConnection()) {
                DSLContext context = DB.getContext(con);
                var records = context.select(
                    field(name("profile_name")),
                    field(name("world_name")),
                    field(name("x")),
                    field(name("y")),
                    field(name("z")),
                    field(name("yaw")),
                    field(name("pitch"))
                ).from(table(name("location_cache"))).fetch();

                for (var r : records) {
                    list.add(new lunatech.jetrtp.model.CachedLocation(
                        r.get("profile_name", String.class),
                        r.get("world_name", String.class),
                        r.get("x", Double.class),
                        r.get("y", Double.class),
                        r.get("z", Double.class),
                        r.get("yaw", Float.class),
                        r.get("pitch", Float.class)
                    ));
                }
            } catch (SQLException e) {
                Logger.get().error("Failed to load location cache from database: " + e.getMessage());
            }
            return list;
        }
    }
}
