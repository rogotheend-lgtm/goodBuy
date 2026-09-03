DROP INDEX IF EXISTS idx_analysis_session_created_at;

ALTER TABLE analysis
    DROP COLUMN anonymous_session_id;

DROP TABLE anonymous_session;
