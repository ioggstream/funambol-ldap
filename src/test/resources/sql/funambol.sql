DROP TABLE fnbl_client_mapping if exists;
CREATE TABLE fnbl_client_mapping (
  principal bigint not null,
  sync_source varchar(128)  not null,
  luid varchar(200)  not null,
  guid varchar(200)  not null,
  last_anchor varchar(20)  DEFAULT NULL,
  PRIMARY KEY (principal,sync_source,luid,guid)
  );
  
CREATE TABLE fnbl_principal (
  username varchar(255) NOT NULL,
  device varchar(128) NOT NULL,
  id bigint NOT NULL,
  PRIMARY KEY (id)
  );

CREATE TABLE fnbl_user (
  username varchar(255) NOT NULL,
  password varchar(255) NOT NULL,
  email varchar(255) DEFAULT NULL,
  first_name varchar(255) DEFAULT NULL,
  last_name varchar(255) DEFAULT NULL,
  PRIMARY KEY (username)
  ); 
  
  CREATE TABLE fnbl_user_role (
  username varchar(255) NOT NULL,
  role varchar(128) NOT NULL,
  PRIMARY KEY (username,role),
  CONSTRAINT fk_userrole FOREIGN KEY (username) REFERENCES fnbl_user (username) ON DELETE CASCADE ON UPDATE CASCADE
  );

CREATE TABLE fnbl_id (
  idspace varchar(30) NOT NULL,
  counter bigint NOT NULL,
  increment_by int DEFAULT '100',
  PRIMARY KEY (idspace)
  );
