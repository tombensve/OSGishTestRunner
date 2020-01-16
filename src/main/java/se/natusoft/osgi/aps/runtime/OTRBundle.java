/*
 * LICENSE
 *     Apache 2.0 (Open Source)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package se.natusoft.osgi.aps.runtime;

import org.osgi.framework.*;
import se.natusoft.osgi.aps.runtime.internal.ServiceRegistry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This provides an implementation of a Bundle.
 *
 * Start with APSRuntime class, it will create instances of this.
 */
public class OTRBundle implements Bundle {
    //
    // Private Members
    //

    private ServiceRegistry serviceRegistry;
    private Dictionary<Object, Object> headers = new Properties();
    private long id;
    private OTRBundleContext bundleContext = new OTRBundleContext( this );
    private Version version = new Version( 1, 0, 0 );
    private String symbolicName;
    private List<String> entryPaths = new LinkedList<>();
    private ClassLoader bundleClassLoader;

    //
    // Constructors
    //

    /**
     * Creates a new TestBundle.
     *
     * @param id              The id of the bundle.
     * @param symbolicName    The symbolic name of the bundle.
     * @param serviceRegistry The common service registry.
     */
    OTRBundle( long id, String symbolicName, ServiceRegistry serviceRegistry ) {
        this.id = id;
        this.symbolicName = symbolicName;
        this.serviceRegistry = serviceRegistry;
    }

    //
    // Methods
    //

    /**
     * Adds an entry to the bundle.
     *
     * @param entryPath The entry to add.
     */
    private void addEntryPath( BundleEntryPath entryPath ) {
        this.entryPaths.add( entryPath.getRelativePath() );
        if ( entryPath.getRelativePath().endsWith( "MANIFEST.MF" ) ) {
            loadManifest( entryPath );
        }
    }

    /**
     * Loads MANIFEST.MF and populates 'headers' with entries.
     *
     * @param manifestPath The source of the MANIFEST.MF file to read. Must either provide a JarFile or a full path.
     */
    private void loadManifest( BundleEntryPath manifestPath ) {

        if ( manifestPath.getJarFile() != null ) {
            JarFile jarFile = manifestPath.getJarFile();
            loadManifest( jarFile );
        } else {
            loadManifest( manifestPath.getFullPath() );
        }
    }

    /**
     * Loads a MANIFEST.MF file using a JarFile.
     *
     * @param jarFile The JarFile to get the MANIFEST.MF from.
     */
    @SuppressWarnings("WeakerAccess")
    void loadManifest( JarFile jarFile ) {
        _loadManifest( jarFile );
    }

    /**
     * Loads a MANIFEST.MF file using a string path.
     *
     * @param jarFilePath The path to the
     */
    @SuppressWarnings("WeakerAccess")
    void loadManifest( String jarFilePath ) {
        _loadManifest( jarFilePath );
    }

    /**
     * Loads a MANIFEST.MF file using an InputStream (which will be closed!).
     *
     * @param manifestStream The stream to load manifest from.
     */
    void loadManifest(InputStream manifestStream) {
        _loadManifest( manifestStream );
    }

    /**
     * Loads a Manifest using a JarFile or a String path.
     *
     * @param manifestResource The resource to load manifest from.
     */
    private void _loadManifest( Object manifestResource ) {
        Attributes mfAttrs;

        try {
            if ( manifestResource instanceof JarFile ) {

                JarFile jarFile = (JarFile) manifestResource;
                Manifest mf = jarFile.getManifest();
                mfAttrs = mf.getMainAttributes();

            } else if ( manifestResource instanceof String ) {

                InputStream is = new FileInputStream( (String) manifestResource );
                Manifest mf = new Manifest( is );
                is.close();
                mfAttrs = mf.getMainAttributes();

            } else if (manifestResource instanceof InputStream) {

                InputStream is = (InputStream)manifestResource;
                Manifest mf = new Manifest( is );
                is.close();
                mfAttrs = mf.getMainAttributes();

            } else {
                Object errResource = "null";
                if (manifestResource != null) {
                    errResource = manifestResource.getClass();
                }
                throw new RuntimeException( "loadManifest(resource) received object of unknown type: " +
                        errResource );
            }

            //System.out.println("    MANIFEST.MF entries:");
            for ( Map.Entry<Object, Object> entry : mfAttrs.entrySet() ) {
                //System.out.println("        "+ entry.getKey() + ": " + entry.getValue());
                this.headers.put( entry.getKey().toString(), entry.getValue().toString() );
            }
            System.out.println();

        } catch ( Exception e ) {
            throw new RuntimeException( "Failed to load bundle MANIFEST.MF", e );
        }
    }

    /**
     * Adds an entry to the bundle.
     *
     * @param entryPath The entry to add.
     */
    void addEntryPath( String entryPath ) {
        addEntryPath( new BundleEntryPath( entryPath ) );
    }

    /**
     * @return the internal service registry.
     */
    ServiceRegistry getServiceRegistry() {
        return this.serviceRegistry;
    }

    /**
     * A quickie method for providing a service instance.
     *
     * @param service The service to provide.
     */
    @SuppressWarnings("unused")
    public void addServiceInstance( Object service ) {
        Properties properties = new Properties();
        properties.setProperty( Constants.OBJECTCLASS, service.getClass().getInterfaces()[ 0 ].getName() );

        this.serviceRegistry.registerService(
                new OTRServiceRegistration(
                        service.getClass().getName(),
                        new OTRServiceReference( this.bundleContext, properties ),
                        this
                ),
                service,
                service.getClass().getInterfaces()[ 0 ]
        );
    }

    /**
     * Set the classloader to use for loading bundle resources.
     *
     * @param bundleClassLoader The class loader to set.
     */
    void setBundleClassLoader( ClassLoader bundleClassLoader ) {
        this.bundleClassLoader = bundleClassLoader;
    }

    /**
     * Supplies manifest headers.
     *
     * @param headers The headers to set.
     */
    @SuppressWarnings("unused")
    public void setHeaders( Dictionary<Object, Object> headers ) {
        this.headers = headers;
    }

    /**
     * Changes the bundle version. Default version is 1.2.3.
     *
     * @param version The new version to set.
     */
    public void setVersion( Version version ) {
        this.version = version;
    }

    /**
     * Since this is not a real Bundle with real content, you have to specify the simulated test content
     * you want to make available to getEntryPath().
     * <p>
     * Paths should always start with '/'!
     *
     * @param entryPaths A List of paths to add.
     */
    @SuppressWarnings("unused")
    public void setEntryPaths( List<BundleEntryPath> entryPaths ) {
        for ( BundleEntryPath path : entryPaths ) {
            addEntryPath( path );
        }
    }

    /**
     * Since this is not a real Bundle with real content, you have to specify the simulated test content
     * you want to make available to getEntryPath().
     * <p>
     * Paths should always start with '/'!
     *
     * @param paths A varargs of Strings, one for each path.
     */
    public void addEntryPaths( String... paths ) {
        for ( String path : paths ) {
            addEntryPath( new BundleEntryPath( path ) );
        }
    }

    /**
     * Since this is not a real Bundle with real content, you have to specify the simulated test content
     * you want to make available to getEntryPath().
     * <p>
     * Paths should always start with '/'!
     *
     * @param paths A varargs of Strings, one for each path.
     */
    public void addEntryPaths( BundleEntryPath... paths ) {
        for ( BundleEntryPath path : paths ) {
            addEntryPath( path );
        }
    }

    /**
     * This provides simulated content by reading the content list of a real jar in the local maven repository.
     * <p>
     * To be **very clear** this does not load nor use the actual files in the specified jar! It just provides
     * the paths that the real jar has. When running the real jar must be available on the classpath. The specified
     * jar is again **not added** to the classpath! So any jar provided here must either be the jar of the project
     * being tested or be added as a test dependency.
     *
     * @param group    The group id of the jar artifact.
     * @param artifact The artifact name.
     * @param version  The version of the artifact.
     */
    public void loadEntryPathsFromMaven( String group, String artifact, String version ) throws IOException {
        loadEntryPathsFromMaven( group, artifact, version, "" );
    }

    /**
     * This provides simulated content by reading the content list of a real jar in the local maven repository.
     * <p>
     * To be **very clear** this does not load nor use the actual files in the specified jar! It just provides
     * the paths that the real jar has. When running the real jar must be available on the classpath. The specified
     * jar is again **not added** to the classpath! So any jar provided here must either be the jar of the project
     * being tested or be added as a test dependency.
     *
     * @param group      The group id of the jar artifact.
     * @param artifact   The artifact name.
     * @param version    The version of the artifact.
     * @param classifier The classifier of the  jar. Can be null or blank.
     */
    private void loadEntryPathsFromMaven( String group, String artifact, String version, @SuppressWarnings("SameParameterValue") String classifier ) throws
            IOException {
        if ( classifier == null || classifier.trim().isEmpty() ) {
            classifier = "";
        } else {
            classifier = "-" + classifier.trim();
        }

        File jarFile = new File( System.getProperty( "user.home" ) );
        jarFile = new File( jarFile, ".m2/repository" );
        jarFile = new File( jarFile, group.replace( '.', '/' ) );
        jarFile = new File( jarFile, artifact );
        jarFile = new File( jarFile, version );
        jarFile = new File( jarFile, artifact + "-" + version + classifier + ".jar" );

        loadEntryPathsFromJar( jarFile );
    }

    /**
     * This provides simulated content by reading the content list of a real jar in the local maven repository.
     * <p>
     * To be **very clear** this does not load nor use the actual files in the specified jar! It just provides
     * the paths that the real jar has. When running the real jar must be available on the classpath. The specified
     * jar is again **not added** to the classpath! So any jar provided here must either be the jar of the project
     * being tested or be added as a test dependency.
     *
     * @param jarFile A path to a jar file.
     */
    void loadEntryPathsFromJar( File jarFile ) throws IOException {

        if ( !jarFile.exists() )
            throw new IllegalArgumentException( "File '" + jarFile + "'  does not exist!" );

        try ( final JarFile jar = new JarFile( jarFile ) ) {
            //System.out.println( "Loading the following paths:" );
            jar.stream().forEach( jarEntry -> {
                if ( !jarEntry.getName().trim().endsWith( "/" ) ) {
                    addEntryPath( new BundleEntryPath( jar, File.separator + jarEntry.getName() ) );
                    if ( jarEntry.getName().startsWith( "lib/aps" ) ) {
                        System.err.println( "WARNING: This bundle seems to contain another aps bundle!" );
                    }
                    //System.out.println( "    " + File.separator + jarEntry.getName() );
                }
            } );
        }
    }

    /**
     * Loads entry paths by doing a file scan at the specified root.
     *
     * @param relPath A path that is relative to the top maven parent project.
     */
    public void loadEntryPathsFromDirScan( String relPath ) {
        loadEntryPathsFromDirScan( new File( new MavenRootFile().getRoot(), relPath ) );
    }

    /**
     * Loads entry paths by doing a file scan at the specified root.
     *
     * @param root The root of the file scan.
     */
    @SuppressWarnings("WeakerAccess")
    public void loadEntryPathsFromDirScan( File root ) {
        new DirScanner( root ).stream().forEach( this::addEntryPath );
    }

    /**
     * Currently not supported. Returns 0.
     */
    @Override
    public int getState() {
        return 0;
    }

    /**
     * Does nothing.
     */
    @SuppressWarnings("RedundantThrows")
    @Override
    public void start( int options ) throws BundleException {
    }

    /**
     * Does nothing.
     */
    @SuppressWarnings("RedundantThrows")
    @Override
    public void start() throws BundleException {
    }

    /**
     * Does nothing.
     */
    @SuppressWarnings("RedundantThrows")
    @Override
    public void stop( int options ) throws BundleException {
    }

    /**
     * Does nothing.
     */
    @SuppressWarnings("RedundantThrows")
    @Override
    public void stop() throws BundleException {
    }

    /**
     * Does nothing.
     */
    @SuppressWarnings("RedundantThrows")
    @Override
    public void update( InputStream input ) throws BundleException {
    }

    /**
     * Does nothing.
     */
    @SuppressWarnings("RedundantThrows")
    @Override
    public void update() throws BundleException {
    }

    /**
     * Does nothing.
     */
    @SuppressWarnings("RedundantThrows")
    @Override
    public void uninstall() throws BundleException {
    }

    /**
     * Returns the added headers.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Dictionary getHeaders() {
        return this.headers;
    }

    /**
     * Returns the bundle id provided at creation.
     */
    @Override
    public long getBundleId() {
        return this.id;
    }

    /**
     * Currently just returns "/tmp". Not sure if that is more useful than "not supported".
     */
    @Override
    public String getLocation() {
        return "/tmp";
    }

    /**
     * Returns all registered services.
     */
    @Override
    public ServiceReference[] getRegisteredServices() {
        return this.serviceRegistry.getRegisteredServices();
    }

    /**
     * Currently this returns the same as getRegisteredServices()!
     */
    @Override
    public ServiceReference[] getServicesInUse() {
        return this.serviceRegistry.getServicesInUse();
    }

    /**
     * Not supported. Always returns true.
     */
    @Override
    public boolean hasPermission( Object permission ) {
        return true;
    }

    /**
     * Just returns the named resource from current ClassLoader.
     *
     * @param name Resource to get.
     */
    @Override
    public URL getResource( String name ) {
        if ( name.startsWith( "/" ) ) {
            name = name.substring( 1 );
        }
        ClassLoader loader = getClass().getClassLoader();
        if ( this.bundleClassLoader != null ) {
            loader = this.bundleClassLoader;
        }
        return loader.getResource( name );
    }

    /**
     * Returns any added headers.
     *
     * @param locale This is completely ignored.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Dictionary getHeaders( String locale ) {
        return this.headers;
    }

    /**
     * Returns the symbolic name provided at construction.
     */
    @Override
    public String getSymbolicName() {
        return this.symbolicName;
    }

    /**
     * Loads the specified call using the current ClassLoader.
     *
     * @param name Name of class to load.
     *
     * @throws ClassNotFoundException on failure to find class.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Class loadClass( String name ) throws ClassNotFoundException {
        if ( name.startsWith( "/" ) || name.startsWith( "." ) ) {
            name = name.substring( 1 );
        }
        // Note that we do not provide OSGi correct Bundle class loaders! This is for testing.
        // So we make use of JUnits classpath.
        return getClass().getClassLoader().loadClass( name );
    }

    /**
     * Returns the named resources by using the current ClassLoader.
     *
     * @param name Resource name to get.
     *
     * @throws IOException on any IO problems.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getResources( String name ) throws IOException {
        if ( name.startsWith( "/" ) ) {
            name = name.substring( 1 );
        }
        ClassLoader loader = getClass().getClassLoader();
        if ( this.bundleClassLoader != null ) {
            loader = this.bundleClassLoader;
        }
        return loader.getResources( name );
    }

    /**
     * Returns the entry paths added.
     *
     * @param path Only returns paths starting with this.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getEntryPaths( String path ) {
        Vector<String> paths = new Vector<>();
        for ( String entryPath : this.entryPaths ) {
            if ( entryPath.startsWith( path ) ) {
                paths.add( entryPath );
            }
        }
        return paths.elements();
    }

    /**
     * Returns an entry matching those provided by setEntryPaths(...) or addEntryPath(...), but in URL form.
     *
     * @param path The start path of the entry to get.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public URL getEntry( String path ) {
        Enumeration enumeration = getEntryPaths( path );
        if ( !enumeration.hasMoreElements() ) {
            return null;
        }
        try {
            return new URL( "file:" + enumeration.nextElement().toString() );
        } catch ( MalformedURLException mfe ) {
            return null;
        }
    }

    /**
     * @return Currently always returns 0.
     */
    @Override
    public long getLastModified() {
        return 0;
    }

    /**
     * Currently not supported. Will just return getEntryPaths(path).
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration findEntries( String path, String filePattern, boolean recurse ) {
        return getEntryPaths( path );
    }

    /**
     * Returns the BundleContext which in this case is an TestBundleContext.
     */
    @Override
    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    /**
     * Not supported! Returns null!
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Map getSignerCertificates( int signersType ) {
        return null;
    }

    /**
     * Returns 1.2.3 unless another version have been set.
     */
    @Override
    public Version getVersion() {
        return this.version;
    }
}
