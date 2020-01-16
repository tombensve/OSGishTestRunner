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
¨ */
package se.natusoft.osgi.aps.runtime;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.UUID;

/**
 * Provides a ServiceRegistration implementation for testing.
 */
public class OTRServiceRegistration implements ServiceRegistration {
    //
    // Private Members
    //

    private UUID id = UUID.randomUUID();

    private String serviceName;
    private OTRServiceReference serviceReference;
    private OTRBundle bundle;

    //
    // Constructors
    //

    /**
     * Creates a new TestServiceRegistration instance.
     *
     * @param serviceName The name of the registered service.
     * @param serviceReference The reference of the registered service.
     */
    OTRServiceRegistration( String serviceName, OTRServiceReference serviceReference, OTRBundle bundle ) {
        this.serviceName = serviceName;
        this.serviceReference = serviceReference;
        this.bundle = bundle;
    }

    //
    // Methods
    //

    /**
     * Returns the name of the registered service.
     */
    public String getServiceName() {
        return this.serviceName;
    }

    @Override
    public ServiceReference getReference() {
        return this.serviceReference;
    }

    /**
     * Does nothing!
     */
    @Override
    public void setProperties(Dictionary properties) {
        serviceReference.setProperties(properties);
    }

    /**
     * Unregisters this service registration.
     */
    @Override
    public void unregister() {
        this.bundle.getServiceRegistry().unregisterService(this);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public boolean equals( OTRServiceRegistration otrServiceRegistration ) {
        return this.id.equals( otrServiceRegistration.id);
    }
}
