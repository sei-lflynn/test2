package gov.nasa.jpl.ammos.asec.kmc.api.sadb;

import gov.nasa.jpl.ammos.asec.kmc.api.ex.KmcException;

/**
 * Database session abstraction. General usage flow:
 * 1. Begin a transaction
 * 2. Make a change (persist/merge/delete). These changes happen in locally, in memory
 * 3. Flush changes to the database. This performs the INSERT/UPDATE/DELETE directives in the database itself.
 * 4. (Optionally) If an error occurs, the changes can be rolled back
 * 5. Commit the changes in the database
 *
 */
public interface IDbSession extends AutoCloseable {
    /**
     * Begin a database transaction
     */
    void beginTransaction();

    /**
     * Indicates session/transaction activity
     *
     * @return true if the transaction is active, false if the transaction is not
     */
    boolean isActive();

    /**
     * Validates a database session/transaction
     *
     * @throws KmcException exception
     */
    void validate() throws KmcException;

    /**
     * Commits a database transaction
     *
     * @throws KmcException exception
     */
    void commit() throws KmcException;

    /**
     * Rolls back a database transaction
     *
     * @throws KmcException exception
     */
    void rollback() throws KmcException;

    /**
     * Persists a change to a database object
     *
     * @param o database object to persist
     * @throws KmcException exception
     */
    void persist(Object o) throws KmcException;

    /**
     * Merges (ie, updates) a change to a database object
     *
     * @param o database object to merge
     * @throws KmcException exception
     */
    void merge(Object o) throws KmcException;

    /**
     * Removes (ie, deletes) a database object
     *
     * @param o database object to delete
     * @throws KmcException exception
     */
    void remove(Object o) throws KmcException;

    /**
     * Flushes current changes to the database
     */
    void flush();
}
