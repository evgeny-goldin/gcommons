package com.github.goldin.gcommons

import com.github.goldin.gcommons.util.MopHelper
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import org.slf4j.LoggerFactory
import com.github.goldin.gcommons.beans.*


/**
 * "GCommons" entry points
 */
@SuppressWarnings ([ 'StatelessClass', 'GroovyConstantNamingConvention', 'FieldName' ])
class GCommons
{
    private static       Injector                                           injector
    private static final Map<Class<? extends BaseBean>, ? extends BaseBean> beansCache = [:]

    static {
        /**
         * Adding new methods to Object and File
         */

        final mopHelper = new MopHelper()

        // noinspection GroovyUnresolvedAccess
        Object.metaClass.splitWith   = { Object[] args                       -> mopHelper.splitWith( delegate, args ) }

        // noinspection GroovyUnresolvedAccess
        File.metaClass.recurse       = { Map configs = [:], Closure callback -> mopHelper.recurse(( File ) delegate, configs, callback ) }

        // noinspection GroovyUnresolvedAccess
        File.metaClass.directorySize = { mopHelper.directorySize(( File ) delegate ) }

        configModule = new GCommonsModule()
        assert injector
    }


    /**
     * Replaces Guice {@link Injector} with the new one created using the {@link com.google.inject.Module} specified.
     *
     * @param m module to create the new injector from
     */
    static void setConfigModule ( Module m )
    {
        assert m
        final  t = System.currentTimeMillis()
        injector = injector( m )
        LoggerFactory.getLogger( GCommons ).debug( "GCommons injector initialized in ${ System.currentTimeMillis() - t } ms" )
    }


    /**
     * Creates new {@link Injector} instance using {@link Module} specified and verifies it by
     * making sure it knows to create all beans required.
     *
     * @param m configuration module to create the injector with
     * @return injector instance created and verified
     */
    private static Injector injector ( Module m )
    {
        assert m
        final  i = Guice.createInjector( m )

        [ AlgorithmsBean, ConstantsBean, FileBean, GeneralBean, GroovyBean,  IOBean, NetBean, VerifyBean ].each {
            Class c -> assert c.isInstance( i.getInstance( c ))
        }

        i
    }


    /**
     * Retrieves bean instance for the class specified.
     *
     * @param beanClass  bean class, extends {@link BaseBean}
     * @param refresh    whether a new instance should be retrieved from Guice injector
     *
     * @return bean instance for the class specified
     */
    private static <T extends BaseBean> T getBean( Class<T> beanClass, boolean refresh, Module m )
    {
        assert ( beanClass ) && BaseBean.isAssignableFrom( beanClass )

        if ( refresh ) { beansCache.clear() }

        final i    = ( m ? injector( m ) : injector )
        final bean = ( T ) ( beansCache[ beanClass ] = beansCache[ beanClass ] ?: i.getInstance( beanClass ))
        assert beanClass.isInstance( bean )
        bean
    }


    static AlgorithmsBean alg       ( boolean refresh = false, Module m = null ) { getBean( AlgorithmsBean, refresh, m ) }
    static ConstantsBean  constants ( boolean refresh = false, Module m = null ) { getBean( ConstantsBean,  refresh, m ) }
    static FileBean       file      ( boolean refresh = false, Module m = null ) { getBean( FileBean,       refresh, m ) }
    static GeneralBean    general   ( boolean refresh = false, Module m = null ) { getBean( GeneralBean,    refresh, m ) }
    static GroovyBean     groovy    ( boolean refresh = false, Module m = null ) { getBean( GroovyBean,     refresh, m ) }
    static IOBean         io        ( boolean refresh = false, Module m = null ) { getBean( IOBean,         refresh, m ) }
    static NetBean        net       ( boolean refresh = false, Module m = null ) { getBean( NetBean,        refresh, m ) }
    static VerifyBean     verify    ( boolean refresh = false, Module m = null ) { getBean( VerifyBean,     refresh, m ) }
}
