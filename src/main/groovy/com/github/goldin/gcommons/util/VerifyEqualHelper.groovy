package com.github.goldin.gcommons.util

import static com.github.goldin.gcommons.GCommons.*
import com.github.goldin.gcommons.beans.BaseBean
import groovy.util.logging.Slf4j
import org.gcontracts.annotations.Requires


/**
 * {@link com.github.goldin.gcommons.beans.VerifyBean#equal(File, File, boolean, String)} helper class
 */
@Slf4j
class VerifyEqualHelper extends BaseBean
{
    /**
     * Verifies two files specified are equal, considering the pattern and returns 1 or 0
     * 1 if files matched the pattern
     * 0 otherwise
     */
    int verifyEqualFiles( File    file1,
                          File    file2,
                          String  pattern,
                          boolean verifyChecksum,
                          String  endOfLine,
                          boolean isLog = true )
    {
        assert file1.file && file2.file

        def file1Path = file1.canonicalPath
        def file2Path = file2.canonicalPath

        if (( ! pattern ) || [ file1Path, file2Path ].every{ general().match( it, pattern ) } )
        {
            if ( endOfLine )
            {
                boolean windows = ( 'windows' == endOfLine )
                String  crlf    = ( windows ? '\r\n' : '\n' )
                file1.write( file1.text.replaceAll( /\r?\n/, crlf ))
                file2.write( file2.text.replaceAll( /\r?\n/, crlf ))

                log.debug( "[$file1Path] and [$file2Path] have CrLf normalized to \"${ windows ? '\\r\\n' : '\\n' }\"" )
            }

            def  ( long file1Length, long file2Length ) = [ file1, file2 ]*.length()
            assert file1Length == file2Length,  "( [$file1Path] length [$file1Length] ) != ( [$file2Path] length [$file2Length] )"

            if ( verifyChecksum )
            {
                def ( String file1Checksum, String file2Checksum ) = [ file1, file2 ].collect { file().checksum( it, 'MD5' ) }

                assert file1Checksum  == file2Checksum,  \
                       "( [$file1Path] SHA-1 checksum [$file1Checksum] ) != ( [$file2Path] SHA-1 checksum [$file2Checksum] )"
            }

            if ( isLog )
            {   // File equality is not logged when verifying directories
                log.info ( "[$file1Path] = [$file2Path]" + ( pattern ? " ($pattern)" : '' ))
            }

            1
        }
        else
        {
            0
        }
    }


   /**
    * Verifies two directories specified are equal, considering the pattern and returns number of files verified
    */
    @Requires({ dir1.directory && dir2.directory })
    int verifyEqualDirectories( File    dir1,
                                File    dir2,
                                String  pattern,
                                boolean verifyChecksum,
                                String  endOfLine  )
    {
        def dir1Path          = dir1.canonicalPath
        def dir2Path          = dir2.canonicalPath
        def dir2VerifiedFiles = [] as Set
        int filesChecked      = 0

        /**
         * Verifying that each file in 'dir1' has a corresponding and equal file in 'dir2'
         */
        dir1.recurse {

            File dir1File ->
            File dir2File = new File( dir2, dir1File.canonicalPath.replace( dir1Path, '' ))

            filesChecked += checkFiles( dir1File, dir2File, dir1, dir2, pattern, verifyChecksum, endOfLine )

            dir2VerifiedFiles << dir2File
        }


        /**
         * If no pattern is applied - verifying that each file in 'dir2' was verified by a previous check
         * and has a corresponding file in 'dir1'
         */
        if ( ! pattern )
        {
            dir2.recurse {

                File dir2File ->

                if ( ! dir2VerifiedFiles.contains( dir2File ))
                {
                    File dir1File = new File( dir1, dir2File.canonicalPath.replace( dir2Path, '' ))

                    def dir1FilePath  = dir1File.canonicalPath
                    def dir2FilePath  = dir2File.canonicalPath

                    if ( dir1File.exists())
                    {
                        /**
                         * The file wasn't verified when we iterated "dir1" but probably it was added later
                         */

                        filesChecked += checkFiles( dir1File, dir2File, dir1, dir2, pattern, verifyChecksum, endOfLine )
                    }
                    else
                    {
                        if ( dir2File.file && (( ! pattern ) || general().match( dir2File.canonicalPath, pattern )))
                        {
                            assert false, "There's no file [$dir1FilePath] corresponding to file [$dir2FilePath]"
                        }
                        else if ( dir2File.directory && ( ! pattern ))
                        {
                            // Directory can only be missed when a pattern is applied
                            assert false, "There's no directory [$dir1FilePath] corresponding to directory [$dir2FilePath]"
                        }
                    }
                }
            }
        }

        log.info( "[$dir1Path] = [$dir2Path]" + ( pattern ? " ($pattern)" : '' ))
        filesChecked
    }


    /**
     * Checks two files/directories specified, considering the pattern and returns number of files checked:
     * 1 for two files, 0 for two directories
     */
    @SuppressWarnings( 'GroovyAssignmentToMethodParameter' )
    @Requires({ file1 && file2 && file1Dir && file2Dir })
    private int checkFiles ( File    file1,
                             File    file2,
                             File    file1Dir,
                             File    file2Dir,
                             String  pattern,
                             boolean verifyChecksum,
                             String  endOfLine )
    {
        def patternMatch = { File file, File fileUpDir, String p ->
            ( ! p ) || general().with { match( file.path, p ) || match( file.path, "$fileUpDir.path/$p" ) }
        }

        file1    = file1.canonicalFile
        file2    = file2.canonicalFile
        file1Dir = file1Dir.canonicalFile
        file2Dir = file2Dir.canonicalFile

        if ( file1.file )
        {
            if ( patternMatch( file1, file1Dir, pattern ))
            {
                if ( file2.file )
                {
                    assert ( patternMatch( file2, file2Dir, pattern )), \
                           "[$file2] doesn't match pattern [$pattern] while [$file1] does"

                    // Pattern is not passed - it's match is already checked
                    return verifyEqualFiles ( file1, file2, null, verifyChecksum, endOfLine, false )
                }

                assert false, "There's no file [$file2] corresponding to file [$file1]"
            }
        }
        else if ( file1.directory )
        {
            // Directories allowed to be missed if pattern is applied
            assert ( file2.directory || ( pattern )), \
                   "There's no directory [$file2] corresponding to directory [$file1]"
        }

        0
    }
}
