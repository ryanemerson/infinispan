package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Optional;

import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.counter.api.WeakCounter;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.infinispan.SecurityActions;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource
 * /subsystem=infinispan/cache-container=X/counter=*
 *
 * @author Pedro Ruivo
 * @author Vladimir Blagojevic
 * @since 9.2
 */
public class CounterResource extends SimpleResourceDefinition {

    //atributes
    static final AttributeDefinition CONFIGURATION = new SimpleAttributeDefinitionBuilder(ModelKeys.CONFIGURATION,
            ModelType.STRING, false)
            .setXmlName(Attribute.CONFIGURATION.getLocalName())
            .setAllowExpression(false)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = { CONFIGURATION };

    // operations

    private static final OperationDefinition COUNTER_RESET = buildOperation("counter-reset").build();
    private static final OperationDefinition COUNTER_INCREASE = buildOperation("counter-increase").build();
    private static final OperationDefinition COUNTER_DECREASE = buildOperation("counter-decrease").build();

    private final boolean runtimeRegistration;

    public CounterResource(PathElement pathElement, ResourceDescriptionResolver descriptionResolver,
            ResolvePathHandler resolvePathHandler, AbstractAddStepHandler addHandler,
            OperationStepHandler removeHandler, boolean runtimeRegistration) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
        this.runtimeRegistration = runtimeRegistration;
    }

    public CounterResource(PathElement pathElement, ResolvePathHandler resolvePathHandler,
            boolean runtimeRegistration) {
        this(pathElement, new InfinispanResourceDescriptionResolver(ModelKeys.COUNTERS), resolvePathHandler,
                new CounterAddHandler(), new CounterRemoveHandler(), runtimeRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }

        if (runtimeRegistration) {
            CounterMetricsHandler.INSTANCE.registerMetrics(resourceRegistration);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        //TODO register reset op here
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(CounterResource.COUNTER_RESET, CounterResetCommand.INSTANCE);
        resourceRegistration.registerOperationHandler(CounterResource.COUNTER_INCREASE,
                CounterIncreaseCommand.INSTANCE);
        resourceRegistration.registerOperationHandler(CounterResource.COUNTER_DECREASE,
                CounterDecreaseCommand.INSTANCE);
    }

    private static SimpleOperationDefinitionBuilder buildOperation(String name) {
        return new SimpleOperationDefinitionBuilder(name, new InfinispanResourceDescriptionResolver(ModelKeys.COUNTERS))
                .setRuntimeOnly();
    }

    private static PathElement counterElement(OperationContext context, ModelNode operation)
            throws OperationFailedException {
        final PathAddress address = pathAddress(operation.require(OP_ADDR));
        final PathElement counterElement = address.getElement(address.size() - 1);
        return counterElement;
    }

    private static String counterName(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathElement counterElement = counterElement(context, operation);
        return counterElement.getValue();
    }

    private static String counterType(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathElement counterElement = counterElement(context, operation);
        return counterElement.getKey();
    }

    private static OperationFailedException counterManagerNotFound() {
        return new OperationFailedException("CounterManager not found in server.");
    }

    public static class CounterRemoveCommand extends BaseCounterManagerCommand {
        public static final CounterRemoveCommand INSTANCE = new CounterRemoveCommand();

        @Override
        protected ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
                throws Exception {
            final String counterName = counterName(context, operation);
            counterManager.remove(counterName);
            return new ModelNode();
        }
    }

    private static class CounterResetCommand extends BaseCounterManagerCommand {
        private static final CounterResetCommand INSTANCE = new CounterResetCommand();

        @Override
        protected ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
                throws Exception {
            final String counterName = counterName(context, operation);
            final String counterType = counterType(context, operation);
            if (counterManager.isDefined(counterName)) {
                boolean isStrongCounter = ModelKeys.STRONG_COUNTER.equals(counterType);
                if (isStrongCounter) {
                    StrongCounter strongCounter = counterManager.getStrongCounter(counterName);
                    strongCounter.reset();
                } else {
                    WeakCounter weakCounter = counterManager.getWeakCounter(counterName);
                    weakCounter.reset();
                }
            }
            return new ModelNode();
        }
    }

    private static class CounterIncreaseCommand extends BaseCounterManagerCommand {
        private static final CounterIncreaseCommand INSTANCE = new CounterIncreaseCommand();

        @Override
        protected ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
                throws Exception {
            final String counterName = counterName(context, operation);
            final String counterType = counterType(context, operation);
            if (counterManager.isDefined(counterName)) {
                boolean isStrongCounter = ModelKeys.STRONG_COUNTER.equals(counterType);
                if (isStrongCounter) {
                    StrongCounter strongCounter = counterManager.getStrongCounter(counterName);
                    strongCounter.incrementAndGet();
                } else {
                    WeakCounter weakCounter = counterManager.getWeakCounter(counterName);
                    weakCounter.increment();
                }
            }
            return new ModelNode();
        }
    }

    private static class CounterDecreaseCommand extends BaseCounterManagerCommand {
        private static final CounterDecreaseCommand INSTANCE = new CounterDecreaseCommand();

        @Override
        protected ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
                throws Exception {
            final String counterName = counterName(context, operation);
            final String counterType = counterType(context, operation);
            if (counterManager.isDefined(counterName)) {
                boolean isStrongCounter = ModelKeys.STRONG_COUNTER.equals(counterType);
                if (isStrongCounter) {
                    StrongCounter strongCounter = counterManager.getStrongCounter(counterName);
                    strongCounter.decrementAndGet();
                } else {
                    WeakCounter weakCounter = counterManager.getWeakCounter(counterName);
                    weakCounter.decrement();
                }
            }
            return new ModelNode();
        }
    }

    private static abstract class BaseCounterManagerCommand extends CacheContainerCommands {

        BaseCounterManagerCommand() {
            //path to container from counter address has two elements
            super(2);
        }

        abstract ModelNode invoke(CounterManager counterManager, OperationContext context, ModelNode operation)
                throws Exception;

        @Override
        protected final ModelNode invokeCommand(EmbeddedCacheManager cacheManager, OperationContext context,
                ModelNode operation) throws Exception {
            Optional<CounterManager> optCounterManager = SecurityActions.findCounterManager(cacheManager);
            CounterManager counterManager = optCounterManager.orElseThrow(CounterResource::counterManagerNotFound);
            return invoke(counterManager, context, operation);
        }
    }
}
