
ALTER TABLE incidents
    ADD COLUMN service_id BIGINT;

ALTER TABLE incidents
    ADD CONSTRAINT fk_incidents_service
        FOREIGN KEY (service_id)
            REFERENCES services (id);

CREATE INDEX idx_incidents_service_id
    ON incidents (service_id);
