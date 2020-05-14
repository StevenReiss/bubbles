CREATE TABLE SrcClass (
   name varchar(512) NOT NULL,
   type varchar(512),
   superclass varchar(512),
   signature varchar(512),
   source varchar(512),
   access int,
   is_abstract smallint,
   is_enum smallint,
   is_final smallint,
   is_iface smallint,
   is_private smallint,
   is_protected smallint,
   is_public smallint,
   is_static smallint,
   is_project smallint
);

CREATE TABLE SrcInterface (
   type varchar(512) NOT NULL,
   iface varchar(512) NOT NULL,
   is_super smallint
);

CREATE TABLE SrcField (
   id varchar(512) NOT NULL,
   name varchar(512) NOT NULL,
   class varchar(512) NOT NULL,
   type varchar(512),
   signature varchar(512),
   access int,
   is_final smallint,
   is_private smallint,
   is_protected smallint,
   is_public smallint,
   is_static smallint,
   is_transient smallint,
   is_volatile smallint
);


CREATE TABLE SrcMethod (
   id varchar(512) NOT NULL,
   name varchar(512) NOT NULL,
   class varchar(512) NOT NULL,
   type varchar(512),
   signature varchar(512),
   numarg int,
   returns varchar(512),
   access int,
   is_abstract smallint,
   is_final smallint,
   is_native smallint,
   is_private smallint,
   is_protected smallint,
   is_public smallint,
   is_static smallint,
   is_synchronized smallint,
   is_varargs smallint
);


CREATE TABLE SrcMethodParam (
   methodid varchar(512) NOT NULL,
   name varchar(512),
   type varchar(512),
   indexno int
);


CREATE TABLE SrcCall (
   methodid varchar(512),
   toclass varchar(512),
   tomethod varchar(512),
   totype varchar(512)
);


CREATE TABLE SrcLines (
   methodid varchar(512),
   lineno int,
   startoffset int
);


CREATE TABLE SrcAlloc (
   methodid varchar(512),
   class varchar(512)
);



