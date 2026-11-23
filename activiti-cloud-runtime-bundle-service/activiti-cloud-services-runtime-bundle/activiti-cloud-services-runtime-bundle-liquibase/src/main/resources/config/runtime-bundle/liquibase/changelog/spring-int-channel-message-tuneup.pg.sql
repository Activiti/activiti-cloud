-- Because PostgreSQL's PostgresChannelMessageStoreQueryProvider
-- deletes via physical tuple ID (CTID) using FOR UPDATE SKIP LOCKED,
-- this index is redundant during normal polling and adds write overhead
--DROP INDEX IF EXISTS INT_CHANNEL_MSG_DELETE_IDX;

-- Alter the sequence to cache values in memory to eliminate sequence lock
-- contention during high-frequency inserts.
ALTER SEQUENCE INT_MESSAGE_SEQ CACHE 100;
