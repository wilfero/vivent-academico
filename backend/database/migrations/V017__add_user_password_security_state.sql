ALTER TABLE users
    ADD COLUMN must_change_password boolean NOT NULL DEFAULT false,
    ADD COLUMN auth_version integer NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD CONSTRAINT ck_users_auth_version_non_negative CHECK (auth_version >= 0);
