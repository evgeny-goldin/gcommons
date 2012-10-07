package com.github.goldin.gcommons.beans

import com.github.goldin.gcommons.GCommons
import com.github.goldin.gcommons.util.VerifyEqualHelper
import groovy.util.logging.Slf4j

/**
 * Verification methods
 */
@Slf4j
class VerifyBean extends BaseBean
{
    private final VerifyEqualHelper helper = new VerifyEqualHelper()


    /**
     * Verifies all objects specified are nulls
     * @param objects objects to check
     */
    void isNull( Object ... objects )
    {
        assert objects != null

        for ( o in objects )
        {
            assert ( o == null ), "Object specified [$o] is not null"
        }
    }

    /**
     * Verifies objects specified are not null
     * @param objects objects to check
     * @return first object checked
     */
    @SuppressWarnings( 'UnnecessaryPublicModifier' )
    public <T> T notNull( T ... objects )
    {
        assert objects != null

        for ( o in objects )
        {
            assert ( o != null ), "Object specified [$o] is null"
        }

        first( objects )
    }


    /**
     * Verifies that files specified exist.
     * @param files files to check
     * @return  first file checked
     */
    File exists ( File ... files )
    {
        assert files != null

        for ( file in files*.canonicalFile )
        {
            assert file.exists(), "[$file] does not exist"
        }

        first( files )
    }


    /**
     * Verifies that files specified are existing files.
     * @param files files to check
     * @return  first file checked
     */
    File file ( File ... files )
    {
        assert files != null

        for ( f in files*.canonicalFile )
        {
            assert f.file, "[$f] ${ f.directory ? 'is a directory, should be a file' : 'does not exist' }"
        }

        first( files )
    }


    /**
     * Verifies files specified are actual files that are not empty.
     * @param files file to check
     * @return first file checked
     */
    File notEmptyFile ( File ... files )
    {
        assert files != null

        for ( file in files*.canonicalFile )
        {
            assert file.file,    "File [$file] does not exist"
            assert file.size() > 0 , "File [$file] is empty"
        }

        first( files )
    }


    /**
     * Verifies that directories specified are existing directories.
     * @param directories directories to check
     * @return  first directory checked
     */
    File directory ( File ... directories )
    {
        assert directories != null

        for ( d in directories*.canonicalFile )
        {
            assert d.directory, "[$d] ${ d.file ? 'is a file, should be a directory' : 'does not exist' }"
        }

        first( directories )
    }


    /**
     * Verifies two files or directories specified are equal.
     *
     * @param file1          file or directory
     * @param file2          another file or directory
     * @param verifyChecksum whether content checksum verification should be performed
     * @param pattern        include pattern, like "*.xml".
     *                       Only files matching the include pattern will be verified.
     *                       All files are verified if <code>null</code>.
     * @param endOfLine      Whether all and of lines should be normalized before comparing files.
     *                       Nothing is done if <code>null</code>,
     *                       normalized to "\r\n" if <code>"windows"</code>,
     *                       normalized to "\n" if any other value
     *
     * @return number of files checked and verified
     */
    int equal ( File    file1,
                File    file2,
                boolean verifyChecksum = true,
                String  pattern        = null,
                String  endOfLine      = null,
                boolean failIfNoFiles  = true )
    {
        assert file1.exists() && file2.exists()

        int nFiles =  [ file1, file2 ].every { it.file } ?
            helper.verifyEqualFiles       ( file1, file2, pattern, verifyChecksum, endOfLine ) :
            helper.verifyEqualDirectories ( file1, file2, pattern, verifyChecksum, endOfLine )

        if (( ! pattern ) && [ file1, file2 ].every { it.directory } )
        {
            long   dir1Size =  GCommons.file().directorySize( file1 )
            long   dir2Size =  GCommons.file().directorySize( file2 )
            assert dir1Size == dir2Size, \
                   "Directory sizes of [$file1.canonicalPath] and [$file2.canonicalPath] are not the same: [$dir1Size] != [$dir2Size]"
        }

        assert (( nFiles > 0 ) || ( ! failIfNoFiles )), "No files found${ pattern ? ' matching [' + pattern + '] pattern' : '' }."
        nFiles
    }


    /**
     * Verifies that Springs specified are not null or empty.
     * @param strings strings to check
     * @return first string checked
     */
    String notNullOrEmpty( String ... strings )
    {
        assert strings != null

        for ( s in strings )
        {
            assert s?.trim()?.length(), "String specified [$s] is null or empty"
        }

        first( strings )
    }


    /**
     * Verifies that Collections specified are not null or empty.
     * @param collections collections to check
     * @return first collection checked
     */
    @SuppressWarnings( 'UnnecessaryPublicModifier' )
    public <T> Collection<T> notNullOrEmpty( Collection<T> ... collections )
    {
        assert collections != null

        for ( c in collections )
        {
            assert c?.size(), "Collection specified $c is null or empty"
        }

        first( collections )
    }


    /**
     * Verifies object is an instance of class specified.
     *
     * @param o object to check
     * @param c class to check
     * @return object checked
     */
    @SuppressWarnings( 'UnnecessaryPublicModifier' )
    public <T> T isInstance( Object o, Class<T> c )
    {
        assert ( o != null ),     'Object specified is null'
        assert ( c != null ),     'Class specified is null'
        assert c.isInstance( o ), "Object specified is of class [${ o.getClass().name }], should be an instance of [${ c.name }]"

        (( T ) o )
    }
}
