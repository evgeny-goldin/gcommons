package com.github.goldin.gcommons.beans

import static com.github.goldin.gcommons.GCommons.*
import com.github.goldin.gcommons.truezip.TrueZip
import com.google.common.collect.Sets
import groovy.io.FileType
import groovy.util.logging.Slf4j
import org.apache.tools.ant.DirectoryScanner
import org.apache.tools.ant.util.FileUtils
import org.apache.tools.zip.ZipFile
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import java.security.MessageDigest


/**
 * File-related helper utilities.
 */
@Slf4j
class FileBean extends BaseBean
{
    /**
     * Archive extensions supported by ZIP and TAR drivers
     */
    @SuppressWarnings ( 'StatelessClass' )
    Set<String> zipExtensions

    @SuppressWarnings ( 'StatelessClass' )
    Set<String> tarExtensions

    @SuppressWarnings ( 'StatelessClass' )
    Set<String> tarGzExtensions

    @SuppressWarnings ( 'StatelessClass' )
    Set<String> gzExtensions

    @SuppressWarnings ( 'StatelessClass' )
    Set<String> allExtensions


    private final FileBeanHelper helper = new FileBeanHelper( this, ant )


    /**
     * Default excludes that can be used when copying or packing files to exclude
     * standard SCM-related files like '.git' or '.svn'.
     */
    @SuppressWarnings( 'PublicInstanceField' )
    public final List<String> defaultExcludes =
    '''**/.cvsignore **/SCCS **/CVS **/.hg **/SCCS/** **/#*# **/.bzr/** **/.hgsubstate **/.bzrignore **/.gitignore
       **/.git **/.svn/** **/vssver.scc **/CVS/** **/.hg/** **/.hgignore **/.gitattributes **/.DS_Store **/._*
       **/.svn **/*~ **/.bzr **/.hgsub **/.hgtags **/.#* **/.git/** **/%*% **/.gitmodules'''.
    tokenize()*.trim().grep().sort().asImmutable()


    FileBean()
    {
        /**
         * We don't want Ant to interfere with its default excludes,
         * user should have a full control over what's copied or packed
         */
        defaultExcludes.each { ant.defaultexcludes( remove : it ) }
        resetCustomArchiveFormats()
    }


    void resetCustomArchiveFormats()
    {
        setExtensions( TrueZip.zipExtensions(),
                       TrueZip.tarExtensions(),
                       TrueZip.tarGzExtensions(),
                       [ 'gz' ])
    }


    @Requires({ formats })
    void setCustomArchiveFormats ( Map<String, List<String>> formats )
    {
        final List<String> zip   = []
        final List<String> tar   = []
        final List<String> tarGz = []

        formats.each {
            String archiveFormat, List<String> extensions ->
            assert archiveFormat && extensions

            final updateExtensions = ( 'zip'    == archiveFormat ) ? zip   :
                                     ( 'tar'    == archiveFormat ) ? tar   :
                                     ( 'tar.gz' == archiveFormat ) ? tarGz :
                                                                     null
            assert ( updateExtensions != null ), \
                   "Unrecognized custom archive format [$archiveFormat], " +
                   "supported formats are 'zip', 'tar' and 'tar.gz'"

            updateExtensions.addAll( extensions )
        }

        TrueZip.setCustomArchiveFormats( zip, tar, tarGz )
        setExtensions( this.zipExtensions   + zip,
                       this.tarExtensions   + tar,
                       this.tarGzExtensions + tarGz,
                       this.gzExtensions )
    }


    @SuppressWarnings([ 'GroovyOverlyComplexArithmeticExpression' ])
    @Requires({ ( zipExtensions != null ) && ( tarExtensions != null ) && ( tarGzExtensions != null ) && ( gzExtensions != null ) })
    private void setExtensions( Collection<String> zipExtensions,
                                Collection<String> tarExtensions,
                                Collection<String> tarGzExtensions,
                                Collection<String> gzExtensions )
    {
        this.zipExtensions   = zipExtensions.toSet().asImmutable()
        this.tarExtensions   = ( tarExtensions - tarGzExtensions ).toSet().asImmutable()
        this.tarGzExtensions = tarGzExtensions.toSet().asImmutable()
        this.gzExtensions    = gzExtensions.toSet().asImmutable()
        this.allExtensions   = ( zipExtensions + tarExtensions + tarGzExtensions + gzExtensions ).toSet().asImmutable()

        assert Sets.intersection( this.zipExtensions,   this.tarExtensions   ).empty
        assert Sets.intersection( this.zipExtensions,   this.tarGzExtensions ).empty
        assert Sets.intersection( this.zipExtensions,   this.gzExtensions    ).empty
        assert Sets.intersection( this.tarExtensions,   this.tarGzExtensions ).empty
        assert Sets.intersection( this.tarExtensions,   this.gzExtensions    ).empty
        assert Sets.intersection( this.tarGzExtensions, this.gzExtensions    ).empty

        log.info( "Extensions set: zip - ${ this.zipExtensions }, tar - ${ this.tarExtensions }, " +
                  "tar.gz - ${ this.tarGzExtensions }, gz - ${ this.gzExtensions }" )
    }


    /**
     * Creates a temp file.
     *
     * @param suffix temp file suffix
     * @return temp file created.
     */
    @SuppressWarnings( 'FileCreateTempFile' )
    @Ensures({ result.file && ( result.size() == 0 ) })
    File tempFile( String suffix = '' ) { File.createTempFile( GeneralBean.name, suffix ).canonicalFile }


    /**
     * Creates a temp directory.
     * @return temp directory created.
     */
    @Ensures({ result.directory && ( result.list().size() == 0 ) })
    File tempDirectory(){ mkdirs ( delete( tempFile())) }


    /**
     * Deletes files or directories specified. Directories are deleted recursively.
     *
     * @param deleteOnExitIfFailed whether file should be attempted to be deleted on JVM exit if deleting it now has failed
     * @param files                files or directories to delete
     * @return first object specified
     */
    File delete ( boolean deleteOnExitIfFailed = true, File ... files )
    {
        for ( f in files*.canonicalFile )
        {
            if ( f.exists())
            {
                if ( f.directory ) { delete( f.listFiles()) }
                if (( ! f.delete()) && deleteOnExitIfFailed )
                {
                    log.warn( "Failed to delete ${ f.file ? 'file' : 'directory' } [$f.canonicalPath], will be deleted on JVM exit" )
                    f.deleteOnExit()
                    addShutdownHook { delete( false, f )}
                }
            }
        }

        first( files )
    }


    /**
     * {@link File#mkdirs()} wrapper for directories specified.
     *
     * @param directories directories to create
     * @return first directory specified
     */
    File mkdirs ( File ... directories )
    {
        for ( directory in directories*.canonicalFile )
        {
            assert ( ! directory.file ), \
                   "Failed to create directory [$directory] - it is an existing file"
            assert (( directory.directory || directory.mkdirs()) && ( directory.directory )), \
                   "Failed to create directory [$directory]"
        }

        first( directories )
    }


    /**
     * Generates a checksum for the file specified.
     *
     * @param file file to generate a checksum for
     * @param algorithm checksum algorithm, supported by Ant's {@code <checksum>} task.
     * @return file's checksum
     */
    @Requires({ file.file && algorithm })
    @Ensures({ result })
    String checksum ( File file, String algorithm = 'SHA-1' )
    {
        assert file.file && algorithm // GContracts seems to be not running in tests with Gradle 1.0-rc-3

        StringBuilder checksum = new StringBuilder()
        MessageDigest md       = MessageDigest.getInstance( algorithm )
        file.canonicalFile.eachByte( 10 * 1024 ) {
            byte[] buffer, int n -> md.update( buffer, 0, n )
        }

        for ( byte b in md.digest())
        {
            String hex = Integer.toHexString(( 0xFF & b ) as int )
            checksum.append( "${( hex.length() < 2 ) ? '0' : '' }$hex".toString())
        }

        checksum.toString()
    }


    /**
     * Retrieves files (and directories, if required) given base directory and inclusion/exclusion patterns.
     * Symbolic links are not followed.
     *
     * @param directory          files base directory
     * @param includes           patterns to use for including files, all files are included if null
     * @param excludes           patterns to use for excluding files, no files are excluded if null
     * @param caseSensitive      whether or not include and exclude patterns are matched in a case sensitive way
     * @param includeDirectories whether directories included should be returned as well
     * @param failIfNotFound     whether execution should fail if no files were found
     * @param stripFileMode      whether filemode should be removed from include patterns: '*.sh|700' => '*.sh'
     *
     * @return files under base directory specified passing an inclusion/exclusion patterns
     */
    @SuppressWarnings([ 'JavaStylePropertiesInvocation' ])
    @Requires({ directory.directory || ( ! failIfNotFound ) })
    @Ensures({ result || ( ! failIfNotFound ) })
    List<File> files ( File         directory,
                       List<String> includes           = null,
                       List<String> excludes           = null,
                       boolean      caseSensitive      = true,
                       boolean      includeDirectories = false,
                       boolean      failIfNotFound     = true,
                       boolean      stripFileMode      = false )
    {
        List<File>       files           = []
        File             baseDirectory   = directory.canonicalFile
        DirectoryScanner scanner         = new DirectoryScanner()
        List<String>     scannerIncludes = ( stripFileMode ? includes*.replaceFirst( ~/\|${ constants().FILEMODE }$/, '' ) : includes )
        List<String>     scannerExcludes = excludes

        if ( directory.directory )
        {
            scanner.with {
                setBasedir( baseDirectory )
                setErrorOnMissingDir( true )
                setFollowSymlinks( false )
                setCaseSensitive( caseSensitive )
                setIncludes(( scannerIncludes ?: null ) as String[] )
                setExcludes(( scannerExcludes ?: null ) as String[] )

                scan()

                files = ( List<File> ) includedFiles.collect { verify().file( [ new File( baseDirectory, it ).canonicalFile ] as File[] ) } +
                        ( includeDirectories ? includedDirectories.collect { verify().directory( [ new File ( baseDirectory, it ).canonicalFile ] as File[] ) } :
                                               [] )
            }
        }

        assert (( files ) || ( ! failIfNotFound )), \
                "No${ scannerIncludes ? ' ' + scannerIncludes : '' } file(s) found in [$baseDirectory]" +
                "${ scannerExcludes ? ', excludes pattern is ' + scannerExcludes : '' }."
        files
    }


    /**
     * Copies file specified to destination directory provided.
     *
     * @param file      source file
     * @param directory destination directory
     * @param newName   file name in the target directory, same name by default
     *
     * @return destination file created
     */
    @Requires({ file.file && directory && newName })
    @Ensures({ result.file })
    File copy ( File file, File directory, String newName = file.name )
    {
        File sourceFile      = file.canonicalFile
        File destinationFile = new File( directory.canonicalFile, newName )

        mkdirs( delete( destinationFile ).parentFile )
        FileUtils.fileUtils.copyFile( sourceFile, destinationFile, null, true, true )
        assert ( sourceFile.size() == verify().file( destinationFile ).size())
        destinationFile
    }


    /**
     * Retrieves relative path of file inside directory specified.
     * For example: for directory <code>"C:\some"</code> and child file <code>"C:\some\folder\opa\1.txt"</code>
     * this function returns <code>"\folder\opa\1.txt"</code>.
     *
     * @param directory file's parent directory
     * @param file      directory's child file
     * @return          relative path of file inside directory specified, starts with "\" or "/"
     */
    @Requires({ directory && file })
    @Ensures({ result })
    @SuppressWarnings( 'UnnecessarySubstring' )
    String relativePath( File directory, File file )
    {
        String directoryPath = directory.canonicalPath
        String filePath      = file.canonicalPath

        assert filePath.startsWith( directoryPath ), \
               "File [$filePath] is not a child of [$directoryPath]"

        filePath.substring( directoryPath.size()).replace( '\\', '/' ) ?: '/'
    }


    /**
     * Copies directory specified into target directory.
     *
     * @param sourceDir source directory to copy
     * @param targetDir target directory to copy to
     * @param includes patterns of files to copy, optional - all files are included by default
     * @param excludes patterns of files not to copy, optional - no files are excluded by default
     * @param failIfNotFound whether execution should fail if no files were copied, optional - "true" by default
     *
     * @return target directory
     */
    @Requires({ sourceDir.directory && targetDir })
    File copyDir( File         sourceDir,
                  File         targetDir,
                  List<String> includes       = [ '**' ],
                  List<String> excludes       = [],
                  boolean      failIfNotFound = true )
    {
        File sourceDirectory = sourceDir.canonicalFile
        File targetDirectory = targetDir.canonicalFile

        for ( file in files( sourceDirectory, includes, excludes, true, true, failIfNotFound ))
        {
            File targetFile = new File( targetDirectory, relativePath( sourceDirectory, file ))

            if ( file.file )
            {
                copy( file, targetFile.parentFile )
            }
            else
            {
                assert file.directory && ( targetFile.directory || targetFile.mkdirs())
            }
        }

        targetDirectory
    }


    /**
     * Determines if there's a match between file type and file specified.
     *
     * @param fileType file type to check
     * @param file     file to check
     * @return <code>true</code>  if file matches the file type specified,
     *         <code>false</code> otherwise
     */
    boolean typeMatch( FileType fileType, File file )
    {
        def typeMatch  = ( fileType == FileType.ANY         ) ? true           :
                         ( fileType == FileType.FILES       ) ? file.file      :
                         ( fileType == FileType.DIRECTORIES ) ? file.directory :
                                                                null
        assert ( typeMatch != null ), "Unknown FileType [$fileType], should be an instance of [${ FileType.name }] enum"
        typeMatch
    }


    /**
     * Archives directory to archive specified. Empty directories are not archived!
     *
     * @param sourceDirectory    directory to archive
     * @param destinationArchive archive to pack the directory to
     * @param includes           patterns to use for including files, all files are included if null
     * @param excludes           patterns to use for excluding files, no files are excluded if null
     * @param failIfNotFound     whether execution should fail if no files were found
     * @param useTrueZip         whether TrueZip library should be used instead of Ant for packed, false by default
     * @param update             whether target archive should be updated if already exists,
     *                           <b>only works for ZIP files (jar, ear, war, hpi, etc)</b>
     * @param defaultExcludes    list of default exclude patterns that won't be displayed in log
     * @param fullpath           exact location in the archive to store the files,
     *                           <b>only works for ZIP and Tar/Tar.Gz files</b>
     * @param prefix             prefix to use when storing files in the archive,
     *                           <b>only works for ZIP and Tar/Tar.Gz files</b>
     * @param overwrite          whether archive should be overwritten if it's newer than source files
     * @param manifestDir        directory where manifest file to be packed is located
     * @param compressionLevel   Zip compression level, a number from 0 (no compression) to 9 (maximal compression).
     *                           <b>Only used when 'useTrueZip' is false.</b>
     *
     * @return archive packed
     */
    @SuppressWarnings([ 'GroovyMethodParameterCount', 'GroovyIfStatementWithTooManyBranches' ])
    @Requires({ sourceDirectory.directory && destinationArchive })
    @Ensures({ result.file && ( result == destinationArchive ) && ( result.size() > 0 ) })
    File pack ( File         sourceDirectory,
                File         destinationArchive,
                List<String> includes         = null,
                List<String> excludes         = null,
                boolean      useTrueZip       = false,
                boolean      failIfNotFound   = true,
                boolean      update           = false,
                List<String> defaultExcludes  = null,
                String       fullpath         = null,
                String       prefix           = null,
                boolean      overwrite        = true,
                File         manifestDir      = null,
                int          compressionLevel = 9 )
    {
        final time      = System.currentTimeMillis()
        final directory = sourceDirectory.canonicalFile
        final archive   = destinationArchive.canonicalFile
        final extension = extension( archive )
        final isZip     = zipExtensions.contains( extension )
        final isTar     = tarExtensions.contains( extension )
        final isTarGz   = tarGzExtensions.contains( extension )
        final isGz      = gzExtensions.contains( extension )

        assert allExtensions.contains( extension ), \
               "Packing [$archive] - unsupported archive extension \"$extension\", " +
               "supported extensions are \"${ allExtensions.join( '", "' )}\"."

        if ( update )
        {
            assert ( isZip && ( ! useTrueZip )), "'update' operation is only provided for Zip archives packed by Ant"
            verify().file( archive )
        }
        else if ( overwrite )
        {
            mkdirs( delete( archive ).parentFile )
        }
        else if ( archive.file )
        {
            assert ( ! overwrite )
            final maxLastModified = files( directory, includes, excludes, false, false, failIfNotFound, true )*.lastModified().max()
            if (  maxLastModified < archive.lastModified())
            {
                log.info( "Packing [$archive] is skipped - [$directory] doesn't contain newer files" )
                return archive
            }
        }

        assert ( ! ( fullpath && prefix )), "Both 'fullpath' and 'prefix' cannot be set"

        if ( manifestDir ) { assert files( manifestDir ).size() == 1 }

        try
        {
            def patterns = "${ includes ?: '' }/${ (( excludes ?: [] ) - defaultExcludes ) ?: '' }"
            patterns     = (( patterns == '/'         ) ? ''             :
                            ( patterns.endsWith( '/' )) ? " ($includes)" : " ($patterns)" )

            log.info( "Packing [$directory$patterns] to [$archive] using ${ helper.toolName( useTrueZip )}" )

            assert files( directory, includes, excludes, false, false, failIfNotFound, isTar && ( ! useTrueZip ))

            if ( useTrueZip )
            {
                assert ( ! isGz ), "TrueZip provides no support for packing .gz archives. Use .tar.gz or Ant"
                helper.packTrueZip( directory, archive, includes, excludes, failIfNotFound, fullpath, prefix, manifestDir )
            }
            else if ( isZip )
            {
                helper.packAntZip ( directory, archive, includes, excludes, failIfNotFound, fullpath, prefix, manifestDir, update, compressionLevel )
            }
            else if ( isTar || isTarGz )
            {
                helper.packAntTar ( directory, archive, includes, excludes, failIfNotFound, fullpath, prefix, manifestDir, tarExtensions, tarGzExtensions )
            }
            else if ( isGz )
            {
                helper.packAntGz ( directory, archive, includes, excludes )
            }
            else
            {
                throw new RuntimeException( "Unrecognized archive [$archive]" )
            }

            log.info( "[$directory$patterns] packed to [$archive] " +
                      "(${( System.currentTimeMillis() - time ).intdiv( 1000 )} sec)" )

            archive
        }
        catch ( e )
        {
            throw new IOException( "Failed to pack [$directory] to [$archive] using ${ helper.toolName( useTrueZip )}",
                                   e )
        }
    }


    /**
     * Unpacks an archive file to the directory specified.
     * Note: target directory is deleted before operation starts!
     *
     * @param sourceArchive        archive file to unpack
     * @param destinationDirectory directory to unpack the file to
     * @param useTrueZip           whether TrueZip library should be used instead of Ant for unpacking,
     *                             true by default
     *
     * @return destination directory where archive was unpacked
     */
    @SuppressWarnings([ 'GroovyIfStatementWithTooManyBranches' ])
    @Requires({ sourceArchive.file && ( sourceArchive.length() > 0  ) && destinationDirectory })
    @Ensures({ result.directory && ( result == destinationDirectory ) })
    File unpack ( File    sourceArchive,
                  File    destinationDirectory,
                  boolean useTrueZip = true )
    {
        final time      = System.currentTimeMillis()
        final archive   = sourceArchive.canonicalFile
        final directory = destinationDirectory.canonicalFile
        final extension = extension( archive )

        assert allExtensions.contains( extension ), \
               "Unpacking [$archive] - unsupported archive extension \"$extension\", " +
               "supported extensions are \"${ allExtensions.join( '", "' )}\"."
        try
        {
            if ( directory.file ) { delete( directory ) }
            mkdirs( directory )

            log.info( "Unpacking [$archive] to [$directory] using ${ helper.toolName( useTrueZip )}" )

            if ( useTrueZip )
            {
                assert ( ! gzExtensions.contains( extension )), "TrueZip provides no support for unpacking .gz archives. Use .tar.gz or Ant"
                TrueZip.unpackArchive( archive, extension, directory )
            }
            else if ( zipExtensions.contains( extension ))
            {
                ant.unzip( src  : archive.canonicalPath,
                           dest : directory.canonicalPath )
            }
            else if ( tarExtensions.contains( extension ) || tarGzExtensions.contains( extension ))
            {
                ant.untar( src         : archive.canonicalPath,
                           dest        : directory.canonicalPath,
                           compression : helper.tarCompression( extension, tarExtensions, tarGzExtensions ))
            }
            else if ( gzExtensions.contains( extension ))
            {
                ant.gunzip( src  : archive.canonicalPath,
                            dest : directory.canonicalPath )
            }
            else
            {
                throw new RuntimeException( "Unrecognized archive [$sourceArchive.canonicalPath]" )
            }

            log.info( "[$archive] unpacked to [$directory] " +
                      "(${( System.currentTimeMillis() - time ).intdiv( 1000 )} sec)" )

            directory
        }
        catch ( e )
        {
            throw new IOException( "Failed to unpack [$archive] to [$directory] using ${ helper.toolName( useTrueZip )}",
                                   e )
        }
    }

    /**
     * Unpack ZIP entries specified to the directory provided.
     *
     * @param sourceArchive        ZIP file to unpack
     * @param destinationDirectory directory to unpack the file to
     * @param zipEntries           ZIP entries to unpack,
     *                             if empty - all entries are unpacked but then {@code zipEntriesExclude} should be defined
     * @param zipEntriesExclude    ZIP entries to exclude from unpacking,
     *                             if empty - no entries are excluded
     * @param preservePath         whether entry path should be preserved
     * @param failIfNotFound       whether execution should fail if one of include zip entries can't be matched
     *
     * @return                     directory where entries were unpacked
     */
    @Requires({ sourceArchive.file && destinationDirectory && ( zipEntries != null ) && ( zipEntriesExclude != null ) })
    @Ensures({ result })
    File unpackZipEntries ( File         sourceArchive,
                            File         destinationDirectory,
                            List<String> zipEntries,
                            List<String> zipEntriesExclude = [],
                            boolean      preservePath      = true,
                            boolean      failIfNotFound    = true )
    {
        File        archive        = sourceArchive.canonicalFile
        File        directory      = destinationDirectory.canonicalFile
        Closure     c              = { List<String> l -> (( Collection<String> ) l*.trim().grep())*.replace( '\\', '/' )*.replaceAll( /^\//, '' ) as Set }
        Set<String> entries        = c( zipEntries )
        Set<String> entriesExclude = c( zipEntriesExclude )

        assert ( entries || entriesExclude ),                 "Both 'zipEntries' and 'zipEntriesExclude' are empty, at least one of them should be defined"
        assert zipExtensions.contains( extension( archive )), "File [$archive] is not recognized as ZIP file"

        ZipFile zipFile = null

        try
        {
            if ( directory.file ) { delete( directory ) }
            mkdirs( directory )

            log.info( "Unpacking [$archive] Zip entries to [$directory]" )

            long time          = System.currentTimeMillis()
            zipFile            = new ZipFile( archive )
            int matchedEntries = helper.findMatchingEntries( archive, zipFile.entries.toList(), entries, entriesExclude, failIfNotFound ).
                                 collect { helper.unpackZipEntry( archive, zipFile, it, directory, preservePath ) }.
                                 grep().size()

            assert ( directory.directory && ( directory.listFiles() || ( ! failIfNotFound )))

            log.info( "[$archive] [$matchedEntries] ${ matchedEntries == 1 ? 'entry' : 'entries' } " +
                      "unpacked to [$directory] (${( System.currentTimeMillis() - time ).intdiv( 1000 )} sec)" )

            directory
        }
        catch ( e )
        {
            throw new IOException( "Failed to unpack [$archive] Zip entries to [$directory]", e )
        }
        finally
        {
            if ( zipFile ) { zipFile.close() }
        }
    }


    /**
     * Retrieves file's base name.
     *
     * @param f file to retrieve its base name
     * @return file base name, empty for ".sth" file names.
     */
    @Requires({ f })
    @Ensures({ result != null })
    String baseName ( File f )
    {
        final extension = extension( f )
        extension ? f.name.substring( 0, f.name.length() - extension.length() - 1 ) :
                    f.name
    }


    /**
     * Retrieves file's extension.
     *
     * @param f file to retrieve its extension
     * @return file extension or empty {@code String} if it is missing
     */
    @Requires({ f })
    @Ensures({ result != null })
    String extension ( File f )
    {
        f.name.with {
            toLowerCase().endsWith( '.tar.gz'  ) ? substring( size() - 'tar.gz'.size())  :
            toLowerCase().endsWith( '.tar.bz2' ) ? substring( size() - 'tar.bz2'.size()) :
                                                   lastIndexOf( '.' ).with { ( delegate > -1 ) ? substring((( int ) delegate ) + 1 ) : '' }
        }
    }


    /**
     * {@code "File.metaClass.directorySize"} wrapper - calculates directory size.
     *
     * @param directory directory to read
     * @return directory size in bytes
     */
    @Requires({ directory.directory })
    long directorySize( File directory ) { directory.canonicalFile.directorySize() }


    /**
     * {@code "File.metaClass.recurse"} wrapper - iterates directory recursively.
     *
     * @param directory directory to iterate recursively
     * @param configs   configurations map
     * @param callback  callback to invoke
     */
    @Requires({ directory.directory })
    List<File> recurse ( File directory, Map configs = [:], Closure callback = {}) { directory.canonicalFile.recurse( configs, callback ) }
}
