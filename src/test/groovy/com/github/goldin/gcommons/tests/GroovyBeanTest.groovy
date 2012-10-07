package com.github.goldin.gcommons.tests

import com.github.goldin.gcommons.BaseTest
import groovy.util.logging.Slf4j
import org.junit.Test


/**
 * {@link com.github.goldin.gcommons.beans.GroovyBean} tests
 */
@Slf4j
class GroovyBeanTest extends BaseTest
{

    @Test
    void shouldEval()
    {
        groovyBean.with {
            assert null    == eval( null,            Object  )
            assert null    == eval( '',              Object  )
            assert null    == eval( ' ',             Object  )
            assert null    == eval( ' ' * 100,       Object  )
            assert null    == eval( '\r\n\t',        Object  )
            assert 12345   == eval( '12345',         Integer )
            assert '12345' == eval( '12345',         String  )
            assert 46      == eval( '12 + 34',       Integer )
            assert 46      == eval( '{{ 12 + 34 }}', Integer )
        }

        def check =
        {
            String expression, Object expectedResult, Object[] bindingObjects ->
            assert expectedResult == groovyBean.eval( expression, expectedResult.class, groovyBean.binding( *bindingObjects ))
        }

        def checkMap =
        {
            String expression, Object expectedResult, Map map, Object[] bindingObjects ->
            assert expectedResult == groovyBean.eval( expression, expectedResult.class, groovyBean.binding( map, *bindingObjects ))
        }

        check( '3 + 4',       7  )
        check( '3 - 4',       -1 )
        check( '{{ 3 + 4 }}', 7  )

        check( 's.size()',                   7,       's',    '1234567' )
        check( 's.substring( j )',           '567',   's',    '1234567', 'j', 4  )
        check( 's.substring( j ).reverse()', '76543', 's',    '1234567', 'j', 2  )
        check( 's[ j .. -1 ]',               '567',   's',    '1234567', 'j', 4  )
        check( 's[ j .. -1 ].reverse()',     '76543', 's',    '1234567', 'j', 2  )
        check( 'map.size()',                 0,       'map',  [:] )
        check( 'list.size()',                0,       'list', [] )
        check( 'map.size()',                 1,       'map',  [ a : 'b' ]  )
        check( 'map.isEmpty()',              false,   'map',  [ a : 'b' ]  )
        check( 'list.size()',                2,       'list', [ '1', '2' ] )

        check( 'someProperty',                           'vvv',    'some-property', 'vvv' )
        check( 'someProperty',                           '000',    'some.property', '000' )
        check( 'someProperty + anotherProperty',         'vvv000', 'some.property', 'vvv',    'another-property', '000' )
        check( 'someProperty.indexOf( anotherProperty )', 3,       'some-property', 'vvv000', 'another.property', '000' )

        check( 'lIiSsTt.size()',             2,       'l-ii-ss-tt', [ 1, 2 ] )
        check( 'lIiSsTt.size()',             3,       'l.ii-ss.tt', [ 1, 2, 'a' ] )
        check( 'lIiSsTt.size()',             4,       'l-ii.ss-tt', [ 1, 2, 'c', 6 ] )

        checkMap( 'lIiSsTt.size()',          4,  [:], 'l-ii.ss-tt', [ 1, 2, 'c', 6 ] )
        checkMap( 'lIiSsTt.size()',          4,  [lIiSsTt:[ 1, 2, 'c', 6 ]], 'l-ii.ss-tt', [ 1, 2, 'c', 6 ] )
        checkMap( 'lIiSsTt.size()',          4,  [lIiSsTt:[ 1, 2, 'c', 6 ]] )
        checkMap( 'lIiSsTt.size()',          4,  [lIiSsTt:[ 1, 2, 'c', 6 ]] )
        checkMap( 'a + b',                 100,  [ a : 46], 'b', 54 )
        checkMap( 'a.indexOf( b )',          3,  [ a : 'qwerty'], 'b', 'rty' )
        checkMap( '[*a, *b].size()',         10, [ a : [ 1, 2, 3, 4, 5 ]], 'b', [6, 7, 8, 9, 10] )

        shouldFailAssert { checkMap( '[*a, *b].size()',         11, [ a : [ 1, 2, 3, 4, 5 ]], 'b', [6, 7, 8, 9, 10] )}
        shouldFailAssert { check( '3 + 4',  7, 's'           ) }
        shouldFailAssert { check( '3 + 4',  7, 1, 2, 3       ) }
        shouldFailAssert { check( '3 + 4',  7, 1, 2, 3, 4, 5 ) }
        shouldFailAssert { check( '3 + 4',    6 ) }
        shouldFailAssert { check( 's.size()', 7, 's', '' ) }

    }


    @Test
    void shouldFixNames()
    {
        def check = {
            Map expected, Map input ->
            assert ( expected == groovyBean.fixNames( input ))
            log.info( "$input => $expected" )
        }

        check( [:], [:] )

        check( [ property : 'value' ],
               [ property : 'value' ] )

        check( [ someProperty           : 'value1',
                 someOtherProperty      : 'value2',
                 yetAnotherProperty     : 'value3' ],
               [ 'some-property'        : 'value1',
                 'some.other-property'  : 'value2',
                 'yet-another.property' : 'value3' ] )

        check( [ aBC         : 'value1',
                 aBCD        : 'value2' ],
               [ 'a...b---c' : 'value1',
                 'a.b.c.d'   : 'value2' ] )

        check( [ aaBbCc         : 'value1',
                 aaBbCcDd       : 'value2' ],
               [ 'aa...bb---cc' : 'value1',
                 'aa.bb.cc.dd'  : 'value2' ] )

        check( [ aBC                      : 'value1',
                 dBfCgDh                  : 'value2' ],
               [ '...a...b---c---'        : 'value1',
                 '-.-.d.bf.cg.dh..---...' : 'value2' ] )
    }

}
