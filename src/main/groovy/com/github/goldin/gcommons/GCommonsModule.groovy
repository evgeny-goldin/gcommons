package com.github.goldin.gcommons

import com.google.inject.AbstractModule
import com.github.goldin.gcommons.beans.FileBean
import com.github.goldin.gcommons.beans.AlgorithmsBean
import com.github.goldin.gcommons.beans.ConstantsBean
import com.github.goldin.gcommons.beans.GeneralBean
import com.github.goldin.gcommons.beans.GroovyBean
import com.github.goldin.gcommons.beans.IOBean
import com.github.goldin.gcommons.beans.NetBean
import com.github.goldin.gcommons.beans.VerifyBean


/**
 * Default GCommons {@link com.google.inject.Module} implementation.
 */
class GCommonsModule extends AbstractModule
{
    @Override
    protected void configure ()
    {
        bind( AlgorithmsBean ).toInstance( new AlgorithmsBean())
        bind( ConstantsBean  ).toInstance( new ConstantsBean())
        bind( FileBean       ).toInstance( new FileBean())
        bind( GeneralBean    ).toInstance( new GeneralBean())
        bind( GroovyBean     ).toInstance( new GroovyBean())
        bind( IOBean         ).toInstance( new IOBean())
        bind( NetBean        ).toInstance( new NetBean())
        bind( VerifyBean     ).toInstance( new VerifyBean())
    }
}
