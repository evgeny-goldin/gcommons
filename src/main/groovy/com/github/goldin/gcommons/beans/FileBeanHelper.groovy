package com.github.goldin.gcommons.beans

import static com.github.goldin.gcommons.GCommons.*
import de.schlichtherle.truezip.file.TFile
import de.schlichtherle.truezip.file.TVFS
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipFile
import de.schlichtherle.truezip.fs.FsDriverProvider
import de.schlichtherle.truezip.file.TArchiveDetector
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * {@link FileBean} helper methods.
 *
 * Class is marked "final" as it's not meant for subclassing.
 * Methods are marked as "protected" to allow package-access only.
 */
@SuppressWarnings( 'FinalClassWithProtectedMember' )
final class FileBeanHelper
{
    private final FileBean   fileBean
    private final AntBuilder ant


    @Requires({ fileBean && ant })
    protected FileBeanHelper( FileBean fileBean, AntBuilder ant )
    {
        this.fileBean = fileBean
        this.ant      = ant
    }


    /**
     * Retrieves packing tool name: Ant or TrueZip
     * @param isTrueZip whether packing tool is TrueZip
     * @return packing tool name: Ant or TrueZip
     */
    protected String toolName ( boolean isTrueZip ) { isTrueZip ? 'TrueZip' : 'Ant' }


    /**
     * Retrieves extensions recognized by drivers specified.
     * @param classes driver classes
     * @return extensions recognized by drivers specified
     */
    @Requires({ classes })
    @Ensures ({ result  })
    protected Set<String> extensions ( Class<? extends FsDriverProvider> ... classes )
    {
        classes.collect { it.newInstance().get().keySet()*.toString() }.flatten()
    }


    /**
     * Retrieves a "compression" value for Ant's tar/untar tasks.
     *
     * @param archiveExtension archive extension
     * @return archive compression according to
     *         http://evgeny-goldin.org/javadoc/ant/CoreTasks/tar.html
     *         http://evgeny-goldin.org/javadoc/ant/CoreTasks/untar.html
     */
    @SuppressWarnings( 'GroovyMultipleReturnPointsPerMethod' )
    protected String tarCompression( String archiveExtension )
    {
        switch ( verify().notNullOrEmpty( archiveExtension ))
        {
            case 'tar'     : return 'none'

            case 'gz'      :
            case 'tgz'     :
            case 'tar.gz'  : return 'gzip'

            case 'tbz2'    :
            case 'tar.bz2' : return 'bzip2'

            default        : throw new IllegalArgumentException( "Unknown tar extension [$archiveExtension]" )
        }
    }


    @Requires({ directory.directory && archive && ( compressionLevel in ( 0 .. 9 )) })
    @Ensures({ archive.file })
    protected void packAntZip ( File         directory,
                                File         archive,
                                List<String> includes,
                                List<String> excludes,
                                boolean      failIfNotFound,
                                String       fullpath,
                                String       prefix,
                                File         manifestDir,
                                boolean      update,
                                int          compressionLevel )
    {
        assert directory.directory && archive && ( compressionLevel in ( 0 .. 9 )) // GContracts seems to be not running in tests with Gradle 1.0-rc-3

        String includesStr = ( includes ?: [] ).join( ',' )
        String excludesStr = ( excludes ?: [] ).join( ',' )

        /**
         * http://evgeny-goldin.org/javadoc/ant/Tasks/zip.html
         */

        if ( update )
        {   /**
             * "ZIP files store file modification times with a granularity of two seconds.
             * If a file is less than two seconds newer than the entry in the archive, Apache Ant will not consider it newer"
             */
            ant.touch { fileset( dir : directory.canonicalPath, includes : includesStr, excludes : excludesStr )}
        }

        def zipMap = [  destfile          : archive.canonicalPath,
                        defaultexcludes   : 'no',
                        encoding          : 'UTF-8',
                        update            : update,
                        level             : compressionLevel,
                        whenempty         : failIfNotFound ? 'fail' : 'skip' ]

        def setMap  = [ erroronmissingdir : failIfNotFound,
                        dir               : directory.canonicalPath,
                        includes          : includesStr,
                        excludes          : excludesStr ]

        def s       = { String s -> s.with { startsWith( '/' ) || startsWith( '\\' ) } ? s.substring( 1 ) : s }

        setMap += ( fullpath ? [ fullpath : s( fullpath ) ] : [:] ) +
                  ( prefix   ? [ prefix   : s( prefix   ) ] : [:] )

        ant.zip( zipMap ) {
            zipfileset ( setMap )
            if ( manifestDir ) { zipfileset ( [ dir : manifestDir.canonicalPath ] )}
        }
    }


    private Map<String, String> tarFileSetMap( File         directory,
                                               List<String> includes,
                                               List<String> excludes,
                                               boolean      failIfNotFound,
                                               String       fullpath,
                                               String       prefix,
                                               String       filemode = null )
    {
        def s = { String s -> s.with { startsWith( '/' ) || startsWith( '\\' ) } ? s.substring( 1 ) : s }

        // noinspection GroovyOverlyComplexArithmeticExpression
        [ erroronmissingarchive : failIfNotFound as String,
          dir                   : directory.canonicalPath,
          includes              : ( includes ?: [] ).join( ',' ),
          excludes              : ( excludes ?: [] ).join( ',' ) ] +
        ( fullpath ? [ fullpath : s( fullpath ) ] : [:] ) +
        ( prefix   ? [ prefix   : s( prefix   ) ] : [:] ) +
        ( filemode ? [ filemode : filemode      ] : [:] )
    }


    @Requires({ directory.directory && archive })
    @Ensures({ archive.file })
    protected void packAntTar ( File         directory,
                                File         archive,
                                List<String> includes,
                                List<String> excludes,
                                boolean      failIfNotFound,
                                String       fullpath,
                                String       prefix,
                                File         manifestDir )
    {
        /**
         * http://evgeny-goldin.org/javadoc/ant/Tasks/tar.html
         */

        def tarMap = [ destfile        : archive.canonicalPath,
                       defaultexcludes : 'no',
                       longfile        : 'gnu',
                       compression     : tarCompression( fileBean.extension( archive )) ]

        def tarFileSets = []

        if ( includes.any { it ==~ /^.+\|${ constants().FILEMODE }$/ })
        {
            tarFileSets = includes.collect {
                String expression ->
                /**
                 * Splitting expression ('*.sh|755' or '*.sh') to include pattern and possible filemode (null if not used)
                 */
                def ( String include, String filemode ) = expression.findAll( ~/^(.+?)(\|(${ constants().FILEMODE }))?$/ ){ it[ 1, 3 ] }[ 0 ]
                tarFileSetMap( directory, [ include ], excludes, failIfNotFound, fullpath, prefix, filemode )
            }
        }
        else
        {
            tarFileSets = [ tarFileSetMap( directory, includes, excludes, failIfNotFound, fullpath, prefix ) ]
        }

        ant.tar( tarMap ) {
            tarFileSets.each { Map tarFileSetMap -> tarfileset ( tarFileSetMap ) }
            if ( manifestDir ) { tarfileset ( [ dir : manifestDir.canonicalPath ] )}
        }
    }


    @Requires({ directory.directory && archive })
    @Ensures({ archive.file })
    protected void packTrueZip ( File         directory,
                                 File         archive,
                                 List<String> includes,
                                 List<String> excludes,
                                 boolean      failIfNotFound,
                                 String       fullpath,
                                 String       prefix,
                                 File         manifestDir )
    {
        for ( File file in fileBean.files( directory, includes, excludes, false, false, failIfNotFound ))
        {
            final filePath = fullpath ?: ( prefix ?: '' ) + fileBean.relativePath( directory, file )
            new TFile( file, TArchiveDetector.NULL ).cp_p( new TFile( archive, filePath ))
        }

        if ( manifestDir )
        {
            final files = fileBean.files( manifestDir )
            assert files.size() == 1

            final manifestFile = verify().file( files.first())
            final manifestPath = fileBean.relativePath( manifestDir, manifestFile )
            new TFile( manifestFile, TArchiveDetector.NULL ).cp_p( new TFile( archive, manifestPath ))
        }

        TVFS.umount()
    }


    /**
     * Finds Zip entries that match user patterns specified.
     *
     * @param archive         Zip archive
     * @param zipEntries      Zip entries to scan
     * @param entries         patterns of entries to include,
     *                        if empty - all entries are unpacked but then {@code entriesExclude} should be defined
     * @param entriesExclude  patterns of entries to exclude,
     *                        if empty - no entries are excluded
     * @param failIfNotFound  whether execution should fail if one of include zip entries can't be matched
     * @return                Zip entries matched
     */
    @Requires({ archive && zipEntries && ( entries || entriesExclude ) })
    @Ensures({ result != null })
    protected Set<ZipEntry> findMatchingEntries ( File           archive,
                                                  List<ZipEntry> zipEntries,
                                                  Set<String>    entries,
                                                  Set<String>    entriesExclude,
                                                  boolean        failIfNotFound )
    {
        Closure<Boolean> isPatterned          = { it.contains( '?' ) || it.contains( '*' ) }

        Closure<Boolean> isEntryMatchPatterns = {
            boolean            matchIfEmpty,
            ZipEntry           entry,
            Collection<String> nonPatternedEntries,
            Collection<String> patternedEntries ->
                                                                                   // Zip entry is matched if:
            ( matchIfEmpty && ( ! ( nonPatternedEntries || patternedEntries ))) || // - Both pattern groups are empty and matchIfEmpty is true
            nonPatternedEntries.any{ entry.name == it }      ||                    // - It is matched by any non-patterned entry pattern
            patternedEntries.   any{ general().match( entry.name, it ) }           // - It is matched by any patterned entry pattern
        }

        Closure<Boolean> isEntryMatch = {
            ZipEntry           entry,
            Collection<String> nonPatternedEntries,
            Collection<String> patternedEntries,
            Collection<String> nonPatternedEntriesExclude,
            Collection<String> patternedEntriesExclude ->

            (   isEntryMatchPatterns( true,  entry, nonPatternedEntries,        patternedEntries )) &&
            ( ! isEntryMatchPatterns( false, entry, nonPatternedEntriesExclude, patternedEntriesExclude ))
        }

        Collection<String> patternedUserEntriesExclude    = entriesExclude.findAll( isPatterned )
        Collection<String> nonPatternedUserEntriesExclude = entriesExclude - patternedUserEntriesExclude
        List<ZipEntry>     matchingEntries                = []

        if ( entries )
        {
            for ( entry in entries )
            {
                boolean        patterned = isPatterned( entry )
                List<ZipEntry> l         = zipEntries.findAll { isEntryMatch( it, patterned ? [] : [ entry ],
                                                                                  patterned ? [ entry ] : [],
                                                                                  nonPatternedUserEntriesExclude,
                                                                                  patternedUserEntriesExclude ) }

                assert ( l || ( ! failIfNotFound )), "Failed to match entry [$entry] in [$archive]"
                matchingEntries.addAll( l )
            }
        }
        else
        {
            matchingEntries = zipEntries.findAll{ isEntryMatch( it, [], [], nonPatternedUserEntriesExclude, patternedUserEntriesExclude ) }
        }

        assert ( matchingEntries || ( ! failIfNotFound )), \
               "No Zip entries matched by $entries${ entriesExclude ? '/' + entriesExclude : '' } in [$archive]"
        matchingEntries as Set
    }


    /**
     * Unpacks zip entry to directory specified.
     *
     * @param archive              original Zip archive
     * @param zipFile              zip file instance created from original archive (optional)
     * @param zipEntry             zip entry to unpack
     * @param destinationDirectory directory to unpack the entry to
     * @param preservePath         whether entry path in Zip should be preserved
     *
     * @return whether entry was actually unpacked, <code>false</code> for directory entries ending with a "/"
     */
    @Requires({ archive.file && zipFile && zipEntry.name && destinationDirectory.directory })
    protected boolean unpackZipEntry( File     archive,
                                      ZipFile  zipFile,
                                      ZipEntry zipEntry,
                                      File     destinationDirectory,
                                      boolean  preservePath )
    {
        def entryName  = zipEntry.name
        def targetFile = new File( destinationDirectory, ( preservePath ? entryName : entryName.replaceAll( /^.*\//, '' ))).canonicalFile

        if ( entryName.endsWith( '/' ))
        {   // Directory entry
            assert zipEntry.size == 0, "Zip entry [$entryName] ends with '/' but it's size is not zero [$zipEntry.size]"
            if ( ! targetFile.directory )
            {
                fileBean.mkdirs( targetFile )
            }

            return false
        }

        // File entry
        fileBean.mkdirs( targetFile.parentFile )

        def bytesWritten = 0
        fileBean.delete( targetFile )

        new BufferedOutputStream( new FileOutputStream( targetFile )).withStream {
            OutputStream os ->

            def    is = zipFile.getInputStream( zipEntry )
            assert is, "Failed to read entry [$entryName] InputStream from [$archive]"

            is.eachByte( 10240 ) {
                byte[] buffer, int length ->
                bytesWritten += length
                os.write( buffer, 0, length )
            }
        }

        verify().file( targetFile )
        assert ( bytesWritten == zipEntry.size ) && ( targetFile.size() == zipEntry.size ), \
               "Zip entry [$entryName]: size is [$zipEntry.size], [$bytesWritten] bytes written, " +
               "[${ targetFile.size() }] file size of [$targetFile]"

        true
    }
}
