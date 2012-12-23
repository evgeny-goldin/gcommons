package com.github.goldin.gcommons.beans

import static com.github.goldin.gcommons.GCommons.*
import groovy.util.logging.Slf4j
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires

import java.util.regex.Matcher


/**
 * Network-related helper methods.
 */
@Slf4j
class NetBean extends BaseBean
{
    boolean isHttp ( String ... s ) { s && s.every{ it && it.toLowerCase().startsWith( 'http:' ) }}
    boolean isScp  ( String ... s ) { s && s.every{ it && it.toLowerCase().startsWith( 'scp:'  ) }}
    boolean isFtp  ( String ... s ) { s && s.every{ it && it.toLowerCase().startsWith( 'ftp:'  ) }}
    boolean isNet  ( String ... s ) { s && s.every{ isHttp( it ) || isScp( it ) || isFtp( it )   }}


    /**
     * Parses network path in the following format:
     * {@code "(http|scp|ftp)://user:password@server:/path/to/file"}
     *
     * @param path network path to parse
     * @return map with following entries: "protocol", "username", "password", "host", "directory"
     */
    @Requires({ path })
    @Ensures ({ result.protocol && result.username && result.password && result.host && result.directory })
    Map<String, String> parseNetworkPath( String path )
    {
        assert isNet( verify().notNullOrEmpty( path ))
        Matcher matcher = ( path =~ constants().NETWORK_PATTERN )

        assert ( matcher.find() && ( matcher.groupCount() == 6 )),
               "Unable to parse [$path] as network path: should be in format " +
               "\"protocol://user:password@host:path\" or \"protocol://user:password@host:port:path\""

        def ( String protocol, String username, String password, String host, String port, String directory ) =
            matcher[ 0 ][ 1 .. 6 ]

        verify().notNullOrEmpty( protocol, username, password, host, directory )

        [
            protocol  : protocol,
            username  : username,
            password  : password,
            host      : host,
            directory : directory.replace( '\\', '/' )
        ] + ( port ? [ port : port ] : [:] )
    }


    /**
     * Initializes and connects an {@link FTPClient} using remote path specified of form:
     * {@code ftp://<user>:<password>@<host>:<path>}
     *
     * @param remotePath remote path to establish ftp connection to: {@code ftp://<user>:<password>@<host>:<path>}
     * @return client instance initialized and connected to FTP server specified
     */
    FTPClient ftpClient( String remotePath )
    {
        Map       data   = parseNetworkPath( remotePath )
        FTPClient client = new FTPClient()

        log.info( "Connecting to FTP server [$data.host:$data.directory] as [$data.username] .." )

        try
        {
            client.connect( data.host )
            int reply = client.replyCode
            assert FTPReply.isPositiveCompletion( reply ),          "Failed to connect to FTP server [$data.host], reply code is [$reply]"
            assert client.login( data.username, data.password ),    "Failed to connect to FTP server [$data.host] as [$data.username]"
            assert client.changeWorkingDirectory( data.directory ), "Failed to change FTP server [$data.host] directory to [$data.directory]"
            client.fileType = FTP.BINARY_FILE_TYPE
            client.enterLocalPassiveMode()
        }
        catch ( Throwable t )
        {
            client.logout()
            client.disconnect()
            throw new RuntimeException( "Failed to connect to FTP server [$remotePath]: $t", t )
        }

        log.info( "Connected to FTP server [$data.host:$data.directory] as [$data.username]. " +
                  "Remote system is [$client.systemName], status is [$client.status]" )
        client
    }


    /**
     * Initializes and connects an {@link FTPClient} using remote path specified of form:
     * {@code ftp://<user>:<password>@<host>:<path>}. When connected, invokes the closure specified, passing
     * it {@link FTPClient} instance connected, and disconnects the client.
     *
     * @param remotePath remote path to establish ftp connection to: {@code ftp://<user>:<password>@<host>:<path>}
     * @param resultType closure expected result type,
     *                   if <code>null</code> - result type check is not performed
     * @param c closure to invoke and pass {@link FTPClient} instance
     * @return closure invocation result
     */
    @SuppressWarnings( 'UnnecessaryPublicModifier' )
    public <T> T ftpClient( String remotePath, Class<T> resultType, Closure c )
    {
        verify().notNullOrEmpty( remotePath )
        verify().notNull( c, resultType )

        FTPClient client = null

        try
        {
            client = ftpClient( remotePath )
            return general().tryIt( 1, resultType ){ c( client ) }
        }
        finally
        {
            if ( client )
            {
                client.logout()
                client.disconnect()
            }
        }
    }


    /**
     * Lists files on the FTP server specified.
     *
     * @param remotePath      remote path to establish ftp connection to: <code>"ftp://<user>:<password>@<host>:<path>"</code>
     * @param globPatterns    glob patterns of files to list: <code>"*.*"</code> or "<code>*.zip"</code>
     * @param excludes        exclude patterns of files to exclude, empty by default
     * @param tries           number of attempts, <code>5</code> by default
     * @param listDirectories whether directories should be returned in result, <code>false</code> by default
     *
     * @return FTP files listed by remote FTP server using glob patterns specified
     */
    List<GFTPFile> listFiles( String       remotePath,
                              List<String> globPatterns    = [ '*' ],
                              List<String> excludes        = null,
                              int          tries           = 10,
                              boolean      listDirectories = false )
    {
        verify().notNullOrEmpty( remotePath )
        assert tries > 0

        /**
         * Trying "tries" times to list files
         */
        general().tryIt( tries, List ){
            /**
             * Getting a list of files for remote path
             */
            ftpClient( remotePath, List )
            {
                FTPClient client ->

                List<GFTPFile> result = []

                log.info( "Listing $globPatterns${ excludes ? '/' + excludes : '' } files .." )

                for ( String globPattern in globPatterns*.trim().collect{ verify().notNullOrEmpty( it ) } )
                {
                    List<GFTPFile> gfiles = client.listFiles( globPattern ).
                                            findAll { it != null }.
                                            findAll { FTPFile  file -> (( file.name != '.' ) && ( file.name != '..' )) }.
                                            collect { FTPFile  file -> new GFTPFile( file, remotePath, globPattern ) }.
                                            findAll { GFTPFile file -> listDirectories ? true /* all entries */ : ( ! file.directory ) /* files */ }.
                                            findAll { GFTPFile file -> ( ! excludes.any{ String exclude -> general().match( file.name, exclude ) ||
                                                                                                           exclude.endsWith( file.name ) } ) }

                    log.info( "[$globPattern] - [${ gfiles.size() }] file${ general().s( gfiles.size() ) }" )
                    if ( log.isDebugEnabled()) { log.debug( '\n' + general().stars( gfiles*.path ))}

                    result.addAll( gfiles )
                }

                log.info( "[${ result.size() }] file${ general().s( result.size()) }" )
                result
            }
        }
    }
}


/**
 * {@link FTPFile} extension providing file's full {@code "ftp://user:pass@server:/path"} and remote path {@code "/path"}.
 */
class GFTPFile
{
    @Delegate FTPFile file

    String  fullPath
    String  path
    boolean directory

    GFTPFile ( FTPFile file, String remotePath, String globPattern )
    {
        assert ( file != null ) && file.name && remotePath, "File [$file], name [$file.name], path [$remotePath] - should be defined"

        this.file       = file
        def patternPath = globPattern.replace( '\\', '/' ).replaceFirst( /^\//, '' ).replaceAll( /\/?[^\/]+$/, '' ) // "/aaaa/bbbb/*.zip" => "aaaa/bbbb"
        def filePath    = "${ file.name.startsWith( patternPath ) ? '' : patternPath + '/' }$file.name"             // "aaaa/bbbb/file.zip"
        this.fullPath   = "$remotePath/$filePath".replace( '\\', '/' ).replaceAll( /(?<!ftp:)\/+/, '/' )            // "ftp://user:pass@server:/path/aaaa/bbbb/file.zip"
        this.path       = this.fullPath.replaceAll( /.+:/, '' )                                                     // "/path/aaaa/bbbb/file.zip"
        this.directory  = file.rawListing.startsWith( 'd' )
    }


    @Override
    String toString() { "[${ this.rawListing }][${ this.path }]" }
}
