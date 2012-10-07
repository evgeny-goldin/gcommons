package com.github.goldin.gcommons.tests

import com.github.goldin.gcommons.BaseTest
import groovy.util.logging.Slf4j
import org.junit.Test


/**
 * {@link com.github.goldin.gcommons.beans.VerifyBean} tests
 */
@Slf4j
class VerifyBeanTest extends BaseTest
{
    @Test
    void shouldVerifyEmptyStrings()
    {
        verifyBean.with {
            notNullOrEmpty( 'aa' )
            notNullOrEmpty( 'aa', 'bb' )

            shouldFailAssert { notNullOrEmpty( 'aa', 'bb', null ) }
            shouldFailAssert { notNullOrEmpty( 'aa', 'bb', ''   ) }
            shouldFailAssert { notNullOrEmpty( 'aa', 'bb', ' '  ) }
            shouldFailAssert { notNullOrEmpty( '', 'bb', ' '  ) }
            shouldFailAssert { notNullOrEmpty( ' ', 'bb', ' '  ) }
            shouldFailAssert { shouldFailAssert { notNullOrEmpty( ' c', 'bb', ' d'  ) }}
        }
    }


    @Test
    void shouldVerifyEmptyCollections()
    {
        verifyBean.with {
            notNullOrEmpty( ['aa'] )
            notNullOrEmpty( ['aa'], ['bb'] )
            notNullOrEmpty( ['aa'], ['bb'], ['zzz'] )

            shouldFailAssert { notNullOrEmpty( ['aa'], ['bb'], null ) }
            shouldFailAssert { notNullOrEmpty( ['aa'], ['bb'], []   ) }
            shouldFailAssert { notNullOrEmpty( ['aa', 'bb'], []  ) }
            shouldFailAssert { notNullOrEmpty( null, ['aa', 'bb'], []  ) }
            shouldFailAssert { notNullOrEmpty( [], null, ['aa', 'bb'], []  ) }

            shouldFailAssert { shouldFailAssert { notNullOrEmpty( [''], ['bb'], [' ']  ) }}
            shouldFailAssert { shouldFailAssert { notNullOrEmpty( [' '], ['bb', ' ']  ) }}
            shouldFailAssert { shouldFailAssert { notNullOrEmpty( [' c'], ['bb'], [' d']  ) }}
        }
    }


    @Test
    void shouldVerifyExists()
    {
        verifyBean.with { fileBean.with {
            exists( constantsBean.USER_DIR_FILE  )
            exists( constantsBean.USER_HOME_FILE )

            def f = tempFile()
            exists( f )
            delete( f )

            shouldFailAssert { exists( f ) }
            shouldFailAssert { exists( new File( 'Doesn\'t exist' )) }
            shouldFailWith( NullPointerException ) { exists( new File( System.getProperty( 'aaa' ))) }
        }}
    }


    @Test
    void shouldVerifyFile()
    {
        def f = fileBean.tempFile()
        verifyBean.file( f )
        shouldFailAssert { verifyBean.file( f.parentFile ) }
        shouldFailAssert { verifyBean.file( f.parentFile.parentFile ) }

        fileBean.delete( f )

        shouldFailAssert { verifyBean.file( f ) }
    }


    @Test
    void shouldVerifyDirectory()
    {
        verifyBean.with { fileBean.with {
            directory( constantsBean.USER_DIR_FILE  )
            directory( constantsBean.USER_HOME_FILE )

            def f = tempFile()
            file( f )
            directory( f.parentFile )
            directory( f.parentFile.parentFile )

            delete( f )

            shouldFailAssert { file( f ) }

            directory( f.parentFile )
            directory( f.parentFile.parentFile )
        }}
    }


    @Test
    @SuppressWarnings( 'ChainedTest' )
    void shouldVerifyEqual()
    {
        verifyBean.with { fileBean.with {
            for ( archiveName in testArchives().keySet())
            {
                File dir1 = testDir( 'unpack-1' )
                File dir2 = testDir( 'unpack-2' )

                unpack( testResource( archiveName + '.zip' ), dir1 )
                unpack( testResource( archiveName + '.jar' ), dir2 )

                equal( dir1, dir2 )
                equal( dir1, dir2, false )
                equal( dir1, dir2, true, '**/*.xml' )
                equal( dir1, dir2, true, '**/*.xml', 'windows' )
                equal( dir1, dir2, true, '**/*.xml', 'linux'   )
                equal( dir1, dir2, true, '**/*.jar' )

                delete( files( dir2, [ '**/*.xml' ] ) as File[] )

                shouldFailAssert { equal( dir1, dir2 )}
                shouldFailAssert { equal( dir2, dir1, true, '**/*.xml' )}
                shouldFailAssert { equal( dir1, dir2, false )}
                shouldFailAssert { equal( dir1, dir2, true, '**/*.xml' )}
                shouldFailAssert { equal( dir1, dir2, true, '**/*.xml', 'windows' )}
                shouldFailAssert { equal( dir1, dir2, true, '**/*.xml', 'linux'   )}
                shouldFailAssert { shouldFailAssert { equal( dir1, dir2, true, '**/*.jar' )}}
            }
        }}
    }


    @Test
    void shouldVerifyIsInstance()
    {
        verifyBean.with {
            shouldFailAssert { isInstance( null, null   ) }
            shouldFailAssert { isInstance( null, Object ) }
            shouldFailAssert { isInstance( '',   null   ) }

            assert '' == isInstance( '', String )
            assert '' == isInstance( '', Serializable )
            assert '' == isInstance( '', CharSequence )
            assert '' == isInstance( '', Object )

            shouldFailAssert { assert '' == isInstance( '', Map ) }

            assert ['1'] == isInstance( ['1'], ArrayList )
            assert ['2'] == isInstance( ['2'], List )
            assert ['3'] == isInstance( ['3'], Object )

            shouldFailAssert { assert ['3'] == isInstance( ['3'], String ) }

            assert [1:2] == isInstance( [1:2], HashMap )
            assert [3:4] == isInstance( [3:4], Map )
            assert [5:6] == isInstance( [5:6], Object )

            shouldFailAssert { assert [5:6] == isInstance( [5:6], List ) }
        }
    }
}
