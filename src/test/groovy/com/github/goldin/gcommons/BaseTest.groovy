package com.github.goldin.gcommons

import com.github.goldin.gcommons.beans.*

/**
 * Base class for the tests
 */
class BaseTest
{
    /**
     * Initializing all beans
     */
    final ConstantsBean  constantsBean = GCommons.constants()
    final VerifyBean     verifyBean    = GCommons.verify()
    final GeneralBean    generalBean   = GCommons.general()
    final IOBean         ioBean        = GCommons.io()
    final FileBean       fileBean      = GCommons.file()
    final NetBean        netBean       = GCommons.net()
    final GroovyBean     groovyBean    = GCommons.groovy()
    final AlgorithmsBean algBean       = GCommons.alg()


    /**
     * Retrieves test archives map: name => unpacked size.
     * @return test archives map: name => unpacked size.
     */
    protected Map<String, Long> testArchives()
    {
        [ 'apache-maven-3.0.1' : 3344327L ] + ( System.getProperty( 'slowTests' ) ? [ 'gradle-0.9' : 27848286L ] : [:] )
    }


    /**
     * Retrieves test resource specified.
     *
     * @param path resource path
     * @return test resource specified
     */
    protected final File testResource( String path )
    {
        final File file = [ 'src/test/resources', 'build/testArchives' ].collect{ new File( it, path )}.find { it.file }

        if ( ! file )
        {
            final alternativeFile = verifyBean.file( new File( 'build/testArchives', "${ fileBean.baseName( file ) }.zip" ))
            final tempDir         = fileBean.tempDirectory()

            fileBean.with {
                unpack( alternativeFile, tempDir )
                pack  ( tempDir, file )
                delete( tempDir )
            }
        }

        verifyBean.file( file )
    }


    /**
     * {@link GroovyTestCase} wrappers
     */
    protected String shouldFailWith   ( Class cl, Closure c ) { new GroovyTestCase().shouldFail( cl, c ) }
    protected String shouldFailAssert ( Closure c )           { new GroovyTestCase().shouldFail( AssertionError, c ) }


    /**
     * Retrieves test dir to be used for temporal output
     * @param dirName test directory name
     * @return test directory to use
     */
    protected File testDir( String dirName = System.currentTimeMillis() as String )
    {
        def caller  = ( StackTraceElement ) new Throwable().stackTrace.findAll { it.className.startsWith( 'com.github.goldin' ) }[ -1 ]
        def testDir = new File( "build/test/${ this.class.name }/${ caller.methodName }/$dirName" )

        fileBean.mkdirs( fileBean.delete( testDir ))
    }


    /**
     * Verifies both lists specified contain identical elements.
     *
     * @param l1 first list to check
     * @param l2 second list to check
     */
    protected void assertSameLists( List l1, List l2 )
    {
        assert l1.size() == l2.size()
        assert l1.every { l2.contains( it ) }
        assert l2.every { l1.contains( it ) }
    }

    /**
     * Verifies both maps specified contain identical elements.
     *
     * @param m1 first map to check
     * @param m2 second map to check
     */
    protected void assertSameMaps( Map m1, Map m2 )
    {
        assert m1.size() == m2.size()
        assert m1.every{ key, value -> m2[ key ] == value }
        assert m2.every{ key, value -> m1[ key ] == value }
    }
}
