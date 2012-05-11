-- Table: server

DROP TABLE IF EXISTS server CASCADE;

CREATE TABLE server
(
  id bigserial NOT NULL,
  engine_type character varying(16) NOT NULL,
  hostname character varying(255) NOT NULL,
  host_address character varying(255) NOT NULL,
  port integer NOT NULL,
  product_type character varying(32) NOT NULL,
  "name" character varying(128) NOT NULL,
  max_clients integer NOT NULL,
  num_clients integer NOT NULL,
  response_time integer NOT NULL,
  last_pull_time bigint NOT NULL,
  credential_username character varying(32),
  credential_password character varying(32),
  map_token character varying(64),
  is_query_proxy_allowed boolean NOT NULL DEFAULT false,
  CONSTRAINT server_pkey PRIMARY KEY (id),
  CONSTRAINT server_host_address_key UNIQUE (host_address, port),
  CONSTRAINT server_hostname_key UNIQUE (hostname, port)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE server OWNER TO arkown_admin;
GRANT ALL ON TABLE server TO arkown_admin;
GRANT SELECT ON TABLE server TO arkown_read;
GRANT SELECT, UPDATE, INSERT, DELETE ON TABLE server TO arkown_write;

GRANT ALL ON TABLE server_id_seq TO arkown_admin;
GRANT SELECT ON TABLE server_id_seq TO arkown_read;
GRANT SELECT, UPDATE ON TABLE server_id_seq TO arkown_write;

-- Index: is_query_proxied

DROP INDEX IF EXISTS is_query_proxied;

CREATE INDEX is_query_proxied
  ON server
  USING btree
  (is_query_proxy_allowed);

-- Table: client

DROP TABLE IF EXISTS client CASCADE;

CREATE TABLE client
(
  id bigserial NOT NULL,
  server_id bigint NOT NULL,
  "name" character varying(32) NOT NULL,
  player_score integer,
  CONSTRAINT client_pkey PRIMARY KEY (id),
  CONSTRAINT client_server_id_fkey FOREIGN KEY (server_id)
      REFERENCES server (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT client_server_id_key UNIQUE (server_id, name)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE client OWNER TO arkown_admin;
GRANT ALL ON TABLE client TO arkown_admin;
GRANT SELECT ON TABLE client TO arkown_read;
GRANT SELECT, UPDATE, INSERT ON TABLE client TO arkown_write;

GRANT ALL ON TABLE client_id_seq TO arkown_admin;
GRANT SELECT ON TABLE client_id_seq TO arkown_read;
GRANT SELECT, UPDATE ON TABLE client_id_seq TO arkown_write;

