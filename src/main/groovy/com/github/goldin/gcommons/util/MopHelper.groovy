package com.github.goldin.gcommons.util

import static com.github.goldin.gcommons.GCommons.*
import com.github.goldin.gcommons.beans.BaseBean
import groovy.io.FileType
import groovy.util.logging.Slf4j
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * MOP updates implementations.
 * http://evgeny-goldin.com/wiki/GCommons#MOP_Updates
 */
@Slf4j
class MopHelper extends BaseBean
{
    /**
     * Container that holds callback invocation results.
     * See {@link #invokeCallback}
     */
    static class InvocationResult
    {
        boolean filterPass
        boolean invocationResult
        boolean wasCalled
    }


    /**
     * Recursive iteration configuration parameters.
     */
    static class RecurseConfig
    {
        Closure  filter
        FileType fileType
        FileType filterType
        boolean  stopOnFalse
        boolean  stopOnFilter
        boolean  detectLoops
        boolean  returnList
    }


    /**
     * Splits object to "pieces" with an "each"-like function specified by name.
     * http://evgeny-goldin.com/wiki/GCommons#MOP_Updates
     *
     * @param delegate original delegate object
     * @param args     invocation object, method and result type (optional)
     */
    List splitWith( Object delegate, Object[] args )
    {
        Object o           // Invocation object
        String methodName  // name of method to invoke
        Class  type        // Type of elements returned

        switch( args.size())
        {
        /**
         * [0] = {java.lang.String@2412}"eachByte" - invocation method
         */
            case 1 : o          = delegate
                     methodName = args[ 0 ]
                     type       = null
                     break
        /**
         * Two options:
         *
         * [0] = {java.lang.String@1944}"eachLine"              - invocation method
         * [1] = {java.lang.Class@1585}"class java.lang.Object" - type
         *
         * or
         *
         * [0] = {java.lang.String@2726}"aa" - invocation object
         * [1] = {java.lang.String@2727}""   - invocation method
         */
            case 2 : def typeAvailable = args[ 1 ] instanceof Class
                     o                 = typeAvailable ? delegate  : args[ 0 ]
                     methodName        = typeAvailable ? args[ 0 ] : args[ 1 ]
                     type              = typeAvailable ? args[ 1 ] : null
                     break
        /**
         * [0] = {java.lang.String@2549}"1\n2"              - invocation object
         * [1] = {java.lang.String@1936}"eachLine"          - invocation method
         * [2] = {java.lang.Class@1421}"class java.io.File" - type
         */
            case 3 : o          = args[ 0 ]
                     methodName = args[ 1 ]
                     type       = args[ 2 ]
                     break
            default : throw new RuntimeException( "splitWith() args is of size [${args.size()}]: [$args]" )
        }

        methodName = ( methodName ?: '' ).trim()
        assert     methodName, 'Method name is not provided'
        MetaMethod m = o.metaClass.pickMethod( methodName, Closure )
        assert     m, "No method [$methodName] accepting Closure argument is found for class [${ o.class.name }]"

        def result = []
        m.doMethodInvoke( o ) { result << it }

        if (( type ) && ( type != Object ))
        {
            result.each{ assert type.isInstance( it ), \
                         "Object [$it][${ it.class.name }] returned by method [$methodName] is not an instance of type [$type.name]" }
        }

        result
    }


    /**
     * Calculates directory size.
     *
     * @param delegate original delegate object, must be a directory
     * @return directory size as a sum of all files it contains recursively
     */
    long directorySize( File delegate )
    {
        long size = 0
        delegate.recurse([ type : FileType.FILES, detectLoops : true, returnList : false ]){ size += it.size() }
        size
    }


    /**
     * Enhanced recursive files iteration.
     * http://evgeny-goldin.com/wiki/GCommons#MOP_Updates
     *
     * @param directory original delegate object
     * @param config    configurations Map with following keys:
     * <li> {@code "filter"}
     * <li> {@code "type"}
     * <li> {@code "filterType"}
     * <li> {@code "stopOnFalse"}
     * <li> {@code "stopOnFilter"}
     * <li> {@code "detectLoops"}
     * <li> {@code "returnList"}
     * @param callback invocation callback
     */
    @Requires({ directory.directory && ( config != null ) && callback })
    @Ensures({ result != null })
    List<File> recurse( File directory, Map config = [:], Closure callback = {} )
    {
        def read = {
            String key -> config.remove( key )
        }
        new RecurseConfig().with {

            filter       = ( Closure ) read( 'filter' ) // Allowed to be null
            fileType     = general().choose(( FileType ) read( 'type'         ), FileType.ANY )
            filterType   = general().choose(( FileType ) read( 'filterType'   ), fileType     )
            // noinspection GroovyAssignabilityCheck
            stopOnFalse  = general().choose( read( 'stopOnFalse'  ), false )
            // noinspection GroovyAssignabilityCheck
            stopOnFilter = general().choose( read( 'stopOnFilter' ), false )
            // noinspection GroovyAssignabilityCheck
            detectLoops  = general().choose( read( 'detectLoops'  ), false )
            // noinspection GroovyAssignabilityCheck
            returnList   = general().choose( read( 'returnList'   ), true  )

            assert config.isEmpty(), "Unread values left in config: $config"

            handleDirectory( directory, callback, (( RecurseConfig ) delegate ), ( detectLoops ? [] as Set : null ))
        }
    }


    /**
     * "File.metaClass.recurse" helper - handles directory provided.
     *
     * @param file         directory to handle
     * @param callback     callback to invoke
     * @param filter       file filter
     * @param fileType     type of callback file
     * @param filterType   type of filter callback file
     * @param stopOnFalse  whether recursive invocation should stop if callback invocation results in negative result
     * @param stopOnFilter whether recursive invocation should stop if filter type is "directory" and it returns false
     *
     * @return <code>false</code> if recursive iteration should be stopped,
     *         <code>true</code>  otherwise
     */
    @Requires({ directory.directory && callback && config })
    @Ensures({ result != null })
    private List<File> handleDirectory( File          directory,
                                        Closure<?>    callback,
                                        RecurseConfig config,
                                        Set<String>   directories )
    {
        def files = []

        if ( config.detectLoops && ( ! directories.add( directory.canonicalPath )))
        {
            log.info( "Loop detected - [$directory.canonicalPath] was already visited" )
            return files
        }

        for ( File f in directory.listFiles().findAll { it.exists() })
        {
            def result = invokeCallback( f, callback, config )

            if ( config.returnList && result.wasCalled ) { files << f }

            def recursiveInvoke = ( f.directory  &&
                                    (( ! config.stopOnFilter ) || ( config.filterType != FileType.DIRECTORIES ) || ( result.filterPass )) &&
                                    (( ! config.stopOnFalse  ) || ( result.invocationResult )))
            if ( recursiveInvoke )
            {
                files.addAll( handleDirectory( f, callback, config, directories ))
            }
        }

        files
    }


    /**
     * "File.metaClass.recurse" helper - invokes callback provided.
     *
     * @param file       file or directory to handle
     * @param callback   callback to invoke
     * @param filter     file filter
     * @param fileType   type of callback file
     * @param filterType type of filter callback file
     *
     * @return callback invocation result
     */
    private InvocationResult invokeCallback ( File          callbackFile,
                                              Closure<?>    callback,
                                              RecurseConfig config )
    {
        verify().exists( callbackFile )
        verify().notNull( callback, config )

        def fileTypeMatch       = file().typeMatch( config.fileType,   callbackFile )
        def filterTypeMatch     = file().typeMatch( config.filterType, callbackFile )
        def result              = new InvocationResult()
        result.filterPass       = (( config.filter == null ) || ( ! filterTypeMatch ) || config.filter( callbackFile ))
        result.invocationResult = true
        result.wasCalled        = false

        if ( fileTypeMatch && result.filterPass )
        {
            Object callbackResult   = callback( callbackFile )
            result.invocationResult = general().choose( callbackResult as boolean, true )
            result.wasCalled        = true
        }

        result
    }
}
