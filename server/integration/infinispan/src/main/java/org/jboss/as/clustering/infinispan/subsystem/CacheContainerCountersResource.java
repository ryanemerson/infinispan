package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.counter.configuration.CounterManagerConfigurationBuilder;
import org.infinispan.counter.configuration.Reliability;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * CacheContainerCountersResource
 *
 * @author Vladimir Blagojevic
 * @since 9.2
 */
public class CacheContainerCountersResource extends SimpleResourceDefinition {
    static final PathElement PATH = PathElement.pathElement(ModelKeys.COUNTERS, ModelKeys.COUNTERS_NAME);

    //atributes
    static final SimpleAttributeDefinition RELIABILITY = new SimpleAttributeDefinitionBuilder(ModelKeys.RELIABILITY,
            ModelType.STRING, false)
            .setXmlName(Attribute.RELIABILITY.getLocalName())
            .setAllowExpression(false)
            .setAllowedValues(Reliability.AVAILABLE.toString(), Reliability.CONSISTENT.toString())
            .setDefaultValue(new ModelNode().set(CounterManagerConfigurationBuilder.defaultConfiguration().reliability().toString()))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    static final SimpleAttributeDefinition NUM_OWNERS = new SimpleAttributeDefinitionBuilder(ModelKeys.NUM_OWNERS,
            ModelType.LONG, false)
            .setXmlName(Attribute.NUM_OWNERS.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode().set(CounterManagerConfigurationBuilder.defaultConfiguration().numOwners()))
            .build();


    static final AttributeDefinition[] ATTRIBUTES = { RELIABILITY, NUM_OWNERS };


    private final boolean runtimeRegistration;
    private final ResolvePathHandler resolvePathHandler;

    CacheContainerCountersResource(ResolvePathHandler resolvePathHandler, boolean runtimeRegistration) {
        super(PATH, new InfinispanResourceDescriptionResolver(ModelKeys.CACHE_CONTAINER, ModelKeys.COUNTERS),
                CacheConfigOperationHandlers.CONTAINER_CONFIGURATIONS_ADD, ReloadRequiredRemoveStepHandler.INSTANCE);

        this.resolvePathHandler = resolvePathHandler;
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration rr) {
        PathElement strongCounterPath = PathElement.pathElement(ModelKeys.STRONG_COUNTER);
        PathElement weakCounterPath = PathElement.pathElement(ModelKeys.WEAK_COUNTER);
        rr.registerSubModel(new CounterResource(strongCounterPath, resolvePathHandler, runtimeRegistration));
        rr.registerSubModel(new CounterResource(weakCounterPath, resolvePathHandler, runtimeRegistration));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        if (runtimeRegistration) {
            CounterConfigurationMetricsHandler.INSTANCE.registerMetrics(resourceRegistration);
        }
    }
}
