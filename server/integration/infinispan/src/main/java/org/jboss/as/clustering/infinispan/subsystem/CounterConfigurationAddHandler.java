package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.infinispan.counter.EmbeddedCounterManagerFactory;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterConfiguration.Builder;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.Storage;
import org.infinispan.server.infinispan.spi.service.CacheContainerServiceName;
import org.jboss.as.clustering.infinispan.DefaultCacheContainer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Add operation handler for /subsystem=infinispan/cache-container=clustered/counter=*
 *
 * @author Vladimir Blagojevic
 *
 */
public class CounterConfigurationAddHandler extends AbstractAddStepHandler {

    CounterConfigurationAddHandler() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        super.performRuntime(context, operation, model);
        String counterName = getCounterName(operation);
        String containerName = getContainerName(operation);
        String counterType = getCounterType(operation);

        Builder b = getBuilder(context, model, counterType);
        processModelNode(context, containerName, model, b);

        // define configuration
        ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        ServiceName serviceName = CacheContainerServiceName.CACHE_CONTAINER.getServiceName(containerName);
        ServiceController<?> controller = serviceRegistry.getService(serviceName);
        DefaultCacheContainer cacheManager = (DefaultCacheContainer) controller.getValue();
        if (cacheManager != null) {
            CounterManager counterManager = EmbeddedCounterManagerFactory.asCounterManager(cacheManager);
            CounterConfiguration configuration = b.build();
            counterManager.defineCounter(counterName, configuration);
        }
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        this.populate(operation, model);
    }

    void populate(ModelNode fromModel, ModelNode toModel) throws OperationFailedException {
        for (AttributeDefinition attr : CounterConfigurationResource.ATTRIBUTES) {
            attr.validateAndSet(fromModel, toModel);
        }
    }

    private String getContainerName(ModelNode operation) {
        PathAddress counterAddress = getCounterAddressFromOperation(operation);
        PathAddress pathAddress = counterAddress.subAddress(0, counterAddress.size() - 3);
        return pathAddress.getLastElement().getValue();
    }

    private String getCounterType(ModelNode operation) {
        PathAddress counterAddress = getCounterAddressFromOperation(operation);
        int size = counterAddress.size();
        PathAddress subAddress = counterAddress.subAddress(size - 1, size);
        return subAddress.getLastElement().getKey();
    }

    private String getCounterName(ModelNode operation) {
        PathAddress counterAddress = getCounterAddressFromOperation(operation);
        int size = counterAddress.size();
        PathAddress subAddress = counterAddress.subAddress(size - 1, size);
        return subAddress.getLastElement().getValue();
    }

    private PathAddress getCounterAddressFromOperation(ModelNode operation) {
        return PathAddress.pathAddress(operation.get(OP_ADDR));
    }

    private Builder getBuilder(OperationContext context, ModelNode counter, String counterType)
            throws OperationFailedException {
        boolean isWeakCounter = ModelKeys.WEAK_COUNTER.equals(counterType);
        if (isWeakCounter) {
            return CounterConfiguration.builder(CounterType.WEAK);
        } else {
            ModelNode lowerBoundModel = counter.get(ModelKeys.LOWER_BOUND);
            ModelNode upperBoundModel = counter.get(ModelKeys.UPPER_BOUND);
            boolean isBounded = lowerBoundModel.isDefined() || upperBoundModel.isDefined();
            return isBounded ? CounterConfiguration.builder(CounterType.BOUNDED_STRONG)
                    : CounterConfiguration.builder(CounterType.UNBOUNDED_STRONG);
        }
    }

    void processModelNode(OperationContext context, String containerName, ModelNode counter,
            CounterConfiguration.Builder builder) throws OperationFailedException {

        long initialValue = CounterConfigurationResource.INITIAL_VALUE.resolveModelAttribute(context, counter).asLong();
        String storageType = CounterConfigurationResource.STORAGE.resolveModelAttribute(context, counter).asString();

        builder.initialValue(initialValue);
        builder.storage(Storage.valueOf(storageType));
    }
}
