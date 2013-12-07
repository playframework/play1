CREATE TABLE test(name varchar);===
-- CREATE TABLE test2(name varchar);
CREATE TABLE test3(
	name varchar,
	quant int,
	cost real);===
CREATE TABLE 'tes;t4'(name varchar);===
CREATE FUNCTION test5() RETURNS VOID AS
$$
BEGIN
	SELECT * FROM test;
	RETURN;
END;
$$
LANGUAGE plpgsql;===
CREATE FUNCTION test6() RETURNS VOID AS
$function$
BEGIN
	SELECT * FROM $1$test3$1$;
	RETURN;
END;
$function$
LANGUAGE plpgsql;===
CREATE RULE test7
 AS ON UPDATE to titles
 DO ALSO (
   INSERT INTO title_acl VALUES (DEFAULT, NEW.title_id, OLD.user_id, 'editor');
   INSERT INTO title_acl VALUES (DEFAULT, NEW.title_id, NEW.user_id, 'owner')
 );===
 CREATE EXTENSION test8/**/;===
 CREATE TABLE test9(name varchar);