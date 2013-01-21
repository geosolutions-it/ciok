-- Table: gaez.data
update gaez.data SET status_code='LCK' where status_code!='LCK';
update gaez.data SET status_code='RDY', status_msg='' where gaez_id in (select gaez_id from gaez.data where file_name_rst='prc_1978.rst');
select * from gaez.data where file_name_rst='prc_1978.rst';
-- DROP TABLE gaez.data;

CREATE TABLE gaez.data
(
  gaez_id character varying(100) NOT NULL,
  theme character varying(20) DEFAULT NULL::character varying,
  crop character varying(50) DEFAULT NULL::character varying,
  scen character varying(50) DEFAULT NULL::character varying,
  "time" character varying(4) DEFAULT NULL::character varying,
  input_level character varying(1) DEFAULT NULL::character varying,
  avail_water_content character varying(10) DEFAULT NULL::character varying,
  water_supply character varying(1) DEFAULT NULL::character varying,
  production_type character varying(10) DEFAULT NULL::character varying,
  "index" character varying(3) DEFAULT NULL::character varying,
  co2f character varying(1) DEFAULT NULL::character varying,
  file_class character varying(20) DEFAULT NULL::character varying,
  file_type character varying(2) DEFAULT NULL::character varying,
  file_rule character varying(2) DEFAULT NULL::character varying,
  file_path_rst character varying(100) NOT NULL,
  file_name_rst character varying(100) NOT NULL,
  file_path_rdc character varying(100) DEFAULT NULL::character varying,
  file_name_rdc character varying(100) DEFAULT NULL::character varying,
  file_path_tif character varying(100) DEFAULT NULL::character varying,
  file_name_tif character varying(100) DEFAULT NULL::character varying,
  file_associated character varying(100) DEFAULT NULL::character varying,
  ignore smallint DEFAULT 1,
  status_code character varying(3) DEFAULT 'RDY'::character varying,
  status_msg character varying(255) DEFAULT NULL::character varying,
  scalar double precision,
  min_value double precision,
  max_value double precision,
  dmin_value character varying(8) DEFAULT NULL::character varying,
  dmax_value character varying(8) DEFAULT NULL::character varying,
  id bigint NOT NULL DEFAULT nextval(('gaez.io_seriale'::text)::regclass),
  CONSTRAINT gaez_data_pkey PRIMARY KEY (gaez_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE gaez.data OWNER TO gis;

-- Index: gaez.id

-- DROP INDEX gaez.id;

CREATE UNIQUE INDEX id
  ON gaez.data
  USING btree
  (gaez_id NULLS FIRST);

