package com.github.goldin.gcommons

import com.github.goldin.gcommons.beans.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import spock.lang.Specification
import java.security.SecureRandom


/**
 * {@link BaseTest} copy.
 */
class BaseSpec extends Specification
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
    final Random         random        = new SecureRandom( new BigInteger( System.currentTimeMillis() as String ).toByteArray())

    static final String MAVEN_TEST_RESOURCE  = 'apache-maven-3.0.1'
    static final String GRADLE_TEST_RESOURCE = 'gradle-0.9'


    /**
     * Retrieves test archives map: name => unpacked size.
     * @return test archives map: name => unpacked size.
     */
    @SuppressWarnings([ 'GStringAsMapKey' ])
    protected Map<String, Long> testArchives()
    {
        [ ( "$MAVEN_TEST_RESOURCE".toString()) : 3344327L ] +
        ( System.getProperty( 'slowTests' ) ? [ ( "$GRADLE_TEST_RESOURCE".toString()) : 56052721L ] : [:] )
    }


    /**
     * Determines current operation system.
     */

    protected boolean isWindows(){ System.getProperty( 'os.name' ).toLowerCase().contains( 'windows' )}
    protected boolean isMac()    { System.getProperty( 'os.name' ).toLowerCase().contains( 'mac os'  )}
    protected boolean isLinux()  { System.getProperty( 'os.name' ).toLowerCase().contains( 'linux'   )}


    /**
     * Retrieves test resource specified.
     *
     * @param path resource path
     * @return test resource specified
     */
    @Requires({ path })
    @Ensures ({ result.file })
    protected final File testResource( String path )
    {
        def file = new File( 'src/test/resources', path )
        if ( file.file ) { return file }

        file = new File( 'build/testArchives', path )
        if ( file.file ) { return file }

        final alternativeFile = verifyBean.file( new File( 'build/testArchives', "${ fileBean.baseName( file ) }.zip" ))
        final tempDir         = fileBean.tempDirectory()

        fileBean.with {
            unpack( alternativeFile, tempDir )
            pack  ( tempDir, file )
            delete( tempDir )
        }

        verifyBean.file( file )
    }


    /**
     * {@link GroovyTestCase} wrappers
     */
    @SuppressWarnings([ 'GroovyAccessibility' ])
    @Requires({ errorClass && callbacks })
    @Ensures ({ result })
    protected List<String> shouldFail ( Class errorClass = AssertionError, Closure ... callbacks )
    {
        callbacks.collect { new GroovyTestCase().shouldFail( errorClass, it )}
    }


    /**
     * Writes dummy data to the file specified.
     * @param f file to write dummy data to
     * @return original file specified
     */
    @Requires({ f && data })
    @Ensures ({ result.is( f ) })
    File write( File f, String data = f.canonicalPath ){ assert f.parentFile.with { directory || mkdirs() }; f.write( data ); f }


    /**
     * Creates N files and directories of random depth in the directory specified.
     *
     * @param rootDirectory    directory to create the files in
     * @param n                number of files and directories to create
     * @param newEntryCallback callback to invoke each time new directory or file is created
     *
     * @return number of files created
     */
    @Requires({ rootDirectory && ( n > 0 ) && newEntryCallback })
    @Ensures({ ( result <= n ) && rootDirectory.listFiles() })
    protected int createRandomDirectory ( File rootDirectory, int n = 100, Closure newEntryCallback = {} )
    {
        assert rootDirectory && ( n > 0 ) && newEntryCallback
        fileBean.delete( rootDirectory )

        int nFiles = 0

        n.times {
            def path = '/'
            random.nextInt( 5 ).times { path += "${ random.nextInt( n ) }/" } // Random depth
            boolean isFile = random.nextBoolean()                             // File or folder

            path   += "${ random.nextInt( n ) }${ isFile ? '.txt' : '' }"
            nFiles += ( isFile ? 1 : 0 )

            new File( rootDirectory, path ).with {
                if ( file )
                {    // File already exists, will be overwritten
                    nFiles--
                }
                else
                {
                    isFile ? write(( File )   delegate ) : mkdirs()
                    newEntryCallback(( File ) delegate )
                }
            }
        }

        assert ( nFiles <= n ) && rootDirectory.listFiles()
        nFiles
    }
}
