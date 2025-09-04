grant all privileges on sadb.* to 'testuser1'@'asec-cmdenc-test-clt1.jpl.nasa.gov' identified by 'l0ngp@ssWord';
create user 'testuser2'@'asec-cmdenc-test-clt1.jpl.nasa.gov' require subject '/C=US/ST=California/L=Pasadena/O=Jet Propulsion Laboratory - MGSS TEST/OU=ASEC Testing/CN=SADB_testuser2/emailAddress=kgieselm@jpl.nasa.gov';
grant all privileges on sadb.* to 'testuser2'@'asec-cmdenc-test-clt1.jpl.nasa.gov';

