package com.github.goldin.gcommons.beans

import static com.github.goldin.gcommons.GCommons.*
import groovy.util.logging.Slf4j
import org.codehaus.groovy.reflection.ReflectionUtils

/**
 * I/O-related utilities
 */
@Slf4j
class IOBean extends BaseBean
{
    long copy ( InputStream input, OutputStream output, long bytesExpected = -1 )
    {
        byte[] buffer         = new byte[ 2 * 1024 ]
        long   totalBytesRead = 0

        for ( int bytesRead = 0; (( bytesRead = input.read( buffer )) != -1 ); totalBytesRead += bytesRead )
        {
            output.write( buffer, 0, bytesRead )
        }

        close( input, output )

        if ( bytesExpected > -1 )
        {
            assert ( totalBytesRead == bytesExpected ), "[$bytesExpected] bytes should be read but [$totalBytesRead] bytes were read"
        }

        totalBytesRead
    }


    Closeable close ( Closeable ... closeables )
    {
        for ( c in closeables )
        {
            try { if ( c != null ) { c.close() }}
            catch ( IOException ignored ) {}
        }

        first( closeables )
    }

    /**
     * Retrieves resource specified using calling class ClassLoader.
     *
     * @param resource resource path
     * @return URL of the resource specified
     */
    @SuppressWarnings([ 'ParameterReassignment' ])
    URL resource( String resource )
    {
        assert resource
        resource = ( resource.startsWith( '/' ) ? resource : '/' + resource )
        assert resource.startsWith( '/' )

        final  c   = ReflectionUtils.getCallingClass( 0 )
        final  url = c.getResource( resource )
        assert url, "Failed to load resource [$resource] using ClassLoader of class [$c.name]\n" +
                    general().stars((( c.classLoader instanceof URLClassLoader ) ? (( URLClassLoader ) c.classLoader).URLs : [] ) as List )
        url
    }


    /**
     * Retrieves text of the resource specified using calling class ClassLoader.
     *
     * @param resourceName resource path
     * @param charset resource charset, <code>"UTF-8"</code> by default
     * @return
     */
    String resourceText( String resourceName, String charset = 'UTF-8' ) { resource( resourceName ).getText( charset ) }
}
