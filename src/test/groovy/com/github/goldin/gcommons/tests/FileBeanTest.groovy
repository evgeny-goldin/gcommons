package com.github.goldin.gcommons.tests

import com.github.goldin.gcommons.BaseTest
import groovy.io.FileType
import groovy.util.logging.Slf4j
import org.apache.tools.zip.ZipFile
import org.junit.Test


/**
 * {@link com.github.goldin.gcommons.beans.FileBean} tests
 */
@Slf4j
class FileBeanTest extends BaseTest
{
    private static File writeFile( File f, String content = null )
    {
        assert ( f.parentFile.directory || f.parentFile.mkdirs())

        if ( content )
        {
            f.write( content )
        }
        else
        {
            f.write( f.canonicalPath )
            f.append( System.currentTimeMillis())
            f.append( new Date())
        }

        assert ( f.exists() && f.file )
        f
    }


    @Test
    void shouldDeleteFiles()
    {
        def file = fileBean.tempFile()

        assert ( file.exists() && file.file )

        fileBean.delete( file )

        assert ! file.exists()
        assert ! file.file

        def dir = fileBean.tempDirectory()
        fileBean.delete( writeFile( new File( dir, '1.txt' )))
        fileBean.delete( writeFile( new File( dir, '2.xml' )))
        fileBean.delete( writeFile( new File( dir, '3.ppt' )))

        assert ! dir.list()
        assert ! dir.listFiles()
        assert ! file.exists()
        assert ! file.file

        fileBean.delete( dir )
    }


    @Test
    @SuppressWarnings( 'AbcComplexity' )
    void shouldMkdir()
    {
        def f = { String name -> new File( constantsBean.USER_HOME_FILE, name ) }

        fileBean.mkdirs( f( 'aa' ), f( 'aa/bb' ), f( 'aa/bb/dd' ), f( 'ee/bb/dd' ), f( 'ff/bb/dd/kk' ))
        verifyBean.directory( f( 'aa' ), f( 'aa/bb' ), f( 'aa/bb/dd' ),
                              f( 'ee' ), f( 'ee/bb' ), f( 'ee/bb/dd' ),
                              f( 'ff' ), f( 'ff/bb' ), f( 'ff/bb/dd' ),f( 'ff/bb/dd/kk' ))

        shouldFailAssert { verifyBean.directory( f( 'aa' ), f( 'aa/bb' ), f( 'aa/bb/dd1' )) }

        f( 'aa/1.txt' ).write( System.currentTimeMillis() as String )
        f( 'aa/bb/2.txt' ).write( System.currentTimeMillis() as String )
        f( 'aa/bb/dd/3.txt' ).write( System.currentTimeMillis() as String )

        f( 'ee/1.txt' ).write( System.currentTimeMillis() as String )
        f( 'ee/bb/2.txt' ).write( System.currentTimeMillis() as String )
        f( 'ee/bb/dd/3.txt' ).write( System.currentTimeMillis() as String )

        f( 'ff/1.txt' ).write( System.currentTimeMillis() as String )
        f( 'ff/bb/2.txt' ).write( System.currentTimeMillis() as String )
        f( 'ff/bb/dd/3.txt' ).write( System.currentTimeMillis() as String )
        f( 'ff/bb/dd/kk/4.txt' ).write( System.currentTimeMillis() as String )

        fileBean.delete( f( 'aa' ), f( 'ee' ), f( 'ff' ))

        shouldFailAssert { verifyBean.directory( f( 'aa' )) }
        shouldFailAssert { verifyBean.directory( f( 'ee' )) }
        shouldFailAssert { verifyBean.directory( f( 'ff' )) }
    }


    @Test
    void shouldDeleteDirectories()
    {
        def dir = fileBean.tempDirectory()

        assert dir.exists() && dir.directory

        writeFile( new File( dir, '1.txt' ))
        writeFile( new File( dir, '2.xml' ))
        writeFile( new File( dir, '3.ppt' ))
        writeFile( new File( dir, 'a/b/1.txt' ))
        writeFile( new File( dir, 'c/d/2.xml' ))
        writeFile( new File( dir, 'e/f/g/h/3.ppt' ))
        writeFile( new File( dir, '11.txt' ))
        writeFile( new File( dir, '22.xml' ))
        writeFile( new File( dir, '33.ppt' ))
        writeFile( new File( dir, 'aw/bq/1j.txt' ))
        writeFile( new File( dir, 'cy/do/2p.xml' ))
        writeFile( new File( dir, 'easdf/fdsd/gwqeq/hujy/3weqw.ppt.eqeq' ))

        fileBean.delete( dir )
        assert ! dir.exists()
        assert ! dir.file
    }


    @Test
    void shouldCalculateChecksum()
    {
        def file = testResource( 'apache-maven-3.0.1.zip' )

        assert fileBean.checksum( file )        == fileBean.checksum( file, 'SHA-1' )
        assert fileBean.checksum( file )        == '7db54443784f547a36a7adb293bfeca2d2c9d15c'
        assert fileBean.checksum( file, 'MD5' ) == '3aeeb8b545ae1b6aa8b2015dce24eec7'

        def dir = fileBean.tempDirectory()
        file    = new File( dir, '1.txt' )

        shouldFailAssert { fileBean.checksum( dir  ) }

        writeFile( file, '7db54443784f547a36a7adb293bfeca2d2c9d15c\r\n' )
        assert fileBean.checksum( file, 'MD5' ) == '04ce83c072936118922107babdf6d21a'
        assert fileBean.checksum( file )        == 'fcd551a840d37d3c885db298e893ec77468a81cd'
        assert fileBean.checksum( file, 'MD5' ) == fileBean.checksum( file, 'MD5' )
        assert fileBean.checksum( file, 'MD5' ) != fileBean.checksum( file )

        fileBean.delete( dir )
    }


    @Test
    @SuppressWarnings( 'ChainedTest' )
    void shouldPackWithUpdate()
    {
        def resourcesDir  = new File( 'src/test/resources' )
        // 'src/test/resources' files to update archives with
        def filesToUpdate = [ 'testResource.txt', 'maven-settings-3.0.2.jar', 'image-3-abc.zip' ]

        def c =
        {
            File unpackDir, File archive ->

            verifyBean.directory( unpackDir )
            verifyBean.file( archive )

            fileBean.pack( resourcesDir, archive, filesToUpdate, [], false, true, true ) /* Updating an archive */
            fileBean.unpackZipEntries( archive, unpackDir, filesToUpdate,  [], false )
            fileBean.unpackZipEntries( archive, unpackDir, [ '**/*.jar' ], [], false )

            filesToUpdate.each{ String fileName -> verifyBean.file( new File( unpackDir, fileName )) }
            assert unpackDir.listFiles().any{ it.name.endsWith( '.jar' ) }
        }

        for ( archiveName in testArchives().keySet())
        {
            def packDir   = testDir( 'pack' )
            def unpackZip = testDir( 'unpackZip' )
            def unpackJar = testDir( 'unpackJar' )

            c( unpackZip, fileBean.copy( testResource( "${archiveName}.zip" ), packDir ))
            c( unpackJar, fileBean.copy( testResource( "${archiveName}.jar" ), packDir ))

            verifyBean.equal( unpackZip, unpackJar )
            verifyBean.equal( unpackZip, unpackJar, true, '*.jar' )
            verifyBean.equal( unpackZip, unpackJar, true, '*.zip' )
            verifyBean.equal( unpackZip, unpackJar, true, '*.txt' )

            for ( extension in [ 'tar', 'tgz', 'tar.gz' ] )
            {
                shouldFailAssert {
                    fileBean.pack( resourcesDir,
                                   fileBean.copy( testResource( "$archiveName.$extension" ), packDir ),
                                   filesToUpdate, [], false, true, true )
                }
            }
        }
    }


    @Test
    void shouldPackWithUpdateAndDuplicate()
    {
        def packDir       = testDir( 'pack'   )
        def unpackDir     = testDir( 'unpack' )
        def sourceArchive = testResource( 'apache-maven-3.0.1.zip' )
        def destArchive   = new File( packDir, 'apache-maven-3.0.1.zip' )
        def file1         = new File( packDir, '1.txt' )
        def file2         = new File( packDir, 'NOTICE.txt' )

        file1.write( 'Aa' )
        file2.write( 'Bb' )

        fileBean.copy  ( sourceArchive, packDir )
        fileBean.pack  ( file1.parentFile, destArchive, [ file1.name ], [], false, true, true, [], 'apache-maven-3.0.1/LICENSE.txt' )
        fileBean.pack  ( file2.parentFile, destArchive, [ file2.name ], [], false, true, true, [], '', 'apache-maven-3.0.1' )
        fileBean.unpack( destArchive, unpackDir )

        assert new File( unpackDir, 'apache-maven-3.0.1/LICENSE.txt' ).text == 'Aa'
        assert new File( unpackDir, 'apache-maven-3.0.1/NOTICE.txt'  ).text == 'Bb'
    }


    @Test
    @SuppressWarnings([ 'ChainedTest', 'AbcComplexity' ])
    void shouldPackWithFullPathAndPrefix()
    {
        Map testArchives  = testArchives()
        def imageFile     = testResource( 'image-3-abc.zip' )
        def imageFileSize = imageFile.size()

        for ( archiveName in testArchives.keySet())
        {
            def packDir = testDir( 'pack' )

            for ( updateArchive in [ 'zip', 'jar', 'war', 'ear' ].collect { new File( packDir, "$archiveName.$it" ) } )
            {
                fileBean.with {
                    copy( testResource( "${archiveName}.zip" ), packDir, updateArchive.name )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], 'image1.zip' )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], '', 'aa' )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], '/image2.zip' )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], '', '/bb' )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], '\\image3.zip' )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], '', '\\cc' )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], '/1/2/image2.zip' )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], '', '/11/22' )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], '\\4/5/image3.zip' )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], '', '\\44\\55' )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], '7/8/image8.zip' )
                    pack( imageFile.parentFile, updateArchive, [ imageFile.name ], [], false, true, true, [], '', '77/88' )
                }

                def unpackDir = testDir( 'unpack' )
                fileBean.unpack( updateArchive, unpackDir )

                [ 'image1.zip', 'image2.zip', 'image3.zip', '1/2/image2.zip', '4/5/image3.zip', '7/8/image8.zip',
                  'aa/image-3-abc.zip', 'bb/image-3-abc.zip', 'cc/image-3-abc.zip',
                  '11/22/image-3-abc.zip', '44/55/image-3-abc.zip', '77/88/image-3-abc.zip' ].each {

                    assert verifyBean.file( new File( unpackDir, it )).size() == imageFileSize

                }
            }

            assert ( new File( packDir, "${archiveName}.zip" ).size() == new File( packDir, "${archiveName}.jar" ).size())
            assert ( new File( packDir, "${archiveName}.jar" ).size() == new File( packDir, "${archiveName}.war" ).size())
            assert ( new File( packDir, "${archiveName}.war" ).size() == new File( packDir, "${archiveName}.ear" ).size())
            assert ( new File( packDir, "${archiveName}.ear" ).size() == new File( packDir, "${archiveName}.zip" ).size())
        }
    }


    @Test
    void shouldCopy()
    {
        def filesToCopy  = [ 'image-3-abc.zip', 'apache-maven-3.0.1.jar' ]
        def testDir1     = testDir( 'copy-1' )
        def testDir2     = testDir( 'copy-2' )
        def testDir3     = testDir( 'copy-3' )

        for ( fileName in filesToCopy )
        {
            fileBean.copy( testResource( fileName ), testDir1 )
            fileBean.copy( testResource( fileName ), testDir2, fileName )
            fileBean.copy( testResource( fileName ), testDir3, fileName + '-3' )
        }

        verifyBean.equal( testDir1, testDir2 )
        shouldFailAssert { verifyBean.equal( testDir1, testDir3 )}
        shouldFailAssert { verifyBean.equal( testDir2, testDir3 )}

        verifyBean.with {
            file( filesToCopy.collect { new File( testDir1, it )} as File[] )
            file( filesToCopy.collect { new File( testDir2, it )} as File[] )
            file( filesToCopy.collect { new File( testDir3, it + '-3' )} as File[] )
        }

        fileBean.with{
            assert directorySize( testDir1 ) == directorySize( testDir2 )
            assert directorySize( testDir1 ) == directorySize( testDir3 )
            assert directorySize( testDir2 ) == directorySize( testDir3 )
        }
    }


    @Test
    @SuppressWarnings([ 'ChainedTest', 'AbcComplexity', 'MethodSize' ])
    void shouldUnpackZipEntries()
    {
        def  mavenZip     = testResource( 'apache-maven-3.0.1.zip' )
        def  mavenJar     = testResource( 'apache-maven-3.0.1.jar' )
        def  mavenTar     = testResource( 'apache-maven-3.0.1.tar' )
        def  mavenTgz     = testResource( 'apache-maven-3.0.1.tgz' )
        def  mavenTarGz   = testResource( 'apache-maven-3.0.1.tar.gz' )
        def  plexusJar    = testResource( 'plexus-component-annotations-1.5.5.jar' )
        List archives     = testArchives().keySet().collect { it + '.zip' }
        def  mavenDir1    = testDir( 'apache-maven-1'  )
        def  mavenDir2    = testDir( 'apache-maven-2'  )
        def  mavenDir3    = testDir( 'apache-maven-3'  )
        def  mavenDir4    = testDir( 'apache-maven-4'  )
        def  mavenDir5    = testDir( 'apache-maven-5'  )
        def  mavenDir6    = testDir( 'apache-maven-6'  )
        def  mavenDir7    = testDir( 'apache-maven-7'  )
        def  mavenDir8    = testDir( 'apache-maven-8'  )
        def  mavenDir9    = testDir( 'apache-maven-9'  )

        def entries      = [ 'apache-maven-3.0.1\\lib\\aether-api-1.8.jar',
                             'apache-maven-3.0.1/lib/commons-cli-1.2.jar',
                             '/apache-maven-3.0.1\\bin\\m2.conf',
                             '/apache-maven-3.0.1/bin/mvn',
                             'apache-maven-3.0.1\\lib\\nekohtml-1.9.6.2.jar',
                             'apache-maven-3.0.1/NOTICE.txt',
                             '/apache-maven-3.0.1/NOTICE.txt',
                             'apache-maven-3.0.1\\NOTICE.txt' ]

        def entries2     = [ 'org/codehaus/plexus/component/annotations/Component.class',
                             'org/codehaus/plexus/component/annotations/Configuration.class',
                             'META-INF/MANIFEST.MF',
                             'META-INF/maven/org.codehaus.plexus/plexus-component-annotations/pom.properties',
                             'META-INF/maven/org.codehaus.plexus/plexus-component-annotations/pom.xml',
                             'org/codehaus/plexus/component/annotations/Requirement.class' ]
        fileBean.with {
            unpackZipEntries( mavenZip,  mavenDir1, entries, [], false )
            unpackZipEntries( mavenZip,  mavenDir2, entries, [], false )
            unpackZipEntries( mavenZip,  mavenDir3, entries )
            unpackZipEntries( mavenJar,  mavenDir4, entries, [], false )
            unpackZipEntries( mavenJar,  mavenDir5, entries )
            unpack( plexusJar, mavenDir6 )
            unpackZipEntries( plexusJar, mavenDir7, entries2 )
        }

        archives.each {
            def testArchiveFile = testResource( it )
            fileBean.unpack( testArchiveFile,  mavenDir8 )
            fileBean.unpackZipEntries( testArchiveFile,  mavenDir9, new ZipFile( testArchiveFile ).entries*.name )
        }

        assert mavenDir1.list().size() == 6
        assert mavenDir2.list().size() == 6
        assert mavenDir4.list().size() == 6
        assert mavenDir3.list().size() == 1
        assert mavenDir5.list().size() == 1
        assert mavenDir6.list().size() == 2
        assert mavenDir7.list().size() == 2

        verifyBean.with {
            equal( mavenDir1, mavenDir2 )
            equal( mavenDir2, mavenDir4 )
            equal( mavenDir4, mavenDir1 )
            equal( mavenDir3, mavenDir5 )
            equal( mavenDir6, mavenDir7 )
            equal( mavenDir6, mavenDir7 )
            equal( mavenDir8, mavenDir9 )
        }

        fileBean.with {
            assert directorySize( mavenDir1 ) == 235902
            assert directorySize( mavenDir2 ) == 235902
            assert directorySize( mavenDir3 ) == 235902
            assert directorySize( mavenDir4 ) == 235902
            assert directorySize( mavenDir5 ) == 235902
            assert directorySize( mavenDir6 ) == 3420
            assert directorySize( mavenDir7 ) == 3420
            assert directorySize( mavenDir8 ) == testArchives().values().sum()
            assert directorySize( mavenDir9 ) == testArchives().values().sum()
        }

        verifyBean.file( new File( mavenDir1, 'aether-api-1.8.jar' ),
                         new File( mavenDir1, 'commons-cli-1.2.jar' ),
                         new File( mavenDir1, 'm2.conf' ),
                         new File( mavenDir1, 'mvn' ),
                         new File( mavenDir1, 'nekohtml-1.9.6.2.jar' ),
                         new File( mavenDir1, 'NOTICE.txt' ))

        verifyBean.file( new File( mavenDir2, 'aether-api-1.8.jar' ),
                         new File( mavenDir2, 'commons-cli-1.2.jar' ),
                         new File( mavenDir2, 'm2.conf' ),
                         new File( mavenDir2, 'mvn' ),
                         new File( mavenDir2, 'nekohtml-1.9.6.2.jar' ),
                         new File( mavenDir2, 'NOTICE.txt' ))

        verifyBean.file( new File( mavenDir3, 'apache-maven-3.0.1/lib/aether-api-1.8.jar' ),
                         new File( mavenDir3, 'apache-maven-3.0.1/lib/commons-cli-1.2.jar' ),
                         new File( mavenDir3, 'apache-maven-3.0.1/bin/m2.conf' ),
                         new File( mavenDir3, 'apache-maven-3.0.1/bin/mvn' ),
                         new File( mavenDir3, 'apache-maven-3.0.1/lib/nekohtml-1.9.6.2.jar' ),
                         new File( mavenDir3, 'apache-maven-3.0.1/NOTICE.txt' ))

        fileBean.with {
            // Entries that don't exist
            shouldFailAssert { unpackZipEntries( plexusJar,  mavenDir7, entries )}
            shouldFailAssert { unpackZipEntries( plexusJar,  mavenDir7, [ 'org/codehaus/plexus/component'  ] )}
            shouldFailAssert { unpackZipEntries( plexusJar,  mavenDir7, [ '/org/codehaus/plexus/component' ] )}
            shouldFailAssert { unpackZipEntries( plexusJar,  mavenDir7, [ 'META-INF'  ] )}
            shouldFailAssert { unpackZipEntries( plexusJar,  mavenDir7, [ '/META-INF' ] )}
            shouldFailAssert { unpackZipEntries( mavenZip,   mavenDir1, [ 'doesnt-exist/entry'  ], [], false )}
            shouldFailAssert { unpackZipEntries( mavenZip,   mavenDir1, [ '/doesnt-exist/entry' ], [], false )}

            // Not Zip files
            shouldFailAssert { unpackZipEntries( mavenTar,   mavenDir1, entries )}
            shouldFailAssert { unpackZipEntries( mavenTgz,   mavenDir1, entries )}
            shouldFailAssert { unpackZipEntries( mavenTarGz, mavenDir1, entries )}

            // Empty list of entries
            shouldFailAssert { unpackZipEntries( mavenZip,   mavenDir1, [ null ] )}
            shouldFailAssert { unpackZipEntries( mavenZip,   mavenDir1, [ ' ', '',  '  ', null ] )}
            shouldFailAssert { unpackZipEntries( mavenZip,   mavenDir1, [ '' ] )}

            // File that doesn't exist
            shouldFailAssert { unpackZipEntries( new File( 'doesnt-exist.file' ), mavenDir1, entries )}

            // Should execute normally and not fail
            shouldFailAssert { shouldFailWith( RuntimeException ) { unpackZipEntries( plexusJar,  mavenDir7, [ '/org/codehaus/plexus/component/'] )}}
            shouldFailAssert { shouldFailWith( RuntimeException ) { unpackZipEntries( plexusJar,  mavenDir7, [ 'org/codehaus/plexus/component/' ] )}}
            shouldFailAssert { shouldFailWith( RuntimeException ) { unpackZipEntries( plexusJar,  mavenDir7, [ '/META-INF/' ] )}}
            shouldFailAssert { shouldFailWith( RuntimeException ) { unpackZipEntries( plexusJar,  mavenDir7, [ 'META-INF/' ] )}}
            shouldFailAssert { shouldFailWith( RuntimeException ) { unpackZipEntries( plexusJar,  mavenDir7, entries2 ) }}
        }
    }


    @Test
    @SuppressWarnings( 'AbcComplexity' )
    void shouldUnpackZipEntriesWithPattern()
    {
        def mavenZip   = testResource( 'apache-maven-3.0.1.zip' )
        def mavenJar   = testResource( 'apache-maven-3.0.1.jar' )
        def mavenDir1  = testDir( 'apache-maven-1' )
        def mavenDir2  = testDir( 'apache-maven-2' )
        def mavenDir3  = testDir( 'apache-maven-3' )
        def mavenDir4  = testDir( 'apache-maven-4' )
        def mavenDir5  = testDir( 'apache-maven-5' )
        def mavenDir6  = testDir( 'apache-maven-6' )
        def mavenDir7  = testDir( 'apache-maven-7' )
        def mavenDir8  = testDir( 'apache-maven-8' )
        def mavenDir9  = testDir( 'apache-maven-9' )
        def mavenDir10 = testDir( 'apache-maven-10' )

        fileBean.with {
            unpackZipEntries( mavenZip, mavenDir1,  [ 'apache-maven-3.0.1/**/*.jar' ] )
            unpackZipEntries( mavenJar, mavenDir2,  [ '**/*.jar' ] )
            unpackZipEntries( mavenZip, mavenDir3,  [ 'apache-maven-3.0.1/**/*.jar' ], [], false )
            unpackZipEntries( mavenJar, mavenDir4,  [ '**/*.jar' ], [], false )
            unpackZipEntries( mavenZip, mavenDir5,  [ '**/*.xml', '**/conf/**' ], [], false )
            unpackZipEntries( mavenJar, mavenDir6,  [ 'apache-maven-3.0.1/conf/settings.xml', '**/*.xml' ], [], false )
            unpack( mavenZip, mavenDir7 )
            unpackZipEntries( mavenJar, mavenDir8,  [ '**' ] )
            unpackZipEntries( mavenJar, mavenDir9,  [ 'apache-maven-3.0.?/**' ] )
            unpackZipEntries( mavenJar, mavenDir10, [ 'apache-maven-?.?.?/**' ] )
        }

        verifyBean.with {
            equal( mavenDir1,  mavenDir2 )
            equal( mavenDir3,  mavenDir4 )
            equal( mavenDir5,  mavenDir6 )
            equal( mavenDir7,  mavenDir8 )
            equal( mavenDir8,  mavenDir9 )
            equal( mavenDir9,  mavenDir10 )
            equal( mavenDir10, mavenDir7 )
        }

        fileBean.with {
            assert directorySize( mavenDir1 ) == 3301021
            assert directorySize( mavenDir2 ) == 3301021
            assert directorySize( mavenDir3 ) == 3301021
            assert directorySize( mavenDir4 ) == 3301021
            assert directorySize( mavenDir5 ) == 1704
            assert directorySize( mavenDir6 ) == 1704
            assert directorySize( mavenDir7 ) == 3344327
            assert directorySize( mavenDir8 ) == 3344327
        }

        assert mavenDir1.list().size() == 1
        assert mavenDir2.list().size() == 1
        assert mavenDir3.list().size() == 32
        assert mavenDir4.list().size() == 32
        assert mavenDir5.list().size() == 1
        assert mavenDir6.list().size() == 1
        assert mavenDir7.list().size() == 1
        assert mavenDir8.list().size() == 1

        fileBean.with {
            shouldFailAssert { unpackZipEntries( mavenZip, mavenDir8, [ '**/*.no-such-file' ] ) }
            shouldFailAssert { unpackZipEntries( mavenZip, mavenDir8, [ 'no-such-file', '**/*.no-such-file' ] ) }
            shouldFailAssert { unpackZipEntries( mavenZip, mavenDir8, [ '**/*.jar', '**/*.ppt' ] ) }
            shouldFailAssert { unpackZipEntries( mavenZip, mavenDir8, [ '**/*.jar', 'no-such-file' ] ) }
            shouldFailAssert { unpackZipEntries( mavenZip, mavenDir8, [ '**/*.exe', 'apache-maven-3.0.1/conf/**', ] ) }
            shouldFailAssert { unpackZipEntries( mavenZip, mavenDir8, [ '**/*.xml', 'apache-maven-3.3.1/**' ] ) }
            shouldFailAssert { unpackZipEntries( mavenZip, mavenDir8, [ '**/*.xml', 'apache-maven-3.3.1' ] ) }
        }
    }


    @Test
    void shouldMatchType()
    {
        def testFile = new File( testDir( 'typeMatch' ), '111' )
        testFile.write( System.currentTimeMillis() as String )

        fileBean.with { constantsBean.with {
            assert   typeMatch( FileType.ANY,         USER_DIR_FILE )
            assert   typeMatch( FileType.DIRECTORIES, USER_DIR_FILE )
            assert ! typeMatch( FileType.FILES,       USER_DIR_FILE )

            assert   typeMatch( FileType.ANY,         USER_HOME_FILE )
            assert   typeMatch( FileType.DIRECTORIES, USER_HOME_FILE )
            assert ! typeMatch( FileType.FILES,       USER_HOME_FILE )

            assert   typeMatch( FileType.ANY,         testFile )
            assert ! typeMatch( FileType.DIRECTORIES, testFile )
            assert   typeMatch( FileType.FILES,       testFile )
        }}
    }
}
