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
    `claim_id`  FOREIGN KEY         NOT NULL,
    `flag`      VARCHAR             NOT NULL,
    `value`     BIT                 NOT NULL,
    PRIMARY KEY (claim_id, flag),
    INDEX claim_index (claim_id),
    FOREIGN KEY (claim_id)
        REFERENCES claims(id)
        ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4;

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