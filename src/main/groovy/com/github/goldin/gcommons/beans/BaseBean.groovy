package com.github.goldin.gcommons.beans

/**
 * Base class for other beans, provides reusable functionality for all of them.
 */
class BaseBean
{
    /**
     * {@link AntBuilder} instance to be used by all beans
     */
    protected final AntBuilder ant = new AntBuilder()

    /**
     * Retrieves first element in the array specified.
     *
     * @param objects array of objects
     * @return first element in the array specified, or <code>null</code> if it si empty
     */
    protected <T> T first( T[] objects ) { objects.size() ? objects[ 0 ] : null }
}
