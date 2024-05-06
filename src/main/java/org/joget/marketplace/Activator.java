package org.joget.marketplace;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {
    
    public static final String VERSION = "8.0.5";

    protected Collection<ServiceRegistration> registrationList;
    
    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(LikeDislike.class.getName(), new LikeDislike(), null));
        registrationList.add(context.registerService(LikeDislikeDatalistColumn.class.getName(), new LikeDislikeDatalistColumn(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}