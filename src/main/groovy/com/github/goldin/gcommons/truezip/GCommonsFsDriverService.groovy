package com.github.goldin.gcommons.truezip

import de.schlichtherle.truezip.fs.FsDriver
import de.schlichtherle.truezip.fs.FsScheme
import de.schlichtherle.truezip.fs.archive.zip.ZipDriver
import de.schlichtherle.truezip.fs.spi.FsDriverService
import de.schlichtherle.truezip.socket.sl.IOPoolLocator


class GCommonsFsDriverService extends FsDriverService
{
    private static final Map<FsScheme, FsDriver> DRIVERS = FsDriverService.newMap([
        [ 'sima', new ZipDriver( IOPoolLocator.SINGLETON ) ],
        [ 'hpi',  new ZipDriver( IOPoolLocator.SINGLETON ) ],
        [ 'sar',  new ZipDriver( IOPoolLocator.SINGLETON ) ]] as Object[][] )


    @Override
    Map<FsScheme, FsDriver> get() { DRIVERS }
}
