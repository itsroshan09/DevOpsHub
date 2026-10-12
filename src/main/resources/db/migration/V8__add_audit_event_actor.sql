ALTER TABLE audit_events
    ADD COLUMN actor_id BIGINT;

ALTER TABLE audit_events
    ADD CONSTRAINT fk_audit_events_actor
        FOREIGN KEY (actor_id)
            REFERENCES users(id)
            ON DELETE RESTRICT;

CREATE INDEX idx_audit_events_actor_id
    ON audit_events(actor_id);
