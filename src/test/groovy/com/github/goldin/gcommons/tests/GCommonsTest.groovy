package com.github.goldin.gcommons.tests

import static com.github.goldin.gcommons.GCommons.*
import com.github.goldin.gcommons.BaseTest
import com.github.goldin.gcommons.GCommons
import groovy.util.logging.Slf4j
import org.junit.Test
import com.github.goldin.gcommons.GCommonsModule


/**
 * {@link GCommons} entry points test
 */
@Slf4j
class GCommonsTest extends BaseTest
{
    @Test
    void shouldRetrieveBeans()
    {
        assert constants()
        assert constants( false )
        assert constants( true )

        assert verify()
        assert verify( false )
        assert verify( true )

        assert general()
        assert general( false )
        assert general( true )

        assert file()
        assert file( false )
        assert file( true )

        assert io()
        assert io( false )
        assert io( true )

        assert net()
        assert net( false )
        assert net( true )

    }


    @Test
    void shouldRefresh()
    {
        assert general() == general()
        assert general() == general( false )

        assert verify() == verify()
        assert verify() == verify( false )

        assert general() == general( true )
        assert verify()  == verify ( true )
        assert net()     == net    ( true )
    }


    @Test
    void shouldUseModule()
    {
        assert general() == general( false, new GCommonsModule())
        assert verify()  == verify ( false, new GCommonsModule())
        assert net()     == net    ( false, new GCommonsModule())

        assert general() != general( true,  new GCommonsModule())
        assert verify()  != verify ( true,  new GCommonsModule())
        assert net()     != net    ( true,  new GCommonsModule())

        final  a1 = alg()
        GCommons.configModule = new GCommonsModule()
        assert a1 == alg()
        assert a1 == alg( false )
        assert a1 != alg( true )
    }


    @Test
    @SuppressWarnings([ 'AbcComplexity', 'MethodSize' ])
    void testSplitWithDirectorySize()
    {
        def text1 = '1\n2\n3'
        def text2 = '''
11111111111111111
rrrrrrrrrrr
yyyyyyyyyyyyyyyyyyyyyyyyy
'''
        def text3 = '''
eqweqwdsadfaf
dfsafsas saf asf safasfa
wetqfasfdasfasf
'''
        def text4 = '''
d;akjcZL;KJCal;kf kl LK
QWRJALKJF DFK AFSLAKJF AKJ
AWD;    2394OI9RURAl    129ui
'''

        def mkdir     = { File f   -> fileBean.mkdirs( f.parentFile ); f }
        def eachLine  = { String s -> generalBean.splitWith( s, 'eachLine' )*.trim().findAll{ it }}
        def eachLineF = { File f   -> generalBean.splitWith( f, 'eachLine' )*.trim().findAll{ it }}

        assert [ '1', '2', '3' ]                                                                            == eachLine( text1 )
        assert [ '11111111111111111', 'rrrrrrrrrrr', 'yyyyyyyyyyyyyyyyyyyyyyyyy' ]                          == eachLine( text2 )
        assert [ 'eqweqwdsadfaf', 'dfsafsas saf asf safasfa', 'wetqfasfdasfasf'  ]                          == eachLine( text3 )
        assert [ 'd;akjcZL;KJCal;kf kl LK', 'QWRJALKJF DFK AFSLAKJF AKJ', 'AWD;    2394OI9RURAl    129ui' ] == eachLine( text4 )

        def filesDir = testDir( 'files' )
        def f1       = mkdir( new File( filesDir, '1.txt'     ))
        def f2       = mkdir( new File( filesDir, '1/2/3.txt' ))
        def f3       = mkdir( new File( filesDir, '5/6/8.txt' ))

        f1.write ( text1 )
        f1.append( text2 )

        f2.write ( text2 )
        f2.append( text3 )

        f3.write ( text3 )
        f3.append( text4 )

        assert eachLineF( f1 ) == [ '1', '2', '3', '11111111111111111', 'rrrrrrrrrrr', 'yyyyyyyyyyyyyyyyyyyyyyyyy' ]
        assert eachLineF( f2 ) == [ '11111111111111111', 'rrrrrrrrrrr', 'yyyyyyyyyyyyyyyyyyyyyyyyy', 'eqweqwdsadfaf', 'dfsafsas saf asf safasfa', 'wetqfasfdasfasf' ]
        assert eachLineF( f3 ) == [ 'eqweqwdsadfaf', 'dfsafsas saf asf safasfa', 'wetqfasfdasfasf', 'd;akjcZL;KJCal;kf kl LK', 'QWRJALKJF DFK AFSLAKJF AKJ', 'AWD;    2394OI9RURAl    129ui' ]


        fileBean.with {
            [( text1 + text2 ), ( text2 + text3 ), ( text3 + text4 )].bytes as List == [ f1, f2, f3 ]*.splitWith( 'eachByte' )
            assert directorySize( filesDir )                 == text1.size() + text2.size() + text2.size() + text3.size() + text3.size() + text4.size()
            assert directorySize( new File( filesDir, '1' )) == directorySize( new File( filesDir, '1/2' ))
            assert directorySize( new File( filesDir, '1' )) == text2.size() + text3.size()
            assert directorySize( new File( filesDir, '5' )) == directorySize( new File( filesDir, '5/6' ))
            assert directorySize( new File( filesDir, '5' )) == text3.size() + text4.size()
        }


        generalBean.with { constantsBean.with {

            List<String> l1 = splitWith( 'aa\nbb\ncc', 'eachLine', String )
            assert l1 == [ 'aa', 'bb', 'cc' ]

            List<File> l2 = splitWith( filesDir, 'eachFile', File )
            assert l2.each{ File f -> [ '1.txt', '1', '5' ].any{ f.name == it }}

            shouldFailAssert { splitWith( '1\n2',   'eachLine', File   ) }
            shouldFailAssert { splitWith( '1\n2',   'eachLine', Map    ) }
            shouldFailAssert { splitWith( filesDir, 'eachFile', String ) }
            shouldFailAssert { shouldFailAssert { splitWith( '1\n2\n3', 'eachLine', String ) }}
            shouldFailAssert { shouldFailAssert { splitWith( filesDir,  'eachFile', File   ) }}

            shouldFailAssert { splitWith( 'aa', ''          ) }
            shouldFailAssert { splitWith( 'aa', ''          ) }
            shouldFailAssert { splitWith( 'aa', '  '        ) }
            shouldFailAssert { splitWith( 'aa', null        ) }
            shouldFailAssert { splitWith( 'aa', 'opa'       ) }
            shouldFailAssert { splitWith( 'aa', 'eachLine1' ) }
            shouldFailAssert { splitWith( 'aa', 'size'      ) }
            shouldFailAssert { splitWith( 'aa', 'toString'  ) }

            shouldFailAssert { splitWith( USER_DIR_FILE, 'eachDi'   ) }
            shouldFailAssert { splitWith( USER_DIR_FILE, 'eachDirr' ) }
            shouldFailAssert { splitWith( USER_DIR_FILE, 'exists'   ) }
            shouldFailAssert { splitWith( USER_DIR_FILE, 'isFile'   ) }

            shouldFailAssert { shouldFailAssert { splitWith( 'aa', 'eachLine'         ) }}
            shouldFailAssert { shouldFailAssert { splitWith( 'aa', 'eachLine', String ) }}
            shouldFailAssert { shouldFailAssert { splitWith( 'aa\nbb', 'eachLine'         ) }}
            shouldFailAssert { shouldFailAssert { splitWith( 'aa\nbb', 'eachLine', String ) }}
            shouldFailAssert { shouldFailAssert { splitWith( 'aa\nbb\ncc', 'eachLine'         ) }}
            shouldFailAssert { shouldFailAssert { splitWith( 'aa\nbb\ncc', 'eachLine', String ) }}
            shouldFailAssert { shouldFailAssert { splitWith( USER_DIR_FILE, 'eachDir'        ) }}
            shouldFailAssert { shouldFailAssert { splitWith( USER_DIR_FILE, 'eachDir', File  ) }}
            shouldFailAssert { shouldFailAssert { splitWith( USER_DIR_FILE, 'eachFile'       ) }}
            shouldFailAssert { shouldFailAssert { splitWith( USER_DIR_FILE, 'eachFile', File ) }}

            assert ['aa']              == splitWith( 'aa', 'eachLine'         )
            assert ['aa']              == splitWith( 'aa', 'eachLine', String )
            assert ['aa', 'bb' ]       == splitWith( 'aa\nbb', 'eachLine'         )
            assert ['aa', 'bb' ]       == splitWith( 'aa\nbb', 'eachLine', String )
            assert ['aa', 'bb', 'cc' ] == splitWith( 'aa\nbb\ncc', 'eachLine'         )
            assert ['aa', 'bb', 'cc' ] == splitWith( 'aa\nbb\ncc', 'eachLine', String )

            assert splitWith( USER_DIR_FILE, 'eachDir'        ).every { it.directory }
            assert splitWith( USER_DIR_FILE, 'eachDir', File  ).every { it.directory }
            assert splitWith( USER_DIR_FILE, 'eachFile'       ).every { it.exists()  }
            assert splitWith( USER_DIR_FILE, 'eachFile', File ).every { it.exists()  }
        }}
    }
}
