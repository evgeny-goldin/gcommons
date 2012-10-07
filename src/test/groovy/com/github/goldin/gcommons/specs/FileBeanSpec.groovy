package com.github.goldin.gcommons.specs
import com.github.goldin.gcommons.BaseSpec
import com.github.goldin.gcommons.GCommons
import com.github.goldin.spock.extensions.testdir.TestDir
import com.github.goldin.spock.extensions.with.With
import groovy.io.FileType
import groovy.util.logging.Slf4j
import org.gcontracts.annotations.Requires


/**
 * {@link com.github.goldin.gcommons.beans.FileBean} Spock tests.
 */
@Slf4j
@With({ GCommons.file() })
class FileBeanSpec extends BaseSpec
{
    /**
     * Supported archive extensions
     */
    private static final List<String> TEST_EXTENSIONS = [ 'sar', 'hpi', 'sima', 'zip', 'jar', 'tar', 'tar.gz' ]


    @SuppressWarnings( 'StatelessClass' )
    @TestDir File testDir


    @Requires({ testArchive && extension })
    def 'Check pack() and unpack() operations' ( Map.Entry<String, Long> testArchive,
                                                 String                  extension )
    {
        assert testDir.directory && ( ! testDir.listFiles())

        given:
        def zipFile    = testResource(      "${testArchive.key}.zip" )
        def zipUnpack1 = new File( testDir, 'zip-1' )
        def zipUnpack2 = new File( testDir, 'zip-2' )
        def extFile1   = new File( testDir, "${testArchive.key}-1.$extension" )
        def extFile2   = new File( testDir, "${testArchive.key}-2.$extension" )
        def extUnpack1 = new File( testDir, "$extension-1" )
        def extUnpack2 = new File( testDir, "$extension-2" )
        def fooFile    = new File( testDir, "${testArchive.key}.foo" )

        when:
        unpack( zipFile,    zipUnpack1,             true  )
        unpack( zipFile,    zipUnpack2,             false )

        pack  ( zipUnpack1, extFile1, [ '**' ], [], true  )
        pack  ( zipUnpack2, extFile2, [ '**' ], [], false )

        unpack( extFile1,   extUnpack1,             true  )
        unpack( extFile2,   extUnpack2,             false )

        extFile1.renameTo( fooFile )

        then:
        directorySize( zipUnpack1 ) == testArchive.value
        directorySize( zipUnpack2 ) == testArchive.value
        directorySize( extUnpack1 ) == testArchive.value
        directorySize( extUnpack2 ) == testArchive.value

        verifyBean.equal( zipUnpack1, zipUnpack2 )
        verifyBean.equal( zipUnpack2, extUnpack1 )
        verifyBean.equal( extUnpack1, extUnpack2 )
        verifyBean.equal( extUnpack2, zipUnpack1 )

        shouldFailAssert{ pack  ( zipUnpack1, fooFile )}.contains( 'unsupported archive extension "foo"' )
        shouldFailAssert{ unpack( fooFile, zipUnpack1 )}.contains( 'unsupported archive extension "foo"' )

        where:
        [ testArchive, extension ] << [ testArchives(), TEST_EXTENSIONS ].combinations()
    }


    @Requires({ testArchive && extension && prefix && fullpath })
    def 'Check "fullpath" and "prefix" pack() options'( Map.Entry<String, Long> testArchive,
                                                        String                  extension,
                                                        String                  prefix,
                                                        String                  fullpath )
    {
        assert testDir.directory && ( ! testDir.listFiles())

        given:
        def zipFile    = testResource(      "${testArchive.key}.zip" )
        def zipUnpack  = new File( testDir, 'zip' )
        def extFile1   = new File( testDir, "${testArchive.key}-1.$extension" )
        def extFile2   = new File( testDir, "${testArchive.key}-2.$extension" )
        def extFile3   = new File( testDir, "${testArchive.key}-3.$extension" )
        def extFile4   = new File( testDir, "${testArchive.key}-4.$extension" )
        def extUnpack1 = new File( testDir, "$extension-1" )
        def extUnpack2 = new File( testDir, "$extension-2" )
        def extUnpack3 = new File( testDir, "$extension-3" )
        def extUnpack4 = new File( testDir, "$extension-4" )

        when:
        unpack( zipFile,   zipUnpack )

        pack  ( zipUnpack, extFile1, [ '**' ],          [], true,  true, false, [], '', prefix )
        pack  ( zipUnpack, extFile2, [ '**' ],          [], false, true, false, [], '', prefix )
        pack  ( zipUnpack, extFile3, [ '**/LICENSE*' ], [], true,  true, false, [], fullpath   )
        pack  ( zipUnpack, extFile4, [ '**/LICENSE*' ], [], false, true, false, [], fullpath   )

        unpack( extFile1,  extUnpack1 )
        unpack( extFile2,  extUnpack2 )
        unpack( extFile3,  extUnpack3 )
        unpack( extFile4,  extUnpack4 )

        then:
        directorySize( zipUnpack  ) == testArchive.value
        directorySize( extUnpack1 ) == testArchive.value
        directorySize( extUnpack2 ) == testArchive.value

        verifyBean.equal( zipUnpack, new File( extUnpack1, prefix ))
        verifyBean.equal( zipUnpack, new File( extUnpack2, prefix ))

        verifyBean.file( new File( extUnpack3, fullpath ))
        verifyBean.file( new File( extUnpack4, fullpath ))

        shouldFailAssert { pack( zipUnpack, extFile1, [ '**' ], [], false, true, false, [], fullpath, prefix )}

        where:
        [ testArchive, extension, prefix, fullpath ] << [ testArchives(), TEST_EXTENSIONS, [ 'prefix-1/22/333' ], [ 'fullpath-1/22/333.txt' ]].
                                                        combinations()
    }


    @Requires({ extension && filemode })
    def 'Check "filemode" pack() option'( String extension, String filemode )
    {
        assert testDir.directory && ( ! testDir.listFiles())

        given:
        def testFiles         = [ '1.txt', '2.ppt', '3.php', '4.php' ]
        def unpackDir         = new File( testDir,   'unpack' )
        def shellFile         = new File( testDir,   'go.sh'  )
        def shellFileUnpacked = new File( unpackDir, shellFile.name )
        def archiveFile       = new File( unpackDir, "shell.$extension" )
        def result            = ''
        def ls                = ''

        when:
        if ( mac )
        {
            testFiles.each { write( new File( testDir, it )) }

            shellFile.write( '#!/bin/bash\nuname -ap' )
            assert unpackDir.with { directory || mkdirs() }
            pack( testDir, archiveFile, [ "*.sh|$filemode", '*.txt', '*.ppt', '*.php' ], [ '4.php' ] )

            generalBean.execute( "tar -xzf $archiveFile -C $unpackDir" )
            verifyBean.file( shellFileUnpacked )
            verifyBean.file(( testFiles - '4.php' ).collect { new File( unpackDir, it ) } as File[] )
            shouldFailAssert { verifyBean.file( new File( unpackDir, '4.php' ))}

            result = generalBean.executeWithResult( "bash  $shellFileUnpacked" )
            ls     = generalBean.executeWithResult( "ls -l $shellFileUnpacked" )

            log.info( "bash/ls = [$result][$ls]" )
        }

        then:
        ( ! mac ) || ( result.toLowerCase().contains( 'evgenyg-mac' ) && ls.startsWith( filemode == '700' ? '-rwx------' :
                                                                                        filemode == '750' ? '-rwxr-x---' :
                                                                                                            '-rwxr-xr-x' ))
        where:
        [ extension, filemode ] << [ 'tar tgz tar.gz'.tokenize(), '700 750 755'.tokenize()].combinations()
    }


    @Requires({ dir && file && path })
    def 'check relative path'( String dir, String file, String path )
    {
        assert testDir.directory && ( ! testDir.listFiles())

        expect:
        relativePath( new File( dir ), new File( file )) == path

        where:
        dir             | file                   | path
        'C:/111/'       | 'C:/111/222/sss/3.txt' | '/222/sss/3.txt'
        'C:/'           | 'C:/111/222/oiu/3.txt' | ( windows ? '' : '/' ) + '111/222/oiu/3.txt'
        'C:/'           | 'C:/111/222/oiu/3.txt' | ( windows ? '' : '/' ) + '111/222/oiu/3.txt'
        'C:/1/2/'       | 'C:/1/2/'              | '/'
        '1/2'           | '1/2/3'                | '/3'
        '1/2/'          | '1/2/3'                | '/3'
        '1'             | '1/2/3/rrr.txt'        | '/2/3/rrr.txt'
        '1/2'           | '1/2/3/rrr.txt'        | '/3/rrr.txt'
        '1/2/3'         | '1/2/3/rrr.txt'        | '/rrr.txt'
        '1/2/3/rrr.txt' | '1/2/3/rrr.txt'        | '/'
    }



    def 'gc-90: Recursive version of FileBean.copyDir()'()
    {
        assert testDir.directory && ( ! testDir.listFiles())

        given:
        def a = new File( testDir, 'a' )
        def b = new File( testDir, 'b' )

        when:
        write( new File( a, '1.txt' ))
        write( new File( a, '1/2.txt' ))
        write( new File( a, '1/2/3.txt' ))
        write( new File( a, '1/2/3/4.txt' ))
        assert new File( a, '1/2/3/5' ).mkdirs()
        assert new File( a, '1/aaaaa' ).mkdirs()

        copyDir( a, b )

        then:
        verifyBean.equal( a, b )
    }


    @Requires({ ( n > 0 ) })
    def 'gc-90: Recursive version of FileBean.copyDir() - random and patterns'( int     n,
                                                                                String  includes,
                                                                                String  excludes )
    {
        assert testDir.directory && ( ! testDir.listFiles())

        given:
        def a = new File( testDir, 'a' )
        def b = new File( testDir, 'b' )

        when:
        createRandomDirectory( a, n )
        copyDir( a, b, [ includes ], [ excludes ] )

        then:
        verifyBean.equal( a, b, false, includes )

        where:
        n           << [ 100,  300        ]
        includes    << [ '**', '**/*.txt' ]
        excludes    << [ '',   ''         ]
    }


    def 'gc-86: Ant scanner fix - set basedir with canonical path'( int n )
    {
        assert testDir.directory && ( ! testDir.listFiles())

        given:
        Set allFiles = [] as Set

        when:
        createRandomDirectory( testDir, n ) { File f -> allFiles << f }
        def allFiles1 = files( testDir )
        def allFiles2 = files( testDir, [ '**/*' ], [], true, true )

        then:
        allFiles2.size() > allFiles1.size()
        allFiles2.size() > allFiles.size()
        allFiles2.intersect( allFiles ).size() == allFiles.size()
        allFiles.intersect( allFiles2 ).size() == allFiles.size()

        allFiles.each { File f -> f.directory || allFiles1.contains( f ) }
        allFiles.each { File f -> allFiles2.contains( f ) }

        allFiles1.each{ File f -> allFiles.contains( f )}
        allFiles2.each{ File f -> allFiles.contains( f )}

        where:
        n << [ 100, 300 ]
    }


    def 'checking files()'()
    {
        assert testDir.directory && ( ! testDir.listFiles())

        given:
        def randomN      = random.nextInt( 30 ) + 30
        createRandomDirectory( testDir, randomN )
        def buildDir     = new File( constantsBean.USER_DIR_FILE, 'build' )
        def srcDir       = new File( constantsBean.USER_DIR_FILE, 'src' )
        def allFiles     = files( constantsBean.USER_DIR_FILE )
        def allFilesDirs = files( constantsBean.USER_DIR_FILE, null, null, true, true  )
        def noTestsFiles = files( constantsBean.USER_DIR_FILE, ['**/*.groovy','**/*.class'], [ '**/*Test*.*', '**/*Spec*.*' ] )
        def classFiles   = files( buildDir, [ '**/*.class'  ])
        def sources      = files( srcDir,   [ '**/*.groovy' ])
        def randomFiles  = files( testDir )
        def randomFilesD = files( testDir, [], [], false, true )


        expect:
        allFiles && allFiles.each{ verifyBean.file( it ) }
        allFiles == files( constantsBean.USER_DIR_FILE, [], [], true, false )
        ( allFiles.size() < allFilesDirs.size())
        ( allFilesDirs - allFiles ).each { verifyBean.directory( it )}

        classFiles.every{ it.name.endsWith( '.class'  ) }
        sources.every   { it.name.endsWith( '.groovy' ) }
        verifyBean.file( classFiles as File[] )
        verifyBean.file( sources    as File[] )
        classFiles.size() > sources.size()

        files( buildDir, [ '**/*.noSuchThing' ], [], true, false, false ).empty

        ! noTestsFiles.any { it.name.with{ contains( 'Test' ) || contains( 'Spec' ) }}
        noTestsFiles.every { it.name.with{ endsWith( '.groovy' ) || endsWith( '.class' ) }}

        shouldFailAssert { files( buildDir, ['**/*.noSuchThing'] )}

        randomFiles.each { verifyBean.file( it ) && it.name.endsWith( '.txt' )}
        ( randomFiles.size() <= randomN ) && ( randomFilesD.size() >= randomN )
        ( randomFilesD - randomFiles ).each { verifyBean.directory( it )}
    }


    def 'gc-98: FileBean.extension() - return empty String if file has no extension'( String fileName, String ext )
    {
        assert testDir.directory && ( ! testDir.listFiles())

        expect:
        extension( new File( fileName )) == ext

        where:
        fileName                | ext
        '1.txt'                 | 'txt'
        'C:/temp/2.pdf'         | 'pdf'
        '/temp/patch/2345.pdf'  | 'pdf'
        ''                      | ''
        '1'                     | ''
        'C:/temp/2'             | ''
        '/temp/patch/2345'      | ''
        '.zip'                  | 'zip'
        '.sth'                  | 'sth'
    }


    @Requires({ fileName && ext })
    def 'gc-100: FileBean.extension() returns extension lower-cased'( String fileName, String ext )
    {
        assert testDir.directory && ( ! testDir.listFiles())

        expect:
        extension( new File( fileName )) == ext

        where:
        fileName                | ext
        '1.txt'                 | 'txt'
        '1.MF '                 | 'MF '
        '1.MF'                  | 'MF'
        'META-INF/MANIFEST.MF'  | 'MF'
        'META-INF/MANIFEST.mf'  | 'mf'
        'META-INF/MANIFEST.Mf'  | 'Mf'
        'META-INF/MANIFEST.mF'  | 'mF'
        'C:/temp/2.tar.gz'      | 'tar.gz'
        'C:/temp/2.TAR.GZ'      | 'TAR.GZ'
        'C:/temp/2.TaR.Gz'      | 'TaR.Gz'
        'C:/temp/2.tar.bz2'     | 'tar.bz2'
        'C:/temp/2.tar.BZ2'     | 'tar.BZ2'
        'C:/temp/2.TAR.bz2'     | 'TAR.bz2'
        'C:/temp/2.tar.bz'      | 'bz'
        'C:/temp/2.tar.bz3'     | 'bz3'
    }


    @SuppressWarnings( 'ClosureAsLastMethodParameter' )
    def 'gc-101: FileBean.unpackZipEntries() - accept new parameter: zip entries to exclude - Maven' ( String testArchive )
    {
        assert testDir.directory && ( ! testDir.listFiles())

        given:
        def zipFile = testResource( "${testArchive}.zip" )
        def unpack1 = new File( testDir, 'unpack/1' )
        def unpack2 = new File( testDir, 'unpack/2' )
        def unpack3 = new File( testDir, 'unpack/3' )
        def unpack4 = new File( testDir, 'unpack/4' )
        def unpack5 = new File( testDir, 'unpack/5' )
        def unpack6 = new File( testDir, 'unpack/6' )

        when:
        unpackZipEntries( zipFile, unpack1, [ '**/*.jar' ] )
        unpackZipEntries( zipFile, unpack2, [], [ '**/*.jar' ] )
        unpackZipEntries( zipFile, unpack3, [ '**/*.jar' ], [ '**/m*.*' ] )
        unpackZipEntries( zipFile, unpack4, [ '**/*.jar' ], [ "$testArchive/lib/maven-core-3.0.1.jar" ] )
        unpackZipEntries( zipFile, unpack5, [ "$testArchive/lib/plexus-utils-2.0.4.jar" ], [ "$testArchive/lib/maven-core-3.0.1.jar" ] )
        unpackZipEntries( zipFile, unpack6, [ '**/*.exe', 'no-such-file', '**/*.jar' ], [ '**/m*.*' ], true, false )

        then:
        32      == recurse( unpack1, [ type: FileType.FILES ], { File f -> assert   f.name.endsWith( '.jar' )}).size()
        10      == recurse( unpack2, [ type: FileType.FILES ], { File f -> assert ! f.name.endsWith( '.jar' )}).size()
        21      == recurse( unpack3, [ type: FileType.FILES ], { File f -> assert   f.name.endsWith( '.jar' )}).size()
        31      == recurse( unpack4, [ type: FileType.FILES ], { File f -> assert   f.name.endsWith( '.jar' ) && ( ! f.name.contains( 'maven-core-3.0.1' )) }).size()
        1       == recurse( unpack5, [ type: FileType.FILES ], { File f -> assert   f.name == 'plexus-utils-2.0.4.jar' }).size()

        3301021 == directorySize( unpack1 )
        43306   == directorySize( unpack2 )
        1860093 == directorySize( unpack3 )
        2770664 == directorySize( unpack4 )
        222137  == directorySize( unpack5 )

        verifyBean.equal( unpack3, unpack6 )

        shouldFailAssert { unpackZipEntries( zipFile, unpack1, [ "$testArchive/lib/maven-core-3.0.1.jar" ], [ "$testArchive/lib/maven-core-3.0.1.jar" ] )}
        shouldFailAssert { unpackZipEntries( zipFile, unpack1, [ '**/*.bat' ], [ '**/m*'   ] )}

        where:
        testArchive << [ MAVEN_TEST_RESOURCE ]
    }


    @SuppressWarnings([ 'ClosureAsLastMethodParameter', 'AbcComplexity' ])
    def 'gc-101: FileBean.unpackZipEntries() - accept new parameter: zip entries to exclude - Gradle' ( String testArchive )
    {
        assert testDir.directory && ( ! testDir.listFiles())

        given:
        def zipFile = testResource( "${testArchive}.zip" )
        def unpack1 = new File( testDir, 'unpack/1' )
        def unpack2 = new File( testDir, 'unpack/2' )
        def unpack3 = new File( testDir, 'unpack/3' )
        def unpack4 = new File( testDir, 'unpack/4' )
        def unpack5 = new File( testDir, 'unpack/5' )
        def unpack6 = new File( testDir, 'unpack/6' )
        def unpack7 = new File( testDir, 'unpack/7' )
        def unpack8 = new File( testDir, 'unpack/8' )

        when:
        unpackZipEntries( zipFile, unpack1, [ '**/*.jar' ] )
        unpackZipEntries( zipFile, unpack2, [], [ '**/*.html', '**/*.groovy', '**/*.java' ] )
        unpackZipEntries( zipFile, unpack3, [ '**/*.jar', '**/*.html', '**/*.groovy' ], [ '**/*d*', '**/*e*' ] )
        unpackZipEntries( zipFile, unpack4, [ '**/*.html' ], [ '**/docs/**' ] )
        unpackZipEntries( zipFile, unpack5, [ "$testArchive/src/org/gradle/api/**/*.groovy" ], [ "$testArchive/src/org/gradle/**/internal/**" ] )
        unpackZipEntries( zipFile, unpack6, [ "$testArchive/src/org/gradle/**" ],              [ '**/*.groovy', '**/*.java' ] )
        unpackZipEntries( zipFile, unpack7, [ "$testArchive/src/org/gradle/**" ],              [ '**/*.groovy', '**/*.java', '**/*.txt', '**/*.png', '**/*.xml' ] )
        unpackZipEntries( zipFile, unpack8, [ '**/*.dll', 'no-such-file', '**/*.html' ], [ '**/docs/**' ], true, false )

        then:
        96       == recurse( unpack1, [ type: FileType.FILES ], { File f -> assert   f.name.endsWith( '.jar' )}).size()
        510      == recurse( unpack2, [ type: FileType.FILES ], { File f -> assert ! [ 'html', 'groovy', 'java' ].any { f.name.endsWith( ".$it" ) }}).size()
        108      == recurse( unpack3, [ type: FileType.FILES ], { File f -> assert ! [ 'd', 'e' ].any { f.name.contains( it ) }}).size()
        7        == recurse( unpack4, [ type: FileType.FILES ], { File f -> assert f.name.endsWith( '.html'   ) && ( ! f.path.contains ( 'docs'     )) }).size()
        32       == recurse( unpack5, [ type: FileType.FILES ], { File f -> assert f.name.endsWith( '.groovy' ) && ( ! f.path.contains ( 'internal' )) }).size()
        29       == recurse( unpack6, [ type: FileType.FILES ], { File f -> assert [ 'gradle', 'html', 'png', 'properties', 'txt', 'xml' ].any{ f.name.endsWith( ".$it" ) }}).size()
        4        == recurse( unpack7, [ type: FileType.FILES ], { File f -> assert [ 'gradle', 'html', 'properties' ].any{ f.name.endsWith( ".$it" ) }}).size()

        9435052  == directorySize( unpack1 )
        11185949 == directorySize( unpack2 )
        3578296  == directorySize( unpack3 )
        33352    == directorySize( unpack4 )
        88130    == directorySize( unpack5 )
        43630    == directorySize( unpack6 )
        6033     == directorySize( unpack7 )

        verifyBean.equal( unpack4, unpack8 )

        shouldFailAssert { unpackZipEntries( zipFile, unpack1, [ '**/*.html' ],       [ '**/*.html'     ] )}
        shouldFailAssert { unpackZipEntries( zipFile, unpack1, [ '**/*.groovy' ],     [ '**/*r*'        ] )}
        shouldFailAssert { unpackZipEntries( zipFile, unpack1, [ '**/build.gradle' ], [ '**/samples/**' ] )}

        where:
        testArchive << [ GRADLE_TEST_RESOURCE ]
    }


    @SuppressWarnings( 'ClosureAsLastMethodParameter' )
    def 'gc-99/gc-45: recurse() - return list of files iterated/make sure no values are left in config' ()
    {
        assert testDir.directory && ( ! testDir.listFiles())

        given:
        def counter = 0
        def nFiles  = createRandomDirectory( testDir, 1000 ) { File f -> counter += ( f.file ? 1 : 0 ) }

        expect:
        counter == nFiles
        recurse( testDir, [ type: FileType.FILES       ], { File f -> assert f.file      }).size() == nFiles
        recurse( testDir, [ type: FileType.DIRECTORIES ], { File f -> assert f.directory }).size() >  nFiles
        recurse( testDir, [ type: FileType.ANY         ], {}                              ).size() >  nFiles

        recurse( testDir, [ type: FileType.FILES,       returnList: false ], {}).empty
        recurse( testDir, [ type: FileType.DIRECTORIES, returnList: false ], {}).empty

        recurse( testDir, [ type: FileType.FILES       ], { File f -> assert f.file      }).every { File f -> f.file      }
        recurse( testDir, [ type: FileType.DIRECTORIES ], { File f -> assert f.directory }).every { File f -> f.directory }

        shouldFailAssert { recurse( testDir, [ something: 'anything' ], {} )}
        shouldFailAssert { recurse( testDir, [ tyype    : 'typo'     ], {} )}
    }


    @SuppressWarnings( 'ClosureAsLastMethodParameter' )
    def "gc-104: FileBean.files() fails when directory specified doesn't exist even if 'failIfNotFound' is 'false'" ()
    {
        assert testDir.directory && ( ! testDir.listFiles())

        expect:
        [] == files( new File( 'no-such-directory' ), [ '**' ],       [],       true, false, false )
        [] == files( new File( 'no-such-directory' ), [ '**/*.exe' ], [],       true, false, false )
        [] == files( new File( 'no-such-directory' ), [],             [ '**' ], true, false, false )

        [] == files( constantsBean.USER_DIR_FILE, [ 'no-such-file.txt' ],  [],  true, false, false )
        [] == files( constantsBean.USER_DIR_FILE, [ '**/*no-such-file*' ], [],  true, false, false )
        [] == files( constantsBean.USER_DIR_FILE, [], [ '**' ],                 true, false, false )
    }



    def "gc-114: FileBean.baseName() - retrieve file base name" ( String fileName, String bName )
    {
        expect:
        baseName( new File( fileName )) == bName

        where:
        fileName                | bName
        '1.txt'                 | '1'
        'C:/temp/2.pdf'         | '2'
        '/temp/patch/2345.pdf'  | '2345'
        ''                      | ''
        '1'                     | '1'
        'C:/temp/2'             | '2'
        '/temp/patch/2345'      | '2345'
        '.zip'                  | ''
        '.sth'                  | ''
        'oppa.aaa'              | 'oppa'
        'oppa.a'                | 'oppa'
        'oppa'                  | 'oppa'
        'oppa.zip'              | 'oppa'
        'zip.zip'               | 'zip'
        'q'                     | 'q'
        'q.w'                   | 'q'
    }


    def "gc-115: FileBean.pack() - allow to set compression level when using Ant"( String testArchive )
    {
        given:
        final unpackDir = new File( testDir, 'unpack' )
        final zip1      = new File( testDir, '1.zip'  )
        final zip2      = new File( testDir, '2.zip'  )
        final zip3      = new File( testDir, '3.zip'  )
        final zip4      = new File( testDir, '4.zip'  )

        unpack( testResource( "${ testArchive }.zip" ), unpackDir )

        pack( unpackDir, zip1, [ '**' ], [], false, true, false, null, null, null, true, null, 0 )
        pack( unpackDir, zip2, [ '**' ], [], false, true, false, null, null, null, true, null, 5 )
        pack( unpackDir, zip3, [ '**' ], [], false, true, false, null, null, null, true, null, 9 )

        expect:
        zip1.file && zip2.file && zip3.file

        zip1.size() > zip2.size()
        zip2.size() > zip3.size()

        shouldFailAssert { fileBean.pack( unpackDir, zip4, [ '**' ], [], false, true, false, null, null, null, true, null, -1 ) }
        shouldFailAssert { fileBean.pack( unpackDir, zip4, [ '**' ], [], false, true, false, null, null, null, true, null, 10 ) }
        shouldFailAssert { shouldFailAssert { fileBean.pack( unpackDir, zip4, [ '**' ], [], false, true, false, null, null, null, true, null, 5 ) }}

        where:
        testArchive << testArchives().keySet()
    }


    def "gc-118: Support *.gz archives unpack"()
    {
        given:
        final gzip = testResource( 'faa-1.gz' )

        when:
        GCommons.file().unpack( gzip, testDir, false )

        then:
        GCommons.verify().file( new File( testDir, 'faa-1' )).name == 'faa-1'
    }
}
