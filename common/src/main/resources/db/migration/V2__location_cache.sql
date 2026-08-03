-- Table for storing cached rtp coordinates to prevent startup CPU spikes
CREATE TABLE IF NOT EXISTS "${tablePrefix}location_cache" (
    "id" INT AUTO_INCREMENT NOT NULL,
    "profile_name" VARCHAR(255) NOT NULL,
    "world_name" VARCHAR(255) NOT NULL,
    "x" DOUBLE NOT NULL,
    "y" DOUBLE NOT NULL,
    "z" DOUBLE NOT NULL,
    "yaw" FLOAT NOT NULL,
    "pitch" FLOAT NOT NULL,
    PRIMARY KEY ("id")
);
