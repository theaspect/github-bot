CREATE TABLE SUB (
  chat_id BIGINT       NOT NULL,
  user_id VARCHAR(100) NOT NULL,
  PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE EVENT (
  event_id   BIGINT PRIMARY KEY,
  user_id    VARCHAR(100) NOT NULL,
  repo_id    VARCHAR(100) NOT NULL,
  event      VARCHAR(100) NOT NULL,
  date       DATETIME     NOT NULL,
  actor      VARCHAR(100),
  sent       BOOLEAN DEFAULT FALSE
);
