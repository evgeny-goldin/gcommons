package com.github.goldin.gcommons.util

import java.nio.ByteBuffer
import java.nio.charset.Charset
import org.slf4j.Logger


/**
 * {@link OutputStream} implementation sending all output to SLF4J {@link Logger}.
 */
class LoggerOutputStream extends OutputStream implements Closeable
{
    final String     name
    final Logger     logger
    final boolean    isError
    final ByteBuffer buffer


    /**
     * Creates a new stream redirecting all data to the logger specified.
     *
     * @param name    name of this stream
     * @param logger  logger redirect the output to
     * @param isError whether messages should be logged with logger.error() instead of logger.info()
     */
    LoggerOutputStream ( String name, Logger logger, boolean isError = false )
    {
        assert name,   'Name specified is null'
        assert logger, 'Logger specified is null'

        this.name    = name
        this.logger  = logger
        this.isError = isError
        this.buffer  = ByteBuffer.allocate( 10 * 1024 )
    }


    @Override
    void write ( int b )
    {
        if ( ! buffer.hasRemaining()) { flush() }
        assert buffer.hasRemaining()
        buffer.put(( byte ) b )
    }


    @Override
    void flush ()
    {
        if ( buffer.position() > 0 )
        {
            /**
             * Indexes of line feed characters in buffer
             */
            def lineFeeds = buffer.array().findIndexValues{ it == '\n' }.findAll { it < buffer.position() } ?:
                            [ buffer.position() - 1 ]

            int start = 0                 // 'start' is an index of first String byte
            for ( int end in lineFeeds )  // 'end'   is an index of last  String byte
            {
                String s       = new String( buffer.array(), start, ( end - start + 1 ), Charset.forName( 'UTF-8' )).
                                 replace( '\r', '' ).replace( '\n', '' )
                String message = "$name: $s"

                isError ? logger.error( message ) : logger.info( message )
                start = end + 1
            }

            /**
             * Moving buffer 'position' to the beginning of the last unterminated String
             * and shifting the content that is left in the buffer to its beginning.
             */
            buffer.position(( int ) lineFeeds[ -1 ] + 1 )
            buffer.compact()
        }
    }


    @Override
    void close ()
    {
        flush()
        super.close()
    }
}
