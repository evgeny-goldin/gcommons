package com.github.goldin.gcommons.specs

import com.github.goldin.gcommons.BaseSpec
import com.github.goldin.gcommons.beans.ExecOption
import com.github.goldin.spock.extensions.time.Time
import groovy.util.logging.Slf4j
import java.util.regex.Pattern
import com.github.goldin.spock.extensions.with.With
import com.github.goldin.gcommons.GCommons


/**
 * {@link com.github.goldin.gcommons.beans.GeneralBean} Spock tests.
 */
@Slf4j
@Time( min = 10, max = 3000 )
class GeneralBeanSpec extends BaseSpec
{
    @Time( min = 0, max = 3000 )
    def 'gc-87: GeneralBean.executeWithResult()'( String        command,
                                                  List<String>  patterns,
                                                  List<Integer> options )
    {
        when:
        def regexes = []
        def results = []

        if ( linux || mac ) {
            regexes = patterns.collect { Pattern.compile( it ) }
            results = options.collect  {
                int option ->
                ExecOption o = [ ExecOption.CommonsExec, ExecOption.ProcessBuilder, ExecOption.Runtime ][ option ]
                log.info( "[$command][$o]" )
                generalBean.executeWithResult( command, o )
            }
        }

        then:
        results.every{ String result -> regexes.every { Pattern p -> p.matcher( result ).find() }}

        where:
        command            | patterns                                                       | options
        'ls'               | [ /\w/, /build\.gradle/, /\w+\.groovy/, /\w+\.(txt|gradle)/ ]  | [0, 1, 2]
        'cat build.gradle' | [ /codenarcRuleSetFiles/, /sourceCompatibility/ ]              | [0, 2]
        'git --version'    | [ /git version (\d+\.)+\d+/ ]                                  | [0, 2]
        'git status'       | [ /# On branch \w+/ ]                                          | [0, 2]
        'mvn --version'    | [ /Apache Maven (\d+\.)+\d+/, /Maven home/, /Java home/ ]      | [0, 2]
    }


    @SuppressWarnings([ 'UnnecessaryObjectReferences', 'AbcComplexity' ])
    @Time( min = 5, max = 200 )
    @With({ GCommons.general() })
    def 'check match()'()
    {
        expect:
        match( '/a/b/c/d', '/a/b/c/d' )
        match( '/a/b/c/d', '**/b/c/d' )
        match( '/a/b/c/d', '**/c/d' )
        match( '/a/b/c/d', '**/d' )
        match( '/a/b/c/d', '**/d' )
        match( '/a/b/c/d', '**' )
        match( '/a/b/c/d/1.txt', '**/*.*' )
        match( '/a/b/c/d/1.txt', '**/1.txt' )
        match( '/a/b/c/d/1.txt', '**/*.txt' )
        match( '/a/b/c/d/1.txt', '**/*.t*' )
        match( '/a/b/c/d/1.txt', '**/*.tx*' )
        match( '/a/b/c/d/1.txt', '**/*.txt*' )

        ! match( '/a/b/c/d', '**/*.*' )
        ! match( '/a/b/c/d/1.txt', '**/*.xml'  )
        ! match( '/a/b/c/d/3.xml', '**/*.xml2' )
        ! match( '/a/b/c/d/3.xml', '**/*.txt'  )
        ! match( '/a/b/c/d/3.xml', '**/*.x'    )
        ! match( '/a/b/c/d/3.xml', '**/*.xm'   )
        ! match( '/a/b/c/d/3.xml', '**/4.xml'  )
        ! match( '/a/b/c/d/3.xml', '**/3xml'  )
        ! match( '/a/b/c/d/3.xml', 'aaa'  )
        ! match( '/a/b/c/d/3.xml', 'bbb'  )

        match( 'c:\\path\\dir', 'c:\\path\\dir' )
        match( 'c:\\path\\dir', '**\\path\\dir' )
        match( 'c:\\path\\dir', '**\\dir' )
        match( 'c:\\path\\dir', '**' )
        match( 'c:\\path\\dir\\1.txt', '**/*.*' )
        match( 'c:\\path\\dir\\1.txt', '**/1.txt'  )
        match( 'c:\\path\\dir\\1.txt', '**/*.txt'  )
        match( 'c:\\path\\dir\\1.txt', '**/*.t*'   )
        match( 'c:\\path\\dir\\1.txt', '**/*.tx*'  )
        match( 'c:\\path\\dir\\1.txt', '**/*.txt*' )

        ! match( 'c:\\path\\dir', '**/*.*' )
        ! match( 'c:\\path\\dir\\1.txt', '**/*.xml'  )
        ! match( 'c:\\path\\dir\\8.xml', '**/*.xml2' )
        ! match( 'c:\\path\\dir\\8.xml', '**/*.txt'  )
        ! match( 'c:\\path\\dir\\8.xml', '**/*.x'    )
        ! match( 'c:\\path\\dir\\8.xml', '**/*.xm'   )
        ! match( 'c:\\path\\dir\\8.xml', '**/9.xml'  )
        ! match( 'c:\\path\\dir\\8.xml', '**/8xml'   )
        ! match( 'c:\\path\\dir\\8.xml', '8xml'      )
        ! match( 'c:\\path\\dir\\8.xml', '8xml/aaa'  )

        match( 'd:/some/path/dir',        'd:/some/path/dir' )
        match( 'd:/some/path/dir',        '**\\path\\dir' )
        match( 'd:/some/path/dir',        '**/path/dir' )
        match( 'd:/some/path/dir',        '**\\dir'   )
        match( 'd:/some/path/dir',        '**/dir'    )
        match( 'd:/some/path/dir',        '**'        )
        match( 'd:/some/path/dir/1.txt',  '**/*.*'    )
        match( 'd:/some\\path/dir/1.txt', '**/1.txt'  )
        match( 'd:/some/path/dir/1.txt',  '**/*.txt'  )
        match( 'd:/some\\path/dir/1.txt', '**/*.t*'   )
        match( 'd:/some/path/dir/1.txt',  '**/*.tx*'  )
        match( 'd:/some\\path/dir/1.txt', '**/*.txt*' )

        ! match( 'd:/some/path/dir',        '**/*.*'    )
        ! match( 'd:/some/path/dir/1.txt',  '**/*.xml'  )
        ! match( 'd:/some/path/dir/8.xml',  '**/*.xml2' )
        ! match( 'd:/some\\path/dir/8.xml', '**/*.txt'  )
        ! match( 'd:/some/path/dir/8.xml',  '**/*.x'    )
        ! match( 'd:/some/path/dir/8.xml',  '**/*.xm'   )
        ! match( 'd:/some\\path/dir/8.xml', '**/9.xml'  )
        ! match( 'd:/some/path/dir/8.xml',  '**/8xml'   )
        ! match( 'd:/some/path/dir/8.xml',  '8xml'      )
        ! match( 'd:/some/path/dir/8.xml',  '8xml/aaa'  )
    }
}
