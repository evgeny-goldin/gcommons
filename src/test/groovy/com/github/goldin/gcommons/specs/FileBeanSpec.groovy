package com.github.goldin.gcommons.specs

import com.github.goldin.gcommons.BaseSpec
import com.github.goldin.gcommons.GCommons
import com.github.goldin.gcommons.beans.FileBean
import com.github.goldin.spock.extensions.testdir.TestDir
import groovy.io.FileType
import groovy.util.logging.Slf4j
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * {@link com.github.goldin.gcommons.beans.FileBean} Spock tests.
 */
@Slf4j
class FileBeanSpec extends BaseSpec
{
    /**
     * Supported archive extensions
     */
    private static final List<String> TEST_EXTENSIONS = defaultExtensions()


    @Ensures ({ result })
    static List<String> defaultExtensions()
    {
        final defaultExtensions = FileBean.class.classLoader.getResource( 'META-INF/services/de.schlichtherle.io.registry.properties' ).
                                  getText( 'UTF-8' ).readLines().first().
                                  tokenize( '=' )*.trim().tail().head().
                                  tokenize( '|' )
        defaultExtensions
    }

    private final FileBean fileBean = GCommons.file()


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
        fileBean.unpack( zipFile,    zipUnpack1,             true  )
        fileBean.unpack( zipFile,    zipUnpack2,             false )

        fileBean.pack  ( zipUnpack1, extFile1, [ '**' ], [], true  )
        fileBean.pack  ( zipUnpack2, extFile2, [ '**' ], [], false )

        fileBean.unpack( extFile1,   extUnpack1,             true  )
        fileBean.unpack( extFile2,   extUnpack2,             false )

        extFile1.renameTo( fooFile )

        then:
        fileBean.directorySize( zipUnpack1 ) == testArchive.value
        fileBean.directorySize( zipUnpack2 ) == testArchive.value
        fileBean.directorySize( extUnpack1 ) == testArchive.value
        fileBean.directorySize( extUnpack2 ) == testArchive.value

        verifyBean.equal( zipUnpack1, zipUnpack2 )
        verifyBean.equal( zipUnpack2, extUnpack1 )
        verifyBean.equal( extUnpack1, extUnpack2 )
        verifyBean.equal( extUnpack2, zipUnpack1 )

        shouldFailAssert{ fileBean.pack  ( zipUnpack1, fooFile )}.contains( 'unsupported archive extension "foo"' )
        shouldFailAssert{ fileBean.unpack( fooFile, zipUnpack1 )}.contains( 'unsupported archive extension "foo"' )

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
        fileBean.unpack( zipFile,   zipUnpack )

        fileBean.pack  ( zipUnpack, extFile1, [ '**' ],          [], true,  true, false, [], '', prefix )
        fileBean.pack  ( zipUnpack, extFile2, [ '**' ],          [], false, true, false, [], '', prefix )
        fileBean.pack  ( zipUnpack, extFile3, [ '**/LICENSE*' ], [], true,  true, false, [], fullpath   )
        fileBean.pack  ( zipUnpack, extFile4, [ '**/LICENSE*' ], [], false, true, false, [], fullpath   )

        fileBean.unpack( extFile1,  extUnpack1 )
        fileBean.unpack( extFile2,  extUnpack2 )
        fileBean.unpack( extFile3,  extUnpack3 )
        fileBean.unpack( extFile4,  extUnpack4 )

        then:
        fileBean.directorySize( zipUnpack  ) == testArchive.value
        fileBean.directorySize( extUnpack1 ) == testArchive.value
        fileBean.directorySize( extUnpack2 ) == testArchive.value

        verifyBean.equal( zipUnpack, new File( extUnpack1, prefix ))
        verifyBean.equal( zipUnpack, new File( extUnpack2, prefix ))

        verifyBean.file( new File( extUnpack3, fullpath ))
        verifyBean.file( new File( extUnpack4, fullpath ))

        shouldFailAssert { fileBean.pack( zipUnpack, extFile1, [ '**' ], [], false, true, false, [], fullpath, prefix )}

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
            fileBean.pack( testDir, archiveFile, [ "*.sh|$filemode", '*.txt', '*.ppt', '*.php' ], [ '4.php' ] )

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
        fileBean.relativePath( new File( dir ), new File( file )) == path

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

        fileBean.copyDir( a, b )

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
        fileBean.copyDir( a, b, [ includes ], [ excludes ] )

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
        def allFiles1 = fileBean.files( testDir )
        def allFiles2 = fileBean.files( testDir, [ '**/*' ], [], true, true )

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
        def allFiles     = fileBean.files( constantsBean.USER_DIR_FILE )
        def allFilesDirs = fileBean.files( constantsBean.USER_DIR_FILE, null, null, true, true  )
        def noTestsFiles = fileBean.files( constantsBean.USER_DIR_FILE, ['**/*.groovy','**/*.class'], [ '**/*Test*.*', '**/*Spec*.*' ] )
        def classFiles   = fileBean.files( buildDir, [ '**/*.class'  ])
        def sources      = fileBean.files( srcDir,   [ '**/*.groovy' ])
        def randomFiles  = fileBean.files( testDir )
        def randomFilesD = fileBean.files( testDir, [], [], false, true )


        expect:
        allFiles && allFiles.each{ verifyBean.file( it ) }
        allFiles == fileBean.files( constantsBean.USER_DIR_FILE, [], [], true, false )
        ( allFiles.size() < allFilesDirs.size())
        ( allFilesDirs - allFiles ).each { verifyBean.directory( it )}

        classFiles.every{ it.name.endsWith( '.class'  ) }
        sources.every   { it.name.endsWith( '.groovy' ) }
        verifyBean.file( classFiles as File[] )
        verifyBean.file( sources    as File[] )
        classFiles.size() > sources.size()

        fileBean.files( buildDir, [ '**/*.noSuchThing' ], [], true, false, false ).empty

        ! noTestsFiles.any { it.name.with{ contains( 'Test' ) || contains( 'Spec' ) }}
        noTestsFiles.every { it.name.with{ endsWith( '.groovy' ) || endsWith( '.class' ) }}

        shouldFailAssert { fileBean.files( buildDir, ['**/*.noSuchThing'] )}

        randomFiles.each { verifyBean.file( it ) && it.name.endsWith( '.txt' )}
        ( randomFiles.size() <= randomN ) && ( randomFilesD.size() >= randomN )
        ( randomFilesD - randomFiles ).each { verifyBean.directory( it )}
    }


    def 'gc-98: FileBean.extension() - return empty String if file has no extension'( String fileName, String ext )
    {
        assert testDir.directory && ( ! testDir.listFiles())

        expect:
        fileBean.extension( new File( fileName )) == ext

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
        fileBean.extension( new File( fileName )) == ext

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
        fileBean.unpackZipEntries( zipFile, unpack1, [ '**/*.jar' ] )
        fileBean.unpackZipEntries( zipFile, unpack2, [], [ '**/*.jar' ] )
        fileBean.unpackZipEntries( zipFile, unpack3, [ '**/*.jar' ], [ '**/m*.*' ] )
        fileBean.unpackZipEntries( zipFile, unpack4, [ '**/*.jar' ], [ "$testArchive/lib/maven-core-3.0.1.jar" ] )
        fileBean.unpackZipEntries( zipFile, unpack5, [ "$testArchive/lib/plexus-utils-2.0.4.jar" ], [ "$testArchive/lib/maven-core-3.0.1.jar" ] )
        fileBean.unpackZipEntries( zipFile, unpack6, [ '**/*.exe', 'no-such-file', '**/*.jar' ], [ '**/m*.*' ], true, false )

        then:
        32      == fileBean.recurse( unpack1, [ type: FileType.FILES ], { File f -> assert   f.name.endsWith( '.jar' )}).size()
        10      == fileBean.recurse( unpack2, [ type: FileType.FILES ], { File f -> assert ! f.name.endsWith( '.jar' )}).size()
        21      == fileBean.recurse( unpack3, [ type: FileType.FILES ], { File f -> assert   f.name.endsWith( '.jar' )}).size()
        31      == fileBean.recurse( unpack4, [ type: FileType.FILES ], { File f -> assert   f.name.endsWith( '.jar' ) && ( ! f.name.contains( 'maven-core-3.0.1' )) }).size()
        1       == fileBean.recurse( unpack5, [ type: FileType.FILES ], { File f -> assert   f.name == 'plexus-utils-2.0.4.jar' }).size()

        3301021 == fileBean.directorySize( unpack1 )
        43306   == fileBean.directorySize( unpack2 )
        1860093 == fileBean.directorySize( unpack3 )
        2770664 == fileBean.directorySize( unpack4 )
        222137  == fileBean.directorySize( unpack5 )

        verifyBean.equal( unpack3, unpack6 )

        shouldFailAssert { fileBean.unpackZipEntries( zipFile, unpack1, [ "$testArchive/lib/maven-core-3.0.1.jar" ], [ "$testArchive/lib/maven-core-3.0.1.jar" ] )}
        shouldFailAssert { fileBean.unpackZipEntries( zipFile, unpack1, [ '**/*.bat' ], [ '**/m*'   ] )}

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
        fileBean.unpackZipEntries( zipFile, unpack1, [ '**/*.jar' ] )
        fileBean.unpackZipEntries( zipFile, unpack2, [], [ '**/*.html', '**/*.groovy', '**/*.java' ] )
        fileBean.unpackZipEntries( zipFile, unpack3, [ '**/*.jar', '**/*.html', '**/*.groovy' ], [ '**/*d*', '**/*e*' ] )
        fileBean.unpackZipEntries( zipFile, unpack4, [ '**/*.html' ], [ '**/docs/**' ] )
        fileBean.unpackZipEntries( zipFile, unpack5, [ "$testArchive/src/org/gradle/api/**/*.groovy" ], [ "$testArchive/src/org/gradle/**/internal/**" ] )
        fileBean.unpackZipEntries( zipFile, unpack6, [ "$testArchive/src/org/gradle/**" ],              [ '**/*.groovy', '**/*.java' ] )
        fileBean.unpackZipEntries( zipFile, unpack7, [ "$testArchive/src/org/gradle/**" ],              [ '**/*.groovy', '**/*.java', '**/*.txt', '**/*.png', '**/*.xml' ] )
        fileBean.unpackZipEntries( zipFile, unpack8, [ '**/*.dll', 'no-such-file', '**/*.html' ], [ '**/docs/**' ], true, false )

        then:
        108      == fileBean.recurse( unpack1, [ type: FileType.FILES ], { File f -> assert   f.name.endsWith( '.jar' )}).size()
        522      == fileBean.recurse( unpack2, [ type: FileType.FILES ], { File f -> assert ! [ 'html', 'groovy', 'java' ].any { f.name.endsWith( ".$it" ) }}).size()
        149      == fileBean.recurse( unpack3, [ type: FileType.FILES ], { File f -> assert ! [ 'd', 'e' ].any { f.name.contains( it ) }}).size()
        7        == fileBean.recurse( unpack4, [ type: FileType.FILES ], { File f -> assert f.name.endsWith( '.html'   ) && ( ! f.path.contains ( 'docs'     )) }).size()
        32       == fileBean.recurse( unpack5, [ type: FileType.FILES ], { File f -> assert f.name.endsWith( '.groovy' ) && ( ! f.path.contains ( 'internal' )) }).size()
        29       == fileBean.recurse( unpack6, [ type: FileType.FILES ], { File f -> assert [ 'gradle', 'html', 'png', 'properties', 'txt', 'xml' ].any{ f.name.endsWith( ".$it" ) }}).size()
        4        == fileBean.recurse( unpack7, [ type: FileType.FILES ], { File f -> assert [ 'gradle', 'html', 'properties' ].any{ f.name.endsWith( ".$it" ) }}).size()

        33788294 == fileBean.directorySize( unpack1 )
        35539173 == fileBean.directorySize( unpack2 )
        17339526 == fileBean.directorySize( unpack3 )
        33352    == fileBean.directorySize( unpack4 )
        88130    == fileBean.directorySize( unpack5 )
        43630    == fileBean.directorySize( unpack6 )
        6033     == fileBean.directorySize( unpack7 )

        verifyBean.equal( unpack4, unpack8 )

        shouldFailAssert { fileBean.unpackZipEntries( zipFile, unpack1, [ '**/*.html' ],       [ '**/*.html'     ] )}
        shouldFailAssert { fileBean.unpackZipEntries( zipFile, unpack1, [ '**/*.groovy' ],     [ '**/*r*'        ] )}
        shouldFailAssert { fileBean.unpackZipEntries( zipFile, unpack1, [ '**/build.gradle' ], [ '**/samples/**' ] )}

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
        fileBean.recurse( testDir, [ type: FileType.FILES       ], { File f -> assert f.file      }).size() == nFiles
        fileBean.recurse( testDir, [ type: FileType.DIRECTORIES ], { File f -> assert f.directory }).size() >  nFiles
        fileBean.recurse( testDir, [ type: FileType.ANY         ], {}                              ).size() >  nFiles

        fileBean.recurse( testDir, [ type: FileType.FILES,       returnList: false ], {}).empty
        fileBean.recurse( testDir, [ type: FileType.DIRECTORIES, returnList: false ], {}).empty

        fileBean.recurse( testDir, [ type: FileType.FILES       ], { File f -> assert f.file      }).every { File f -> f.file      }
        fileBean.recurse( testDir, [ type: FileType.DIRECTORIES ], { File f -> assert f.directory }).every { File f -> f.directory }

        shouldFailAssert { fileBean.recurse( testDir, [ something: 'anything' ], {} )}
        shouldFailAssert { fileBean.recurse( testDir, [ tyype    : 'typo'     ], {} )}
    }


    @SuppressWarnings( 'ClosureAsLastMethodParameter' )
    def "gc-104: FileBean.files() fails when directory specified doesn't exist even if 'failIfNotFound' is 'false'" ()
    {
        assert testDir.directory && ( ! testDir.listFiles())

        expect:
        [] == fileBean.files( new File( 'no-such-directory' ), [ '**' ],       [],       true, false, false )
        [] == fileBean.files( new File( 'no-such-directory' ), [ '**/*.exe' ], [],       true, false, false )
        [] == fileBean.files( new File( 'no-such-directory' ), [],             [ '**' ], true, false, false )

        [] == fileBean.files( constantsBean.USER_DIR_FILE, [ 'no-such-file.txt' ],  [],  true, false, false )
        [] == fileBean.files( constantsBean.USER_DIR_FILE, [ '**/*no-such-file*' ], [],  true, false, false )
        [] == fileBean.files( constantsBean.USER_DIR_FILE, [], [ '**' ],                 true, false, false )
    }



    def 'gc-114: FileBean.baseName() - retrieve file base name' ( String fileName, String bName )
    {
        expect:
        fileBean.baseName( new File( fileName )) == bName

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


    def 'gc-115: FileBean.pack() - allow to set compression level when using Ant'( String testArchive )
    {
        given:
        final unpackDir = new File( testDir, 'unpack' )
        final zip1      = new File( testDir, '1.zip'  )
        final zip2      = new File( testDir, '2.zip'  )
        final zip3      = new File( testDir, '3.zip'  )
        final zip4      = new File( testDir, '4.zip'  )

        fileBean.unpack( testResource( "${ testArchive }.zip" ), unpackDir )

        fileBean.pack( unpackDir, zip1, [ '**' ], [], false, true, false, null, null, null, true, null, 0 )
        fileBean.pack( unpackDir, zip2, [ '**' ], [], false, true, false, null, null, null, true, null, 5 )
        fileBean.pack( unpackDir, zip3, [ '**' ], [], false, true, false, null, null, null, true, null, 9 )

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


    def 'gc-118: Support *.gz archives unpack'()
    {
        given:
        final gzip = testResource( 'faa-1.gz' )

        when:
        GCommons.file().unpack( gzip, testDir, false )

        then:
        GCommons.verify().file( new File( testDir, 'faa-1' )).name == 'faa-1'
    }


    def 'gc-120: Allow to specify custom archive formats for zip, tar, tar.gz and gz pack and unpack operations'()
    {
        given:
        final zipAnt       = new File( testDir, 'fileAnt.z' )
        final tarAnt       = new File( testDir, 'fileAnt.t' )
        final tarGzAnt     = new File( testDir, 'fileAnt.tz' )
        final zipTrueZip   = new File( testDir, 'fileTrueZip.z' )
        final tarTrueZip   = new File( testDir, 'fileTrueZip.t' )
        final tarGzTrueZip = new File( testDir, 'fileTrueZip.tz' )
        final unpack1      = new File( testDir, '1' )
        final unpack2      = new File( testDir, '2' )
        final unpack3      = new File( testDir, '3' )
        final unpack4      = new File( testDir, '4' )
        final unpack5      = new File( testDir, '5' )
        final unpack6      = new File( testDir, '6' )

        fileBean.customArchiveFormats = [ zip: [ 'z' ] , tar: [ 't' ] , 'tar.gz': [ 'tz' ]]

        fileBean.unpack( testResource( 'apache-maven-3.0.1.zip'    ), unpack1 )
        fileBean.unpack( testResource( 'apache-maven-3.0.1.tar'    ), unpack2 )
        fileBean.unpack( testResource( 'apache-maven-3.0.1.tar.gz' ), unpack3 )

        verifyBean.equal( unpack1, unpack2 )
        verifyBean.equal( unpack2, unpack3 )
        verifyBean.equal( unpack3, unpack1 )

        fileBean.pack( unpack1, zipAnt,       null, null, false )
        fileBean.pack( unpack1, zipTrueZip,   null, null, true  )
        fileBean.pack( unpack2, tarAnt,       null, null, false )
        fileBean.pack( unpack2, tarTrueZip,   null, null, true  )
        fileBean.pack( unpack3, tarGzAnt,     null, null, false )
        fileBean.pack( unpack3, tarGzTrueZip, null, null, true  )

        fileBean.unpack ( zipAnt,   unpack4, false )
        fileBean.unpack ( tarAnt,   unpack5, false )
        fileBean.unpack ( tarGzAnt, unpack6, false )

        verifyBean.equal( unpack3, unpack4 )
        verifyBean.equal( unpack4, unpack5 )
        verifyBean.equal( unpack5, unpack6 )

        fileBean.delete( unpack4, unpack5, unpack6 )

        fileBean.unpack ( zipAnt,   unpack4, true )
        fileBean.unpack ( tarAnt,   unpack5, true )
        fileBean.unpack ( tarGzAnt, unpack6, true)

        verifyBean.equal( unpack3, unpack4 )
        verifyBean.equal( unpack4, unpack5 )
        verifyBean.equal( unpack5, unpack6 )

        fileBean.delete( unpack4, unpack5, unpack6 )

        fileBean.unpack ( zipTrueZip,   unpack4, false )
        fileBean.unpack ( tarTrueZip,   unpack5, false )
        fileBean.unpack ( tarGzTrueZip, unpack6, false )

        verifyBean.equal( unpack3, unpack4 )
        verifyBean.equal( unpack4, unpack5 )
        verifyBean.equal( unpack5, unpack6 )

        fileBean.delete( unpack4, unpack5, unpack6 )

        fileBean.unpack ( zipTrueZip,   unpack4, true )
        fileBean.unpack ( tarTrueZip,   unpack5, true )
        fileBean.unpack ( tarGzTrueZip, unpack6, true )

        verifyBean.equal( unpack3, unpack4 )
        verifyBean.equal( unpack4, unpack5 )
        verifyBean.equal( unpack5, unpack6 )

        fileBean.resetCustomArchiveFormats()
    }
}
