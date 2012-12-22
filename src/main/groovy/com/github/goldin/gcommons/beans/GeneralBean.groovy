package com.github.goldin.gcommons.beans

import static com.github.goldin.gcommons.GCommons.*
import groovy.util.logging.Slf4j
import org.gcontracts.annotations.Ensures
import org.springframework.util.AntPathMatcher
import org.apache.commons.exec.*


/**
 * General usage methods
 */
@Slf4j
class GeneralBean extends BaseBean
{
    /**
     * {@link org.springframework.util.PathMatcher#match(String, String)} wrapper
     * @param path    path to match
     * @param pattern pattern to use, prepended with {@link org.springframework.util.AntPathMatcher#DEFAULT_PATH_SEPARATOR}
     *                                if path start with {@link org.springframework.util.AntPathMatcher#DEFAULT_PATH_SEPARATOR}
     *
     * @return true if path specified matches the pattern,
     *         false otherwise
     */
    boolean match ( String path, String pattern )
    {
        verify().notNullOrEmpty( path, pattern )

        ( path, pattern ) = [ path, pattern ]*.replaceAll( /\\+/, AntPathMatcher.DEFAULT_PATH_SEPARATOR )

        if ( path.startsWith( AntPathMatcher.DEFAULT_PATH_SEPARATOR ) != pattern.startsWith( AntPathMatcher.DEFAULT_PATH_SEPARATOR ))
        {   // Otherwise, false is returned
            pattern = "${ AntPathMatcher.DEFAULT_PATH_SEPARATOR }${ pattern }"
        }

        new AntPathMatcher().match( pattern, path )
    }


    /**
     * Retrieves first non-null object.
     * @param objects objects to check
     * @return first non-null object
     */
    @SuppressWarnings( 'UnnecessaryPublicModifier' )
    public <T> T choose ( T ... objects )
    {
        def      result = ( T ) objects.find { it != null }
        assert ( result != null ), 'All objects specified are null'
        result
    }


    /**
     * Attempts to execute a closure specified and return its result.
     *
     * @param nTries     number of time execution will be attempted
     * @param resultType expected type of result to be returned by closure,
     *                   if <code>null</code> - result type check is not performed
     * @param c          closure to invoke
     * @return closure execution result
     * @throws RuntimeException if execution fails nTries times
     */
    @SuppressWarnings( 'UnnecessaryPublicModifier' )
    public <T> T tryIt( int nTries, Class<T> resultType, Closure c )
    {
        assert ( nTries > 0 )
        verify().notNull( c )

        def tries = 0

        while( true )
        {
            try
            {
                Object value = c()
                assert ( resultType == null ) || ( value != null ), \
                       "Result returned is null, should be of type [$resultType]"
                assert ( resultType == null ) || resultType.isInstance( value ), \
                       "Result returned [$value] is of type [${ value.class }], should be of type [$resultType]"
                return (( T ) value )
            }
            catch ( Throwable t )
            {
                assert tries < nTries
                if (( ++tries ) == nTries )
                {
                    throw new RuntimeException( "Failed to perform action after [$tries] attempt${s( tries )}: $t", t )
                }
            }
        }
    }


    /**
     * Returns '' if number specified is 1, 's' otherwise. Used for combining plural sentences in log messages.
     * @param n number to check
     * @return '' if number specified is 1, 's' otherwise
     */
    String s( Number n ) { ( n == 1 ) ? '' : 's' }


    /**
     * {@code "Object.metaClass.splitWith"} wrapper - splits object to "pieces" uses method specified.
     *
     * @param o          object to split
     * @param methodName name of the method to use, the method should accept a Closure argument
     * @return           list of objects returned by iterating method
     */
    @SuppressWarnings( 'UnnecessaryPublicModifier' )
    public <T> List<T> splitWith( Object o, String methodName, Class<T> type = Object ) { o.splitWith( methodName, type ) }


    /**
     * Retrieves an array combined from values provided:
     * <ul>
     * <li> If first parameter specified is not <code>null</code> - it is returned as a single-element array.
     * <li> If second parameter specified is not <code>null</code> - it is returned as-is.
     * <li> Otherwise, empty array is returned.
     * </ul>
     *
     * @param instance single instance to return as a single-element array
     * @param array    second option to check  if <code>instance</code> is <code>null</code>
     * @param type     element's type (required for creating an array)
     * @param <T>      element's type
     *
     * @return         instance specified as a single-element array or array specified if instance is null.
     */
    @Ensures({ result != null })
    public <T> List<T> list ( T[] array, T instance )
    {
        ( array )            ? array.toList() :
        ( instance != null ) ? [ instance ]   :
                               []
    }


    /**
     * Determines if current OS is Windows according to "os.name" system property
     * @return true  if current OS is Windows,
     *         false otherwise
     */
    boolean isWindows()
    {
        System.getProperty( 'os.name' ).toLowerCase().contains( 'windows' )
    }


    /**
     * Executes the command specified.
     *
     * @param command     command to execute
     * @param option      strategy for executing the command, {@link ExecOption#Runtime} by default
     * @param failOnError whether {@code RuntimeException} should be thrown when running command fails
     * @param timeoutMs   command timeout in ms, 5 min by default.
     *                    Returns immediately if zero value is specified, blocks and waits for process
     *                    to terminate if negative value is specified, blocks and waits amount of
     *                    milliseconds specified if positive value is specified.
     * @param directory   process working directory
     * @param environment environment to pass to process started
     *
     * @return            stdout + stderr of the command specified
     */
    String executeWithResult ( String     command,
                               ExecOption option      = ExecOption.Runtime,
                               boolean    failOnError = true,
                               long       timeoutMs   = ( 5 * constants().MILLIS_IN_MINUTE ) /* 5 min */,
                               File       directory   = new File( constants().USER_DIR ),
                               Map        environment = new HashMap( System.getenv()))
    {
        ByteArrayOutputStream out      = new ByteArrayOutputStream( 1024 )
        def                   readOut  = { out.toString( 'UTF-8' ).trim() }
        ByteArrayOutputStream err      = new ByteArrayOutputStream( 64   )
        def                   readErr  = { err.toString( 'UTF-8' ).trim() }

        try
        {
            execute( command, option, out, err, timeoutMs, directory, environment )
        }
        catch ( Throwable e )
        {
            if ( ! readErr())
            {
                err.write(( e.class.name + ' : ' + e.message ).getBytes( 'UTF-8' ))
            }

            if ( failOnError )
            {
                throw new RuntimeException(
                    "Failed to execute \"$command\" in [${ directory.canonicalPath }], " +
                    "stdout is [${ readOut() }], stderr is [${ readErr() }]",
                    e )
            }
        }

        String result = readOut() + readErr()
        result
    }



    /**
     * Executes the command specified.
     *
     * @param command     command to execute
     * @param option      strategy for executing the command, {@link ExecOption#Runtime} by default
     * @param stdout      OutputStream to send command's stdout to, System.out by default
     * @param stderr      OutputStream to send command's stderr to, System.err by default
     * @param timeoutMs   command timeout in ms, 5 min by default.
     *                    Returns immediately if zero value is specified, blocks and waits for process
     *                    to terminate if negative value is specified, blocks and waits amount of
     *                    milliseconds specified if positive value is specified.
     * @param directory   process working directory
     * @param environment environment to pass to process started
     *
     * @return           command exit value or -1 if negative or zero timeout was specified
     */
    int execute ( String       command,
                  ExecOption   option      = ExecOption.Runtime,
                  OutputStream stdout      = System.out,
                  OutputStream stderr      = System.err,
                  long         timeoutMs   = ( 5 * constants().MILLIS_IN_MINUTE ) /* 5 min */,
                  File         directory   = new File( constants().USER_DIR ),
                  Map          environment = new HashMap( System.getenv()))
    {
        verify().notNullOrEmpty( command )
        verify().directory( directory )

        try
        {
            switch ( option )
            {
                case ExecOption.CommonsExec:

                    Executor executor = new DefaultExecutor()
                    executor.with {
                        streamHandler    = new PumpStreamHandler( stdout, stderr )
                        workingDirectory = directory
                        watchdog         = ( timeoutMs == 0 ) ? null :
                                           ( timeoutMs  < 0 ) ? new ExecuteWatchdog( ExecuteWatchdog.INFINITE_TIMEOUT ) :
                                                                new ExecuteWatchdog( timeoutMs )
                    }

                    return executor.execute( CommandLine.parse( command ), environment )

                case ExecOption.Runtime:

                    def envp = environment.collect{ "$it.key=$it.value" } as String[]
                    return handleProcess( command.execute( envp, directory ), stdout, stderr, timeoutMs )

                case ExecOption.ProcessBuilder:

                    ProcessBuilder builder = new ProcessBuilder( command ).directory( directory )
                    builder.environment() << environment
                    return handleProcess( builder.start(), stdout, stderr, timeoutMs )

                default:
                    throw new RuntimeException( "Unknown option [$option]. Known options are ${ ExecOption.values() }" )
            }
        }
        catch ( Throwable e )
        {
            throw new RuntimeException( "Failed to execute \"$command\" in [$directory.canonicalPath]", e )
        }
        finally
        {
            stdout.close()
            stderr.close()
        }
    }


    /**
     * Handles the process specified.
     *
     * @param p         process to handle
     * @param stdout    standard output to send process output stream
     * @param stderr    error output to send process error stream
     * @param timeoutMs process execution timeout in milliseconds,
     *                  execution is asynchronous if timeout is zero
     * @return          process exit code
     */
    private int handleProcess( Process p, OutputStream stdout, OutputStream stderr, long timeoutMs )
    {
        if ( timeoutMs > 0 )
        {
            p.consumeProcessOutputStream( stdout )
            p.consumeProcessErrorStream ( stderr )
            p.waitForOrKill( timeoutMs )
        }
        else
        {
            p.waitForProcessOutput( stdout, stderr )
        }

        stdout.close()
        stderr.close()
        p.exitValue()
    }


    /**
     * Creates a decorated multi-line <code>Collection</code> representation where each element is prepended
     * with a prefix and optional space.
     *
     * @param          c <code>Collection</code> to iterate
     * @param prefix   decoration prefix
     * @param padSize  elements spacing, starting from the second element
     * @param crlf     line separator to use, system's 'line.separator' by default
     * @return         multi-line <code>Collection</code> representation where each element is prepended with a prefix,
     *                 empty <code>String</code> if collection is empty
     */
    String stars ( Collection c, String prefix = '* ', int padSize = 0, String crlf = constants().CRLF )
    {
        ( c ? "$prefix[${ c.join( "]$crlf${ ' ' * padSize }$prefix[") }]" : '' )
    }
}


/**
 * Strategy for executing the command, see {@link GeneralBean#execute}
 */
public enum ExecOption
{
    /**
     * Apache Commons Exec {@link Executor} is used
     */
    CommonsExec,

    /**
     * {@link Runtime#getRuntime()} is used
     */
    Runtime,


    /**
     * {@link ProcessBuilder} is used
     */
    ProcessBuilder
}
