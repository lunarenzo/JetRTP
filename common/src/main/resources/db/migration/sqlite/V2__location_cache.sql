-- Table for storing cached rtp coordinates to prevent startup CPU spikes
CREATE TABLE IF NOT EXISTS "${tablePrefix}location_cache" (
    "id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "profile_name" TEXT NOT NULL,
    "world_name" TEXT NOT NULL,
    "x" DOUBLE NOT NULL,
    "y" DOUBLE NOT NULL,
    "z" DOUBLE NOT NULL,
    "yaw" REAL NOT NULL,
    "pitch" REAL NOT NULL
);
