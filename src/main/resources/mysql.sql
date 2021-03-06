CREATE TABLE IF NOT EXISTS claims (
    `id`    INT AUTO_INCREMENT  NOT NULL,
    `owner` VARCHAR(36)         NOT NULL,
    `x1`    INT                 NOT NULL,
    `y1`    INT                 NOT NULL,
    `z1`    INT                 NOT NULL,
    `x2`    INT                 NOT NULL,
    `y2`    INT                 NOT NULL,
    `z2`    INT                 NOT NULL,
    `world` VARCHAR(36)         NOT NULL,
    PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS claim_flags (
    `claim_id`      INT             NOT NULL,
    `flag`          VARCHAR(255)    NOT NULL,
    `auth_level`    VARCHAR(20)     NOT NULL,
    `value`         BIT             NOT NULL,
    PRIMARY KEY (claim_id, flag, auth_level),
    FOREIGN KEY claim_index (claim_id)
        REFERENCES claims(id)
        ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS claim_access (
    `claim_id`      INT         NOT NULL,
    `player`        VARCHAR(36) NOT NULL,
    `auth_level`    VARCHAR(20) NOT NULL,
    PRIMARY KEY (claim_id, player),
    FOREIGN KEY claim_index (claim_id)
        REFERENCES  claims(id)
        ON DELETE CASCADE
) DEFAULT  CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS unlocked_items (
    `player`    VARCHAR(36)     NOT NULL,
    `item`      VARCHAR(35)     NOT NULL,
    PRIMARY KEY (player, item)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS balances (
    `player`    VARCHAR(36)     NOT NULL,
    `balance`   INT             NOT NULL,
    PRIMARY KEY (player)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS transactions (
    `payee`     VARCHAR(36)     NOT NULL,
    `payer`     VARCHAR(36),
    `amount`    INT             NOT NULL,
    `desc`      TINYTEXT        NOT NULL,
    PRIMARY KEY (payer, payee)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS inventory_cache (
    `holder`        VARCHAR(36) NOT NULL,
    `last_stored`   TRAILING    NOT NULL,
    `contents`      LONGTEXT    NOT NULL,
    `armor`         LONGTEXT    NOT NULL,
    `exp`           INT         NOT NULL,
    `x`             INT         NOT NULL,
    `y`             INT         NOT NULL,
    `z`             INT         NOT NULL,
    `world`         VARCHAR(36) NOT NULL,
    PRIMARY KEY (holder)
)