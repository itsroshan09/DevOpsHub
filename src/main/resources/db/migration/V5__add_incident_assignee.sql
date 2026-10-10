
ALTER TABLE incidents
    ADD COLUMN assigned_to BIGINT NULL;

ALTER TABLE incidents
    ADD CONSTRAINT fk_incidents_assigned_user
        FOREIGN KEY (assigned_to)
            REFERENCES users(id)
            ON DELETE RESTRICT;

CREATE INDEX idx_incidents_assigned_to
    ON incidents(assigned_to);
