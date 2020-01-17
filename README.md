# OSGishTestRunner

This is a bit of code that I have broken out of my APS project and made independent of that project after having someone mailing me asking if this could be used for other projects.

This provides an API to deploy an OSGi bundle in a test, thus running it as it would run in reality, but with the exception of modularity. The point here is to run with JUnits classpath (or whatever test framework you are using). Its only for testing functionality.

## Usage

Let your test class extend OTROSGiServiceTestTools. Then you can do this:

__Java__

    deploy( "<bundle name>" ).with( new MyBundleActivator() ).from( "<maven group>","<maven artifact>","<maven version>" );
 
    deploy( "<bundle name>" ).with( new MyBundleActivator() ).from( "<classpath root>") ;
 
    deploy( "<bundle name>" ).with( new MyBundleActivator() ).fromJar( "<jar file path>" );
    
    deploy( "<bundle name>" ).with( new MyBundleActivator() ).using( ["<path>", "<path>", ... ] );
    
    deploy( "<bundle name>" ).with( new MyBundleActivator() ).start();

__Groovy__

Since groovy allows for removing dots and parenthesis it can look like this in groovy:

    deploy "<bundle name>" with new MyBundleActivator() from "<maven group>","<maven artifact>","<maven version>"
 
    deploy "<bundle name>" with new MyBundleActivator() from "<classpath root>"
 
    deploy "<bundle name>" with new MyBundleActivator() fromJar "<jar file path>"
    
    deploy "<bundle name>" with new MyBundleActivator() using ["<path>", "<path>", ... ]
    
    deploy "<bundle name>" with new MyBundleActivator() start()

The first  of the four examples deploys a bundle from ~/.m2/repository using maven GAV.

The second deploys from a classpath root .

The third deploys from a jar file.

The fourth deploys from a list of paths to classes.

The fifth doesn't care about bundle content.

There is a **REALLY BIG NOTE** here: The different ways to specify what to deploy is only to be able to deliver Bundle content!  In my case I use this to do dependency injection using a generic BundleActivator that does the dependency injections. If your bundle never uses this information it does not matter what the content is.  Remember from a bit upp in this document: This does not modularize, this uses JUnit, etc classpath! So any "deployed" bundle must be on test classpath.  The deployment is quite faked. _The point is to actually start one or more bundles using their activators, and have events working so that service trackers work._

`from(...)`, `fromJar(jar)`, `using(...)` all end by calling `.start()`. In this version I've made the `start()` method public. In other words, it is possible to skip `from/fromJar/using` and just do `.start()`.

To be proper you should probably do something like this in your tests:

    ...
    try {
        ...
    } 
    finally {
        shutdown();
        hold().maxTime(500).unit(TimeUnit.MILLISECONDS).go();
    }

## Unsupported OSGi APIs

The following is a list of what is not supported. If anyone adds support for all or part of these, please do a pull request!

The reason for this is that this code is broken out of my APS project to be able to be used standalone, and I have only implemented what I actually use. Yes, I'm lazy :-).

### Bundle

Partly supported.

#### Not supported

- getState() -- always returns 0.
- start(options)
- start()
- stop(options)
- stop()
- update(input)
- update()
- uninstall()
- getLocation()
- hasPermission() -- always return true.
- getLastModified() -- always return 0.
- getSignerCertificates( signersType ) -- returns null.

### BundleContext

Partly supported.

#### Not supported

- installBundle( String location, InputStream input ) -- throws BundleException ( has API for that).
- installBundle( String location ) -- throws BundleException (OTROSGiServiceTestTools  has API for that).
- addFrameworkListener( FrameworkListener listener )
- removeFrameworkListener( FrameworkListener listener )
- ungetService( ServiceReference reference ) -- does nothing, always returns true.
- getDataFile( String filename ) -- Throws RuntimeException.

### ServiceReference

Partly supported.

#### Not supported

- isAssignableTo(Bundle bundle, String className) -- always returns true, should probably always reurn false ...
- compareTo(Object reference) -- always return 0.

### ServiceRegistration

Fully supported.
