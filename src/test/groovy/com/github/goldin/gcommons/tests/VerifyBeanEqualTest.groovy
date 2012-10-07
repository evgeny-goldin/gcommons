package com.github.goldin.gcommons.tests

import com.github.goldin.gcommons.BaseTest
import groovy.util.logging.Slf4j
import org.junit.Test


/**
 * {@link com.github.goldin.gcommons.beans.VerifyBean#equal(File, File, boolean, String, String)} tests
 */
@Slf4j
class VerifyBeanEqualTest extends BaseTest
{
    @Test
    void shouldFailOnNullInput()
    {
        verifyBean.with {
            shouldFailAssert { equal( new File( 'doesn\'t exist' ), null ) }
            shouldFailWith( NullPointerException ) { equal( null, null ) }
            shouldFailWith( NullPointerException ) { equal( null, new File( 'aaa' )) }
            shouldFailWith( NullPointerException ) { equal( null, constantsBean.USER_DIR_FILE  ) }
            shouldFailWith( NullPointerException ) { equal( null, constantsBean.USER_HOME_FILE ) }
            shouldFailWith( NullPointerException ) { equal( constantsBean.USER_DIR_FILE,  null ) }
            shouldFailWith( NullPointerException ) { equal( constantsBean.USER_HOME_FILE, null ) }
        }
    }


    @Test
    void shouldFailOnMissingFiles()
    {
        verifyBean.with { constantsBean.with {
            shouldFailAssert { equal( new File( 'doesn\'t exist' ),
                                                 new File( 'doesn\'t exist' ) ) }

            shouldFailAssert { equal( new File( USER_DIR_FILE, '1111.txt' ),
                                      new File( USER_DIR_FILE, '2222.txt' )) }

            shouldFailAssert { equal( new File( USER_DIR_FILE, '3333.txt' ),
                                                 USER_DIR_FILE) }

            def file = new File( USER_HOME_FILE, 'a.txt' )

            shouldFailAssert { equal( USER_HOME_FILE, file ) } // Directory + missing file

            file.write( 'anything' )

            shouldFailAssert { equal( USER_HOME_FILE, file ) } // Directory + existing file
            shouldFailAssert { equal( USER_DIR_FILE,  file ) }
            shouldFailAssert { equal( file,  USER_HOME_FILE) }
            shouldFailAssert { equal( file,  USER_DIR_FILE) }

            fileBean.delete( file )
            shouldFailAssert { equal( file, file ) }
        }}
    }


    @Test
    void shouldVerifyEqualFiles()
    {
        verifyBean.with { fileBean.with {
            File f1 = tempFile()
            File f2 = tempFile()

            equal( f1, f2 )

            def data = System.currentTimeMillis() as String

            f1.write( data * 10 )
            f2.write( data * 10 )

            equal( f1, f2 )

            f1.append( data * 10 )
            f2.append( data * 10 )

            equal( f1, f2 )

            f1.write( data * 10 )
            f2.write( data * 11 )

            shouldFailAssert{ equal( f1, f2 ) }

            delete( f1, f2 )
            shouldFailAssert{ equal( f1, f2 ) }
        }}
    }


    @Test
    void shouldVerifyEqualDirectories()
    {
        verifyBean.with { fileBean.with {
            def d1 = tempDirectory()
            def d2 = tempDirectory()

            shouldFailAssert { equal( d1, d2 ) }
            equal( d1, d2, true, null, null, false )

            new File( d1, 'a.txt' ).write( 'aa' )
            new File( d2, 'a.txt' ).write( 'aa' )

            equal( d1, d2 )

            new File( d1, 'aa.txt' ).write( 'aa' )
            new File( d2, 'aa.txt' ).write( 'ab' )

            shouldFailAssert { equal( d1, d2 ) }

            new File( d2, 'aa.txt' ).write( 'aa' )

            equal( d1, d2 )

            new File( d1, 'aa.txt' ).write( 'aa' )
            new File( d2, 'aa.xml' ).write( 'aa' )

            shouldFailAssert { equal( d1, d2 ) }

            shouldFailAssert {
                shouldFailAssert { equal( constantsBean.USER_DIR_FILE,  constantsBean.USER_DIR_FILE, false )}
            }

            shouldFailAssert { equal( constantsBean.USER_DIR_FILE,  constantsBean.USER_HOME_FILE, false )}

            delete( d1, d2 )
            shouldFailAssert { equal( d1, d2 ) }
        }}
    }


    @Test
    @SuppressWarnings( 'AbcComplexity' )
    void shouldVerifyEqualDirectoriesWithPattern()
    {
        verifyBean.with { fileBean.with { constantsBean.with {

            def buildDir = new File( USER_DIR_FILE, 'build/classes' )
            def srcDir   = new File( USER_DIR_FILE, 'src/main'      )

            equal( buildDir, buildDir, false )
            equal( buildDir, buildDir, false, '**/*.class' )
            equal( buildDir, buildDir, false, '*.class', null, false )

            shouldFailAssert { equal( buildDir, buildDir, false, '*.class' ) }

            equal( srcDir, srcDir, true )

            shouldFailAssert { equal( srcDir, srcDir, false, '*.class' ) }
            equal( srcDir, srcDir, false, '*.class',    null, false )
            equal( srcDir, srcDir, false, '**/*.class', null, false )

            shouldFailAssert {  equal( srcDir, srcDir, true,  '*.groovy' )}
            equal( srcDir, srcDir, true, '**/*.groovy' )

            equal( srcDir, srcDir, true, '**/*.xml'    )

            def d1 = tempDirectory()
            def d2 = tempDirectory()

            new File( d1, 'a.txt' ).write( 'txt'  ) // Same content for 'txt' files
            new File( d2, 'a.txt' ).write( 'txt'  )
            new File( d1, 'a.xml' ).write( 'xml1' ) // Different content for 'xml' files
            new File( d2, 'a.xml' ).write( 'xml2' )

            shouldFailAssert { equal( d1, d2 ) }
            shouldFailAssert { equal( d1, d2, true, '*.*'       ) }
            shouldFailAssert { equal( d1, d2, true, '*.xml'     ) }
            shouldFailAssert { equal( d1, d2, true, '**/*.*'    ) }
            shouldFailAssert { equal( d1, d2, true, '**/*.xml'  ) }
            shouldFailAssert { equal( d1, d2, true, '**\\*.xml' ) }
            shouldFailAssert { equal( d1, d2, true, '**/a.xml'  ) }
            shouldFailAssert { equal( d1, d2, true, '**\\a.xml' ) }
            shouldFailAssert { equal( d1, d2, true, '**//a.xml' ) }

            equal( d1, d2, true, '**/*.txt' )
            equal( d1, d2, true, '*.txt' )
            equal( d1, d2, true, '*.tx*' )
            equal( d1, d2, true, '*.t*t' )
            equal( d1, d2, true, '*.t*' )
            equal( d1, d2, true, '**/a.txt' )
            equal( d1, d2, true, '**\\a.txt' )
            equal( d1, d2, true, '**//a.txt' )

            equal( d1, d2, true, 'b.xml', null, false )

            shouldFailAssert { equal( d1, d2, true, 'b.xml'     )}
            shouldFailAssert { equal( d1, d2, true, '**/b.xml'  )}
            shouldFailAssert { equal( d1, d2, true, '**/c.xml'  )}
            shouldFailAssert { equal( d1, d2, true, '**//b.xml' )}
            shouldFailAssert { equal( d1, d2, true, '**\\b.xml' )}

            delete( d1, d2 )
            shouldFailAssert { equal( d1, d2 ) }
        }}}
    }
}
