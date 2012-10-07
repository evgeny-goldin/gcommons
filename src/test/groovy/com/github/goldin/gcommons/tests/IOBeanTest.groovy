package com.github.goldin.gcommons.tests

import com.github.goldin.gcommons.BaseTest
import groovy.util.logging.Slf4j
import org.junit.Test

/**
 * {@link com.github.goldin.gcommons.beans.IOBean} tests
 */
@Slf4j
class IOBeanTest extends BaseTest
{
    @Test
    void testResource()
    {
        ioBean.with {
            assert   resource( 'emptyTestResource.txt' )
            assert   resource( 'emptyTestResource.txt' ).openStream()
            assert   resource( '/emptyTestResource.txt' )
            assert   resource( '/emptyTestResource.txt' ).openStream()
            assert ! resource( 'emptyTestResource.txt' ).openStream().available()
            assert   resource( 'testResource.txt' )
            assert   resource( 'testResource.txt' ).openStream()
            assert   resource( 'testResource.txt' ).openStream().available()
            assert   resource( '/testResource.txt' )
            assert   resource( '/testResource.txt' ).openStream()
            assert   resource( '/testResource.txt' ).openStream().available()
            assert   resource( 'gradle-0.9.tar.gz' )
            assert   resource( 'gradle-0.9.tar.gz' ).openStream()
            assert   resource( 'gradle-0.9.tar.gz' ).openStream().available()
            assert   resource( '/gradle-0.9.tar.gz' )
            assert   resource( '/gradle-0.9.tar.gz' ).openStream()
            assert   resource( '/gradle-0.9.tar.gz' ).openStream().available()

            shouldFailAssert { resource( 'emptyTestResourceAAA.txt' ) }
            shouldFailAssert { resource( 'testResourceAAA.txt' ) }
        }
    }

    @Test
    void testResourceText()
    {
        ioBean.with {
            assert '' ==  resourceText( 'emptyTestResource.txt' )
            assert '' !=  resourceText( 'testResource.txt' )
            assert "1-2-3-4:5-10:100<code>''''''</code>" ==  resourceText( 'testResource.txt' )

            shouldFailAssert { resourceText( 'emptyTestResourceAAA.txt' ) }
            shouldFailAssert { resourceText( 'testResourceAAA.txt' ) }
        }
    }
}
