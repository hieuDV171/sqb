-- PostgreSQL. Back up the database and stop backend writers before running.
-- Apply changelog 09-02 first. Run this entire file before starting the new backend.
BEGIN;
SET LOCAL lock_timeout = '10s';

LOCK TABLE public.questions, public.user_ratings, public.question_error_reports
    IN SHARE ROW EXCLUSIVE MODE;

DO $migration$
DECLARE
    legacy_count bigint;
    inserted_count bigint;
    removed_count bigint;
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_catalog.pg_attribute
        WHERE attrelid = 'public.user_ratings'::regclass
          AND attname = 'is_error'
          AND NOT attisdropped
    ) THEN
        RAISE NOTICE 'Migration skipped: public.user_ratings.is_error has already been removed.';
        RETURN;
    END IF;

    SELECT COUNT(*) INTO legacy_count
    FROM public.user_ratings
    WHERE is_error IS TRUE;

    INSERT INTO public.question_error_reports (user_id, rated_question_id, description, created_at)
    SELECT user_id, rated_question_id, comment, created_at
    FROM public.user_ratings
    WHERE is_error IS TRUE
    ON CONFLICT (user_id, rated_question_id) DO NOTHING;

    GET DIAGNOSTICS inserted_count = ROW_COUNT;

    IF EXISTS (
        SELECT 1
        FROM public.user_ratings AS legacy
        WHERE legacy.is_error IS TRUE
          AND NOT EXISTS (
              SELECT 1
              FROM public.question_error_reports AS report
              WHERE report.user_id = legacy.user_id
                AND report.rated_question_id = legacy.rated_question_id
          )
    ) THEN
        RAISE EXCEPTION 'Some legacy reports have no matching row in question_error_reports.';
    END IF;

    -- Include questions whose reports were moved earlier by the compatibility code.
    WITH affected_questions AS (
        SELECT DISTINCT rated_question_id
        FROM public.question_error_reports
    ), star_statistics AS (
        SELECT affected.rated_question_id,
               COUNT(rating.user_id)::integer AS rating_count,
               COALESCE(AVG(rating.rating), 0.0) AS avg_rating
        FROM affected_questions AS affected
        LEFT JOIN public.user_ratings AS rating
            ON rating.rated_question_id = affected.rated_question_id
           AND rating.is_error IS FALSE
        GROUP BY affected.rated_question_id
    )
    UPDATE public.questions AS question
    SET rating_count = statistics.rating_count,
        avg_rating = statistics.avg_rating
    FROM star_statistics AS statistics
    WHERE question.id = statistics.rated_question_id
      AND (question.rating_count, question.avg_rating)
          IS DISTINCT FROM (statistics.rating_count, statistics.avg_rating);

    -- A rating of zero is one star. Only the legacy flag identifies a report.
    DELETE FROM public.user_ratings
    WHERE is_error IS TRUE;

    GET DIAGNOSTICS removed_count = ROW_COUNT;

    IF removed_count <> legacy_count THEN
        RAISE EXCEPTION 'Expected to remove % legacy reports but removed %.', legacy_count, removed_count;
    END IF;

    ALTER TABLE public.user_ratings DROP COLUMN is_error;

    RAISE NOTICE 'Legacy reports: %, copied: %, already present: %, removed from ratings: %.',
        legacy_count, inserted_count, legacy_count - inserted_count, removed_count;
END;
$migration$;

COMMIT;
