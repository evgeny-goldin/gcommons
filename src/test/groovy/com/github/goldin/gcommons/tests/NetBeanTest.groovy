package com.github.goldin.gcommons.tests

import com.github.goldin.gcommons.BaseTest
import com.github.goldin.gcommons.beans.GFTPFile
import groovy.util.logging.Slf4j
import org.apache.commons.net.ftp.FTPFile
import org.junit.Test


/**
 * {@link com.github.goldin.gcommons.beans.NetBean} tests
 */
@Slf4j
class NetBeanTest extends BaseTest
{
    private static final String  FTP_DIR  = '/public_ftp'
    private static final String  FTP_PATH = "ftp://geniekho:I46O4l4bbt@geniek.host-ed.me:$FTP_DIR/"

    @Test
    void shouldParseNetworkPath()
    {
        netBean.with {
            def map = parseNetworkPath( 'ftp://someUser:somePassword@someServer:/somePath' )
            assert [ 'ftp', 'someUser', 'somePassword', 'someServer', '/somePath' ] ==
                   [ map.protocol, map.username, map.password, map.host, map.directory ]

            map = parseNetworkPath( 'scp://another.user:strange@passw@rd@aaa.server.com:/' )
            assert [ 'scp', 'another.user', 'strange@passw@rd', 'aaa.server.com', '/' ] ==
                   [ map.protocol, map.username, map.password, map.host, map.directory ]

            map = parseNetworkPath( 'http://another.-weir.d.user:even-more.!strange@passw@rd@address.server.com:path' )
            assert [ 'http', 'another.-weir.d.user', 'even-more.!strange@passw@rd', 'address.server.com', 'path' ] ==
                   [ map.protocol, map.username, map.password, map.host, map.directory ]
        }
    }


    @Test
    void shouldRecognizeNetworkPath()
    {
        netBean.with {
            assert   isFtp( 'ftp://user' )
            assert   isFtp( 'ftp://user',  'ftp://user' )
            assert   isFtp( 'ftp://user',  'ftp://user', 'ftp://user' )
            assert ! isFtp( 'ftp://user',  'ftp://user', 'ftp://user', 'ftp1://user' )
            assert ! isFtp( 'ftp2://user', 'ftp://user', 'ftp://user', 'ftp://user' )
            assert ! isFtp( 'scp://user',  'ftp://user', 'ftp://user', 'ftp://user' )
            assert ! isFtp( ' ftp://user', 'ftp://user', 'ftp://user' )
            assert   isFtp( 'ftp://user:password@host:path' )
            assert ! isFtp( 'stp://user:password@host:path' )
            assert ! isFtp( 'scp://user:password@host:path' )

            assert   isScp( 'scp://user' )
            assert   isScp( 'scp://user', 'scp://user' )
            assert   isScp( 'scp://user', 'scp://user', 'scp://user' )
            assert ! isScp( ' scp://user', 'scp://user', 'scp://user' )
            assert ! isScp( 'http://user', 'scp://user', 'scp://user' )
            assert ! isScp( 'scp://user', 'scp ://user' )
            assert   isScp( 'scp://user:password@host:path' )
            assert ! isScp( 'ftp://user:password@host:path' )

            assert   isHttp( 'http://user' )
            assert   isHttp( 'http://user', 'http://user' )
            assert   isHttp( 'http://user', 'http://user', 'http://user' )
            assert ! isHttp( 'http ://user', 'http://user', 'http://user' )
            assert ! isHttp( 'http://user', 'scp://user', 'http://user' )
            assert ! isHttp( 'http://user', 'http://user', 'ftp://user' )
            assert   isHttp( 'http://user:password@host:path' )
            assert ! isHttp( 'htp://user:password@host:path' )

            assert   isNet( 'http://user:password@host:path' )
            assert   isNet( 'scp://user', 'ftp://user' )
            assert   isNet( 'scp://user' )
            assert   isNet( 'ftp://user' )
            assert   isNet( 'http://user:password@host:path', 'scp://user', 'ftp://user' )
            assert ! isNet( 'htp://user:password@host:path', 'scp://user', 'ftp://user' )
            assert ! isNet( 'http://user:password@host:path', 'scp ://user', 'ftp://user' )
            assert ! isNet( 'http://user:password@host:path', 'scp://user', 'fttp://user' )

            assert ! isFtp()
            assert ! isFtp( null )
            assert ! isScp()
            assert ! isScp( null )
            assert ! isHttp()
            assert ! isHttp( null )
            assert ! isNet()
            assert ! isNet( null )
        }
    }


    @Test
    void shouldMatchNetworkPattern()
    {
        constantsBean.with {
            assert FTP_PATH ==~ NETWORK_PATTERN
            assert FTP_PATH  =~ NETWORK_PATTERN
            assert 'ftp://user:password@server.com:/pa'    ==~ NETWORK_PATTERN
            assert 'ftp://user:password@server.com:/'       =~ NETWORK_PATTERN
            assert 'http://user:password@server.com:/pat'  ==~ NETWORK_PATTERN
            assert 'http://user:password@server.com:/path'  =~ NETWORK_PATTERN
            assert 'scp://user:password@server.com:/path'  ==~ NETWORK_PATTERN
            assert 'scp://user:password@server.com:/path'   =~ NETWORK_PATTERN
        }
    }


    @Test
    void shouldListFtpFiles()
    {
        def htmlFiles = netBean.listFiles( FTP_PATH, ['*.html'] )
        def indexFile = netBean.listFiles( FTP_PATH, ['index.html'] )
        def jarFiles  = netBean.listFiles( FTP_PATH, ['apache-maven-3.0.1/lib/*.jar'] )
        def txtFiles  = netBean.listFiles( FTP_PATH, ['apache-maven-3.0.1/*.txt'] )

        assert 1 == htmlFiles.size()
        assert 1 == indexFile.size()
        assert indexFile[ 0 ].name     == 'index.html'
        assert indexFile[ 0 ].fullPath.endsWith( '/index.html' )
        assert indexFile[ 0 ].fullPath == "${FTP_PATH}index.html"
        assert indexFile[ 0 ].path     == "$FTP_DIR/index.html"
        assert indexFile[ 0 ].size     == 1809

        assert jarFiles.size() == 5
        assert jarFiles*.name  == [ 'apache-maven-3.0.1/lib/wagon-file-1.0-beta-7.jar',
                                    'apache-maven-3.0.1/lib/wagon-http-lightweight-1.0-beta-7.jar',
                                    'apache-maven-3.0.1/lib/wagon-http-shared-1.0-beta-7.jar',
                                    'apache-maven-3.0.1/lib/wagon-provider-api-1.0-beta-7.jar',
                                    'apache-maven-3.0.1/lib/xercesMinimal-1.9.6.2.jar' ]
        assert jarFiles*.fullPath.every { it.startsWith( 'ftp://' ) && it.endsWith( '.jar' ) }

        assert jarFiles[ -2 ].fullPath.endsWith( '/apache-maven-3.0.1/lib/wagon-provider-api-1.0-beta-7.jar' )
        assert jarFiles[ -2 ].fullPath == "${FTP_PATH}apache-maven-3.0.1/lib/wagon-provider-api-1.0-beta-7.jar"

        assert jarFiles[ -1 ].fullPath.endsWith( '/apache-maven-3.0.1/lib/xercesMinimal-1.9.6.2.jar' )
        assert jarFiles[ -1 ].fullPath == "${FTP_PATH}apache-maven-3.0.1/lib/xercesMinimal-1.9.6.2.jar"

        assert jarFiles[ -2 ].path == "$FTP_DIR/apache-maven-3.0.1/lib/wagon-provider-api-1.0-beta-7.jar"
        assert jarFiles[ -1 ].path == "$FTP_DIR/apache-maven-3.0.1/lib/xercesMinimal-1.9.6.2.jar"
        assert jarFiles*.size  == [ 11063, 14991, 25516, 53227, 39798 ]

        assert txtFiles.size() == 3
        assert txtFiles*.name  == [ 'apache-maven-3.0.1/LICENSE.txt',
                                    'apache-maven-3.0.1/NOTICE.txt',
                                    'apache-maven-3.0.1/README.txt' ]
        assert txtFiles*.fullPath.every { it.startsWith( 'ftp://' ) && it.endsWith( '.txt' ) }

        assert txtFiles[ -2 ].fullPath.endsWith( '/apache-maven-3.0.1/NOTICE.txt' )
        assert txtFiles[ -2 ].fullPath == "${FTP_PATH}apache-maven-3.0.1/NOTICE.txt"

        assert txtFiles[ -1 ].fullPath.endsWith( '/apache-maven-3.0.1/README.txt' )
        assert txtFiles[ -1 ].fullPath == "${FTP_PATH}apache-maven-3.0.1/README.txt"

        assert txtFiles[ -2 ].path == "$FTP_DIR/apache-maven-3.0.1/NOTICE.txt"
        assert txtFiles[ -1 ].path == "$FTP_DIR/apache-maven-3.0.1/README.txt"
        assert txtFiles*.size  == [ 11560, 1030, 2559 ]
    }


    @Test
    void shouldListFtpFilesWithMultiplePatterns()
    {
        def files = netBean.listFiles( FTP_PATH, [ '*.html', '*.zip', '*.jar', '*.xml' ] )
        assert files.size() == 5
        assert [ 'index.html', 'net.zip', 'net2.zip', 'net.jar', 'pom.xml' ].every { String filename -> files.any { GFTPFile file -> file.name.endsWith( filename ) }}
    }


    @Test
    void shouldListFtpFilesWithExcludes()
    {
        def fileNames = [ 'wagon-file-1.0-beta-7.jar', 'wagon-provider-api-1.0-beta-7.jar', 'xercesMinimal-1.9.6.2.jar' ]
        def files1    = netBean.listFiles( FTP_PATH, ['apache-maven-3.0.1/lib/*.jar'],      [ '**/wagon-http*' ] )
        def files2    = netBean.listFiles( FTP_PATH, [ '**/lib/*.jar'], [ 'apache-maven-3.0.1/lib/wagon-http-lightweight-1.0-beta-7.jar',
                                                                           'apache-maven-3.0.1/lib/wagon-http-shared-1.0-beta-7.jar' ] )
        def files3    = netBean.listFiles( FTP_PATH + 'apache-maven-3.0.1', [ 'lib/*.jar'], [ 'lib/wagon-http-*.jar' ] )
        def files4    = netBean.listFiles( FTP_PATH + 'apache-maven-3.0.1', [ '**/*.jar'],  [ 'boot/**', '**/lib/wagon-h*.jar' ] )

        assert [ files1, files2, files3, files4 ].every {
            List<FTPFile> files ->
            ( files.size() == fileNames.size()) &&
            fileNames.each{    String fileName -> files*.name.any{ it.endsWith( fileName ) }} &&
            files*.name.every{ String fileName -> fileNames.any{ fileName.endsWith( it )   }}
        }
    }


    @Test
    void shouldListFtpDirectories()
    {
        def    files = netBean.listFiles( FTP_PATH, [ '*' ], [], 5, true )
        assert files.size() == 8
        assert files.findAll { it.directory }.size() == 3
        assert [ 'apache-maven-3.0.1', 'incoming' ].every{ String dirName -> files.any{ it.name.endsWith( dirName ) }}

        files = netBean.listFiles( FTP_PATH, [ 'apache-maven-3.0.1/*' ], [], 5, true )
        assert files.size() == 7
        assert files.findAll { it.directory }.size() == 4
        assert files.findAll { it.directory }.every { it.path.contains( '/apache-maven-3.0.1/' ) }

        files = netBean.listFiles( FTP_PATH, [ '*' ] )
        assert files.size() == 5
    }
}
