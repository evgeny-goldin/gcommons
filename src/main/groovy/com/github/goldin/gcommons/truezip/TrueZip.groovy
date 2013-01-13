package com.github.goldin.gcommons.truezip

import com.github.goldin.gcommons.beans.FileBean
import de.schlichtherle.io.DefaultArchiveDetector
import de.schlichtherle.io.GlobalArchiveDriverRegistry
import de.schlichtherle.io.ArchiveDetector
import de.schlichtherle.io.archive.spi.ArchiveDriver
import de.schlichtherle.io.archive.tar.TarDriver
import de.schlichtherle.io.archive.zip.ZipDriver
import de.schlichtherle.io.archive.tar.TarGZipDriver
import de.schlichtherle.io.Files
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * TrueZip library wrapper.
 */
final class TrueZip
{
    private TrueZip (){}

    private static DefaultArchiveDetector customFormatsArchiveDetector


    @Requires({ ( zipExtensions != null ) && ( tarExtensions != null ) && ( tarGzExtensions != null ) })
    static void setCustomArchiveFormats ( List<String> zipExtensions, List<String> tarExtensions, List<String> tarGzExtensions )
    {
        customFormatsArchiveDetector = new CustomFormatsArchiveDetector( zipExtensions, tarExtensions, tarGzExtensions )
    }

    static void resetCustomArchiveFormats()
    {
        customFormatsArchiveDetector = null
    }


    @Ensures ({ result })
    static Set<String> zipExtensions (){ driverExtensions( ZipDriver ) }

    @Ensures ({ result })
    static Set<String> tarExtensions (){ driverExtensions( TarDriver ) }

    @Ensures ({ result })
    static Set<String> tarGzExtensions(){ driverExtensions( TarGZipDriver ) }


    @SuppressWarnings([ 'GroovyAccessibility '])
    @Requires({ sourceArchive && archiveExtension && destinationDirectory })
    static void unpackArchive( File sourceArchive, String archiveExtension, File destinationDirectory )
    {
        final detector = new SingleFileArchiveDetector( sourceArchive, archiveExtension, customFormatsArchiveDetector )

        Files.cp_r( true,
                    new de.schlichtherle.io.File( sourceArchive, detector ),
                    destinationDirectory,
                    detector,
                    ArchiveDetector.NULL )
        umount()
    }


    @Requires({ sourceFile && filePath && destinationArchive })
    static void addFileToArchive( File sourceFile, String filePath, File destinationArchive )
    {
        de.schlichtherle.io.File.cp_p( sourceFile,
                                       new de.schlichtherle.io.File( destinationArchive, filePath, customFormatsArchiveDetector ))
    }


    static void umount (){ de.schlichtherle.io.File.umount() }


    @SuppressWarnings([ 'GroovyAccessibility' ])
    @Requires({ path })
    @Ensures ({ result })
    static ArchiveDriver getDefaultArchiveDriver( String path )
    {
        ArchiveDetector.DEFAULT.getArchiveDriver( path ) ?:
        GlobalArchiveDriverRegistry.INSTANCE.getArchiveDriver( path )
    }


    @SuppressWarnings([ 'GroovyAccessibility' ])
    @Requires({ requiredDriverClass })
    @Ensures ({ result })
    private static Set<String> driverExtensions ( Class<? extends ArchiveDriver> requiredDriverClass )
    {
        (( Map<String,?> ) GlobalArchiveDriverRegistry.INSTANCE ).findAll {
            def extension, driver ->
            final driverClass = (( driver instanceof ArchiveDriver ) ? driver.class :
                                 ( driver instanceof String        ) ? FileBean.class.classLoader.loadClass( driver, true ) :
                                                                       null )
            driverClass && requiredDriverClass.isAssignableFrom( driverClass )
        }.
        keySet()*.toLowerCase()
    }
}
