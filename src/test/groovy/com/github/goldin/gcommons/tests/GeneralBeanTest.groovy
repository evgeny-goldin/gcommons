package com.github.goldin.gcommons.tests

import com.github.goldin.gcommons.BaseTest
import com.github.goldin.gcommons.beans.ExecOption
import groovy.util.logging.Slf4j
import org.junit.Test

import java.nio.BufferOverflowException

/**
 * {@link com.github.goldin.gcommons.beans.GeneralBean} tests
 */
@Slf4j
class GeneralBeanTest extends BaseTest
{

    /**
     * Verifies {@link #shouldFailWith} behavior
     */
    @Test
    @SuppressWarnings( [ 'ComparisonOfTwoConstants', 'UnnecessaryStringInstantiation', 'ConstantAssertExpression' ])
    void testShouldFail()
    {
        shouldFailWith( RuntimeException )        { throw new RuntimeException()        }
        shouldFailWith( RuntimeException )        { throw new BufferOverflowException() }
        shouldFailWith( BufferOverflowException ) { throw new BufferOverflowException() }
        shouldFailWith( IOException )             { throw new FileNotFoundException()   }
        shouldFailWith( IOException )             { throw new IOException( new RuntimeException())}

        shouldFailAssert {
            shouldFailWith( NullPointerException ) { throw new RuntimeException() }
        }

        shouldFailAssert { throw new AssertionError() }
        shouldFailAssert { shouldFailAssert { throw new IOException() }}
        shouldFailAssert { assert 3 == 5 }
        shouldFailAssert { assert false  }
        shouldFailAssert { assert null   }
        shouldFailAssert { assert ''     }
        shouldFailAssert { assert 'aa' == 'bb' }
        shouldFailAssert { assert    3 == 4    }
        shouldFailAssert { assert 'aa'.is( new String( 'aa' )) }

        try
        {
            shouldFailWith( IOException ) { throw new IllegalArgumentException() }
            assert false // Shouldn't get here
        }
        catch ( AssertionError ignored ){ /* Good */ }

        try
        {
            shouldFailAssert { shouldFailAssert { shouldFailAssert { throw new IOException() }}}
            assert false // Shouldn't get here
        }
        catch ( AssertionError ignored ){ /* Good */ }
    }


    @Test
    void matchShouldFailOnBadInput ()
    {
        generalBean.with {
            shouldFailAssert { match( '', ''       ) }
            shouldFailAssert { match( '   ', ''    ) }
            shouldFailAssert { match( 'aaaa', ''   ) }
            shouldFailAssert { match( '  ', 'bbbb' ) }
            shouldFailAssert { match( null, 'bbbb' ) }
            shouldFailAssert { match( 'cccc', null ) }
            shouldFailAssert { match( null, null   ) }
            shouldFailAssert { match( null, ''     ) }
            shouldFailAssert { match( '  ', null   ) }
        }
    }


    @Test
    void shouldMatchAgain()
    {
        def check = { String path, String pattern, boolean positiveCheck = true ->
            def result = generalBean.match( path, pattern )
            if ( positiveCheck ) { assert result;       log.info( "[$path] matches [$pattern]" ) }
            else                 { assert ( ! result ); log.info( "[$path] doesn't match [$pattern]" ) }
        }

        check( 'M1.xml',                                        '**/*' )
        check( 'M2.xml',                                        '**/*.xml' )
        check( 'M3.xml',                                        '*.xml' )
        check( 'M4.xml',                                        'M4.xml' )
        check( '.hudson/hudson.scm.CVSSCM.xml',                 '**/*' )
        check( '.hudson/hudson.scm.CVSSCM.xml',                 '**/*.xml' )
        check( '.hudson/aaa/bbb/someDir/hudson.scm.CVSSCM.xml', '**/someDir/*.xml' )
        check( '.hudson/aaa/bbb/someDir/hudson.scm.CVSSCM.xml', '**/aaa/bbb/someDir/*.xml' )
        check( '.hudson/aaa/bbb/someDir/hudson.scm.CVSSCM.xml', '**/aaa/**/someDir/*.xml' )
        check( '.hudson/aaa/bbb/someDir/hudson.scm.CVSSCM.xml', '**/aaa/**' )
        check( '.hudson/aaa/bbb/someDir/hudson.scm.CVSSCM.xml', '**/bbb/**' )
        check( '.hudson/aaa/bbb/someDir/hudson.scm.CVSSCM.xml', '**/someDir/**' )
        check( '.hudson/aaa/bbb/someDir/hudson.scm.CVSSCM.xml', '**/bbb/**/*.xml' )
        check( 'src/test/resources/configs/google-guice',       '**' )
        check( 'src/test/resources/configs/google-guice',        '**/google-guice' )
        check( 'src/test/resources/configs/google-guice',        '**/google-guice/**' )
        check( 'src/test/resources/configs/google-guice/1.txt',  '**/*.txt' )
        check( 'src/test/resources/configs/google-guice',        'src/test/resources/configs/google-guice' )
        check( 'src/test/resources/configs/google-guice/1.txt',  'src/test/resources/configs/google-guice/1.txt' )
        check( 'src/test/resources/configs/google-guice/1.txt',  'src/**/resources/**/google-guice/1.txt' )
        check( 'src/test/resources/configs/google-guice/1.txt',  'src/**/resources/**/**/1.txt' )
        check( 'src/test/resources/configs/google-guice/1.txt',  'src/**/resources/**/1.txt' )
        check( 'src/test/resources/configs/google-guice/1.txt',  'src/**/1.txt' )
        check( 'src/test/resources/configs/google-guice/1.txt',  '**/1.txt' )

        check( '/home/evgenyg_admin/java/agent/work/265f468bcb78a703/maven-hudson-plugin/full/src/test/resources/configs/gitorious-wsarena3-version1/config.xml',
               '**/*.xml' )
        check( '/home/evgenyg_admin/java/agent/work/265f468bcb78a703/maven-hudson-plugin/full/src/test/resources/configs/gitorious-wsarena3-version1/config.xml',
               '/**/*.xml' )
        check( '/home/evgenyg_admin/java/agent/work/265f468bcb78a703/maven-hudson-plugin/full/src/test/resources/configs/gitorious-wsarena3-version1/config.xml',
               '**' )
        check( '/home/evgenyg_admin/java/agent/work/265f468bcb78a703/maven-hudson-plugin/full/src/test/resources/configs/gitorious-wsarena3-version1/config.xml',
               '/**/' )

        check( 'src/test/resources/configs/google-guice',       '**/*.xml', false )
        check( 'src/test/resources/configs/google-guice',       'src/test/resources/configs/google-guice/a', false )
        check( 'src/test/resources/configs/google-guice',       'a/src/test/resources/configs/google-guice/a', false )
        check( 'src/test/resources/configs/google-guice',       'a/src/test/resources/configs/google-guice', false )
        check( 'src/test/resources/configs/google-guice/a.xml', '**/*.txt', false )
        check( 'src/test/resources/configs/google-guice/a.xml', '**/aaaaa/*.xml', false )
    }


    @Test
    @SuppressWarnings([ 'ConstantAssertExpression' ])
    void testTryIt()
    {
        generalBean.tryIt( 1, null ) {}
        shouldFailWith( RuntimeException )  { generalBean.tryIt( 1, String ) {}}
        shouldFailWith( RuntimeException )  { generalBean.tryIt( 1, String ) { 1 }}
        shouldFailAssert { shouldFailAssert { generalBean.tryIt( 1, String ) { 'aaaaa' }}}
        shouldFailAssert { shouldFailAssert { generalBean.tryIt( 1, Number ) { 33 + 44 }}}

        assert '12345' == generalBean.tryIt( 1, String ) { '12345' }
        assert 12345   == generalBean.tryIt( 1, Number ) {  12345  }
        assert 12345   == generalBean.tryIt( 1, Number ) { 12345 - 5 + 5 }

        def c =
        {
            int n, int max, String s ->
            def counter = 0
            generalBean.tryIt( max, String )
            {
                if (( ++counter ) == n ) { s }
                else                     { assert false, counter }
            }
        }

        shouldFailWith( RuntimeException )  { assert 'qwerty' == c( 0, 3, 'qwerty' ) }
        shouldFailAssert { assert 'qwerty1' == c( 1, 3, 'qwerty2' ) }
        shouldFailAssert { shouldFailAssert { assert 'qwerty' == c( 2, 3, 'qwerty' ) }}

        assert 'qwerty1' == c( 1, 3, 'qwerty1' )
        assert 'qwerty2' == c( 2, 3, 'qwerty2' )
        assert 'qwerty3' == c( 3, 3, 'qwerty3' )
        assert 'qwerty4' == c( 3, 4, 'qwerty4' )
        assert 'qwerty5' == c( 4, 5, 'qwerty5' )
        assert 'qwerty6' == c( 1, 5, 'qwerty6' )
    }


    @Test
    void testS()
    {
        assert 's' == generalBean.s( 0 )
        assert ''  == generalBean.s( 1 )
        assert 's' == generalBean.s( 2 )

        assert '5 attempts'   == "5 attempt${ generalBean.s( 5 ) }"
        assert '1 attempt'    == "1 attempt${ generalBean.s( 1 ) }"
        assert 'many chances' == "many chance${ generalBean.s( 1000 ) }"
        assert 'one chance'   == "one chance${ generalBean.s( 1 ) }"
    }


    @Test
    void testList()
    {
        assert [1, 2] == generalBean.list( [1, 2] as Integer[], 3 )
        assert [5]    == generalBean.list( []     as Integer[], 5 )
        assert [8]    == generalBean.list( null, 8 )
        assert [1]    == generalBean.list( [1]    as Integer[], null )
        assert [1, 2] == generalBean.list( [1, 2] as Integer[], null )
        assert []     == generalBean.list( []     as Integer[], null )
        assert []     == generalBean.list( null, null )

        assert ['1', '2'] == generalBean.list( ['1', '2'] as String[], '3' )
        assert ['5']      == generalBean.list( []         as String[], '5' )
        assert ['8']      == generalBean.list( null, '8' )
        assert ['1']      == generalBean.list( ['1']      as String[], null )
        assert ['1', '2'] == generalBean.list( ['1', '2'] as String[], null )
        assert []         == generalBean.list( []         as String[], null )
        assert []         == generalBean.list( null, null )
    }


    @Test
    void testChoose()
    {
        assert 3   == generalBean.choose( null, null, null, 3 )
        assert 3   == generalBean.choose( null, null, null, 3, null )
        assert '4' == generalBean.choose( null, null, null, null, '4', '5' )
        assert [:] == generalBean.choose( [:] )
        assert [:] == generalBean.choose( null, [:], null )
        assert []  == generalBean.choose( null, [], [:], null )
    }


    @Test
    void shouldExecute()
    {
        if ( ! generalBean.windows )
        {
            return
        }

        List<String> commands = [ 'java -version', 'groovy --version', 'gradle -version', 'mvn -version' ]
        File         tempFile = fileBean.tempFile( '.bat' )
        tempFile.write(( [ 'dir'    ] + commands.collect { "call $it" }).join( constantsBean.CRLF ))
        def          command  = tempFile.canonicalPath

        log.info( "Command to run: $command" )

        generalBean.with {
            assert 0 == execute( command )
            assert 0 == execute( command, ExecOption.CommonsExec )
            assert 0 == execute( command, ExecOption.ProcessBuilder )
            assert 0 == execute( command, ExecOption.Runtime )
        }

        fileBean.delete( tempFile )
    }


    @Test
    void testStars()
    {
        def check = {
            String expected, List<String> input, String prefix = '* ', int padSize = 0 ->
            assert expected.replaceAll( /^\s*/, '' ).
                            replaceAll( /\r?\n/, constantsBean.CRLF )  == generalBean.stars( input, prefix, padSize )
        }

        check( '',       [] )
        check( '* [aa]', [ 'aa' ] )
        check( '''
* [aa]
* [bb]
* [cc]''', [ 'aa', 'bb', 'cc' ] )

         check( '''
$ [1]
$ [22]
$ [333]''', [ '1', '22', '333' ], '$ ' )

         check( '''
% [1]
   % [22]
   % [333]''', [ '1', '22', '333' ], '% ', 3 )

        shouldFailAssert { shouldFailAssert {
            check( '''
* [aa]
* [bb]
* [cc]''', [ 'aa', 'bb', 'cc' ] )
        }}

        shouldFailAssert {
            check( '''
* [aa]
 * [bb]
* [cc]''', [ 'aa', 'bb', 'cc' ] )
        }

        shouldFailAssert {
            check( '''
$ [aa]
$ [cc]''', [ 'aa', 'cc' ], '$' )
        }

        shouldFailAssert { shouldFailAssert {
            check( '''
$ [aa]
$ [cc]''', [ 'aa', 'cc' ], '$ ' )
        }}

     }
}
