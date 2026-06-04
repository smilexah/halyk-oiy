-- Creates one database per service inside the single shared Postgres instance.
-- Mounted at /docker-entrypoint-initdb.d/ and executed once on first start.

CREATE DATABASE budget_db;
CREATE DATABASE transaction_db;
CREATE DATABASE goals_db;
CREATE DATABASE family_db;
CREATE DATABASE keycloak_db;
CREATE DATABASE analytics_db;
CREATE DATABASE priors_db;
CREATE DATABASE alser_db;
CREATE DATABASE travel_db;