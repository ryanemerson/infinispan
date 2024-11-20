package org.infinispan.marshall.core.next.impl;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

/**
 * Interface used to initialise the {@link org.infinispan.marshall.core.GlobalMarshaller}'s {@link
 * org.infinispan.protostream.SerializationContext} using the specified Pojos, Marshaller implementations and provided
 * .proto schemas.
 *
 * @author Ryan Emerson
 * @since 11.0
 */
@AutoProtoSchemaBuilder(
      dependsOn = {
            org.infinispan.commons.marshall.GlobalContextInitializer.class,
            org.infinispan.commons.marshall.PersistenceContextInitializer.class,
            org.infinispan.marshall.persistence.impl.PersistenceContextInitializer.class
      },
      includeClasses = {
            org.infinispan.cache.impl.BiFunctionMapper.class,
            org.infinispan.cache.impl.EncoderEntryMapper.class,
            org.infinispan.cache.impl.EncoderKeyMapper.class,
            org.infinispan.cache.impl.EncoderValueMapper.class,
            org.infinispan.cache.impl.FunctionMapper.class,
            org.infinispan.commands.CommandInvocationId.class,
            org.infinispan.commands.control.LockControlCommand.class,
            org.infinispan.commands.irac.IracCleanupKeysCommand.class,
            org.infinispan.commands.irac.IracMetadataRequestCommand.class,
            org.infinispan.commands.irac.IracRequestStateCommand.class,
            org.infinispan.commands.irac.IracStateResponseCommand.class,
            org.infinispan.commands.irac.IracStateResponseCommand.State.class,
            org.infinispan.commands.irac.IracTombstoneCleanupCommand.class,
            org.infinispan.commands.irac.IracTombstonePrimaryCheckCommand.class,
            org.infinispan.commands.irac.IracTombstoneRemoteSiteCheckCommand.class,
            org.infinispan.commands.irac.IracTombstoneStateResponseCommand.class,
            org.infinispan.commands.irac.IracUpdateVersionCommand.class,
            org.infinispan.commands.functional.Mutations.ReadWrite.class,
            org.infinispan.commands.functional.Mutations.ReadWriteWithValue.class,
            org.infinispan.commands.functional.Mutations.Write.class,
            org.infinispan.commands.functional.Mutations.WriteWithValue.class,
            org.infinispan.commands.functional.ReadOnlyKeyCommand.class,
            org.infinispan.commands.functional.ReadOnlyManyCommand.class,
            org.infinispan.commands.functional.ReadWriteKeyCommand.class,
            org.infinispan.commands.functional.ReadWriteKeyValueCommand.class,
            org.infinispan.commands.functional.ReadWriteManyCommand.class,
            org.infinispan.commands.functional.ReadWriteManyEntriesCommand.class,
            org.infinispan.commands.functional.TxReadOnlyKeyCommand.class,
            org.infinispan.commands.functional.TxReadOnlyManyCommand.class,
            org.infinispan.commands.functional.WriteOnlyKeyCommand.class,
            org.infinispan.commands.functional.WriteOnlyKeyValueCommand.class,
            org.infinispan.commands.functional.WriteOnlyManyCommand.class,
            org.infinispan.commands.functional.WriteOnlyManyEntriesCommand.class,
            org.infinispan.commands.functional.functions.MergeFunction.class,
            org.infinispan.commands.read.GetKeyValueCommand.class,
            org.infinispan.commands.read.SizeCommand.class,
            org.infinispan.commands.remote.CheckTransactionRpcCommand.class,
            org.infinispan.commands.remote.ClusteredGetAllCommand.class,
            org.infinispan.commands.remote.ClusteredGetCommand.class,
            org.infinispan.commands.remote.SingleRpcCommand.class,
            org.infinispan.commands.remote.recovery.CompleteTransactionCommand.class,
            org.infinispan.commands.remote.recovery.GetInDoubtTransactionsCommand.class,
            org.infinispan.commands.remote.recovery.GetInDoubtTxInfoCommand.class,
            org.infinispan.commands.remote.recovery.TxCompletionNotificationCommand.class,
            org.infinispan.commands.statetransfer.ConflictResolutionStartCommand.class,
            org.infinispan.commands.statetransfer.StateResponseCommand.class,
            org.infinispan.commands.statetransfer.StateTransferCancelCommand.class,
            org.infinispan.commands.statetransfer.StateTransferGetListenersCommand.class,
            org.infinispan.commands.statetransfer.StateTransferGetTransactionsCommand.class,
            org.infinispan.commands.statetransfer.StateTransferStartCommand.class,
            org.infinispan.commands.topology.CacheAvailabilityUpdateCommand.class,
            org.infinispan.commands.topology.CacheJoinCommand.class,
            org.infinispan.commands.topology.CacheLeaveCommand.class,
            org.infinispan.commands.topology.CacheShutdownCommand.class,
            org.infinispan.commands.topology.CacheShutdownRequestCommand.class,
            org.infinispan.commands.topology.CacheStatusRequestCommand.class,
            org.infinispan.commands.topology.RebalancePhaseConfirmCommand.class,
            org.infinispan.commands.topology.RebalancePolicyUpdateCommand.class,
            org.infinispan.commands.topology.RebalanceStartCommand.class,
            org.infinispan.commands.topology.RebalanceStatusRequestCommand.class,
            org.infinispan.commands.topology.TopologyUpdateCommand.class,
            org.infinispan.commands.topology.TopologyUpdateStableCommand.class,
            org.infinispan.commands.triangle.BackupNoopCommand.class,
            org.infinispan.commands.triangle.MultiEntriesFunctionalBackupWriteCommand.class,
            org.infinispan.commands.triangle.MultiKeyFunctionalBackupWriteCommand.class,
            org.infinispan.commands.triangle.PutMapBackupWriteCommand.class,
            org.infinispan.commands.triangle.SingleKeyBackupWriteCommand.class,
            org.infinispan.commands.triangle.SingleKeyBackupWriteCommand.Operation.class,
            org.infinispan.commands.triangle.SingleKeyFunctionalBackupWriteCommand.class,
            org.infinispan.commands.triangle.SingleKeyFunctionalBackupWriteCommand.Operation.class,
            org.infinispan.commands.tx.CommitCommand.class,
            org.infinispan.commands.tx.PrepareCommand.class,
            org.infinispan.commands.tx.RollbackCommand.class,
            org.infinispan.commands.tx.VersionedCommitCommand.class,
            org.infinispan.commands.tx.VersionedPrepareCommand.class,
            org.infinispan.commands.write.BackupMultiKeyAckCommand.class,
            org.infinispan.commands.write.ClearCommand.class,
            org.infinispan.commands.write.ComputeCommand.class,
            org.infinispan.commands.write.ComputeIfAbsentCommand.class,
            org.infinispan.commands.write.ExceptionAckCommand.class,
            org.infinispan.commands.write.InvalidateCommand.class,
            org.infinispan.commands.write.InvalidateL1Command.class,
            org.infinispan.commands.write.IracPutKeyValueCommand.class,
            org.infinispan.commands.write.PutKeyValueCommand.class,
            org.infinispan.commands.write.PutMapCommand.class,
            org.infinispan.commands.write.RemoveCommand.class,
            org.infinispan.commands.write.RemoveExpiredCommand.class,
            org.infinispan.commands.write.ReplaceCommand.class,
            org.infinispan.commands.write.ValueMatcher.class,
            org.infinispan.configuration.cache.CacheMode.class,
            org.infinispan.configuration.cache.XSiteStateTransferMode.class,
            org.infinispan.container.entries.ImmortalCacheEntry.class,
            org.infinispan.container.entries.ImmortalCacheValue.class,
            org.infinispan.container.entries.MortalCacheEntry.class,
            org.infinispan.container.entries.MortalCacheValue.class,
            org.infinispan.container.entries.TransientCacheEntry.class,
            org.infinispan.container.entries.TransientCacheValue.class,
            org.infinispan.container.entries.TransientMortalCacheEntry.class,
            org.infinispan.container.entries.TransientMortalCacheValue.class,
            org.infinispan.container.entries.metadata.MetadataImmortalCacheEntry.class,
            org.infinispan.container.entries.metadata.MetadataImmortalCacheValue.class,
            org.infinispan.container.entries.metadata.MetadataMortalCacheValue.class,
            org.infinispan.container.entries.metadata.MetadataTransientCacheEntry.class,
            org.infinispan.container.entries.metadata.MetadataTransientCacheValue.class,
            org.infinispan.container.entries.metadata.MetadataTransientMortalCacheEntry.class,
            org.infinispan.container.entries.metadata.MetadataTransientMortalCacheValue.class,
            org.infinispan.container.versioning.irac.IracTombstoneInfo.class,
            org.infinispan.context.Flag.class,
            org.infinispan.distribution.ch.impl.AbstractConsistentHash.SegmentOwnership.class,
            org.infinispan.distribution.ch.impl.DefaultConsistentHash.class,
            org.infinispan.distribution.ch.impl.DefaultConsistentHashFactory.class,
            org.infinispan.distribution.ch.impl.ReplicatedConsistentHash.class,
            org.infinispan.distribution.ch.impl.ReplicatedConsistentHashFactory.class,
            org.infinispan.distribution.ch.impl.SyncConsistentHashFactory.class,
            org.infinispan.distribution.ch.impl.SyncReplicatedConsistentHashFactory.class,
            org.infinispan.distribution.ch.impl.TopologyAwareConsistentHashFactory.class,
            org.infinispan.distribution.ch.impl.TopologyAwareSyncConsistentHashFactory.class,
            org.infinispan.distribution.group.impl.CacheEntryGroupPredicate.class,
            org.infinispan.encoding.DataConversion.class,
            org.infinispan.expiration.impl.TouchCommand.class,
            org.infinispan.filter.AcceptAllKeyValueFilter.class,
            org.infinispan.filter.CompositeKeyValueFilter.class,
            org.infinispan.filter.CacheFilters.ConverterAsCacheEntryFunction.class,
            org.infinispan.filter.CacheFilters.FilterConverterAsCacheEntryFunction.class,
            org.infinispan.filter.CacheFilters.FilterConverterAsValueFunction.class,
            org.infinispan.filter.CacheFilters.KeyValueFilterAsPredicate.class,
            org.infinispan.filter.CacheFilters.NotNullCacheEntryPredicate.class,
            org.infinispan.functional.MetaParam.MetaEntryVersion.class,
            org.infinispan.functional.MetaParam.MetaLifespan.class,
            org.infinispan.functional.MetaParam.MetaMaxIdle.class,
            org.infinispan.functional.impl.EntryViews.NoValueReadOnlyView.class,
            org.infinispan.functional.impl.EntryViews.ReadOnlySnapshotView.class,
            org.infinispan.functional.impl.EntryViews.ReadWriteSnapshotView.class,
            org.infinispan.functional.impl.Params.class,
            org.infinispan.functional.impl.StatsEnvelope.class,
            org.infinispan.globalstate.ScopeFilter.class,
            org.infinispan.globalstate.ScopedState.class,
            org.infinispan.globalstate.impl.CacheState.class,
            org.infinispan.interceptors.distribution.VersionedResult.class,
            org.infinispan.interceptors.distribution.VersionedResults.class,
            org.infinispan.manager.impl.ReplicableManagerFunctionCommand.class,
            org.infinispan.manager.impl.ReplicableRunnableCommand.class,
            org.infinispan.marshall.core.MarshallableFunctions.Identity.class,
            org.infinispan.marshall.core.MarshallableFunctions.Remove.class,
            org.infinispan.marshall.core.MarshallableFunctions.RemoveIfValueEqualsReturnBoolean.class,
            org.infinispan.marshall.core.MarshallableFunctions.RemoveReturnBoolean.class,
            org.infinispan.marshall.core.MarshallableFunctions.RemoveReturnPrevOrNull.class,
            org.infinispan.marshall.core.MarshallableFunctions.ReturnReadOnlyFindIsPresent.class,
            org.infinispan.marshall.core.MarshallableFunctions.ReturnReadOnlyFindOrNull.class,
            org.infinispan.marshall.core.MarshallableFunctions.ReturnReadWriteFind.class,
            org.infinispan.marshall.core.MarshallableFunctions.ReturnReadWriteGet.class,
            org.infinispan.marshall.core.MarshallableFunctions.ReturnReadWriteView.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValue.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueMetas.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetInternalCacheValue.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueIfAbsentReturnBoolean.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueIfAbsentReturnPrevOrNull.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueIfEqualsReturnBoolean.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueIfPresentReturnBoolean.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueIfPresentReturnPrevOrNull.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueIfPresentReturnBoolean.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueMetasIfAbsentReturnBoolean.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueMetasIfAbsentReturnPrevOrNull.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueMetasIfPresentReturnBoolean.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueMetasIfPresentReturnPrevOrNull.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueMetasReturnPrevOrNull.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueMetasReturnView.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueReturnPrevOrNull.class,
            org.infinispan.marshall.core.MarshallableFunctions.SetValueReturnView.class,
            org.infinispan.marshall.protostream.impl.adapters.ClassAdapter.class,
            org.infinispan.marshall.protostream.impl.adapters.OptionalAdapter.class,
            org.infinispan.marshall.protostream.impl.MarshallableArray.class,
            org.infinispan.marshall.protostream.impl.MarshallableCollection.class,
            org.infinispan.marshall.protostream.impl.MarshallableLambda.class,
            org.infinispan.marshall.protostream.impl.MarshallableMap.class,
            org.infinispan.marshall.protostream.impl.MarshallableObject.class,
            org.infinispan.marshall.protostream.impl.MarshallableThrowable.class,
            org.infinispan.metadata.impl.InternalMetadataImpl.class,
            org.infinispan.notifications.cachelistener.cluster.ClusterEvent.class,
            org.infinispan.notifications.cachelistener.cluster.ClusterListenerRemoveCallable.class,
            org.infinispan.notifications.cachelistener.cluster.ClusterListenerReplicateCallable.class,
            org.infinispan.notifications.cachelistener.cluster.MultiClusterEventCommand.class,
            org.infinispan.notifications.cachelistener.event.Event.Type.class,
            org.infinispan.notifications.cachelistener.filter.CacheEventConverterAsConverter.class,
            org.infinispan.notifications.cachelistener.filter.CacheEventFilterAsKeyValueFilter.class,
            org.infinispan.notifications.cachelistener.filter.CacheEventFilterConverterAsKeyValueFilterConverter.class,
            org.infinispan.notifications.cachelistener.filter.KeyValueFilterAsCacheEventFilter.class,
            org.infinispan.notifications.cachelistener.filter.KeyValueFilterConverterAsCacheEventFilterConverter.class,
            org.infinispan.partitionhandling.AvailabilityMode.class,
            org.infinispan.reactive.publisher.PublisherReducers.AllMatchReducer.class,
            org.infinispan.reactive.publisher.PublisherReducers.AndFinalizer.class,
            org.infinispan.reactive.publisher.PublisherReducers.AnyMatchReducer.class,
            org.infinispan.reactive.publisher.PublisherReducers.CollectorFinalizer.class,
            org.infinispan.reactive.publisher.PublisherReducers.CollectorReducer.class,
            org.infinispan.reactive.publisher.PublisherReducers.CollectReducer.class,
            org.infinispan.reactive.publisher.PublisherReducers.CombinerFinalizer.class,
            org.infinispan.reactive.publisher.PublisherReducers.FindFirstReducerFinalizer.class,
            org.infinispan.reactive.publisher.PublisherReducers.MaxReducerFinalizer.class,
            org.infinispan.reactive.publisher.PublisherReducers.MinReducerFinalizer.class,
            org.infinispan.reactive.publisher.PublisherReducers.NoneMatchReducer.class,
            org.infinispan.reactive.publisher.PublisherReducers.OrFinalizer.class,
            org.infinispan.reactive.publisher.PublisherReducers.ReduceReducerFinalizer.class,
            org.infinispan.reactive.publisher.PublisherReducers.ReduceWithIdentityReducer.class,
            org.infinispan.reactive.publisher.PublisherReducers.ReduceWithInitialSupplierReducer.class,
            org.infinispan.reactive.publisher.PublisherReducers.SumFinalizer.class,
            org.infinispan.reactive.publisher.PublisherReducers.SumReducer.class,
            org.infinispan.reactive.publisher.PublisherReducers.ToArrayFinalizer.class,
            org.infinispan.reactive.publisher.PublisherReducers.ToArrayReducer.class,
            org.infinispan.reactive.publisher.PublisherTransformers.IdentityTransformer.class,
            org.infinispan.reactive.publisher.impl.DeliveryGuarantee.class,
            org.infinispan.reactive.publisher.impl.commands.batch.CancelPublisherCommand.class,
            org.infinispan.reactive.publisher.impl.commands.batch.InitialPublisherCommand.class,
            org.infinispan.reactive.publisher.impl.commands.batch.KeyPublisherResponse.class,
            org.infinispan.reactive.publisher.impl.commands.batch.NextPublisherCommand.class,
            org.infinispan.reactive.publisher.impl.commands.batch.PublisherResponse.class,
            org.infinispan.reactive.publisher.impl.commands.reduction.ReductionPublisherRequestCommand.class,
            org.infinispan.reactive.publisher.impl.commands.reduction.SegmentPublisherResult.class,
            org.infinispan.reactive.publisher.impl.PublisherHandler.SegmentResult.class,
            org.infinispan.remoting.responses.BiasRevocationResponse.class,
            org.infinispan.remoting.responses.CacheNotFoundResponse.class,
            org.infinispan.remoting.responses.ExceptionResponse.class,
            org.infinispan.remoting.responses.PrepareResponse.class,
            org.infinispan.remoting.responses.SuccessfulResponse.class,
            org.infinispan.remoting.responses.UnsuccessfulResponse.class,
            org.infinispan.remoting.responses.UnsureResponse.class,
            org.infinispan.remoting.transport.jgroups.JGroupsTopologyAwareAddress.class,
            org.infinispan.statetransfer.StateChunk.class,
            org.infinispan.statetransfer.TransactionInfo.class,
            org.infinispan.stats.impl.ClusterCacheStatsImpl.DistributedCacheStatsCallable.class,
            org.infinispan.stream.CacheCollectors.CollectorSupplier.class,
            org.infinispan.stream.StreamMarshalling.AlwaysTruePredicate.class,
            org.infinispan.stream.StreamMarshalling.EntryToKeyFunction.class,
            org.infinispan.stream.StreamMarshalling.EntryToValueFunction.class,
            org.infinispan.stream.StreamMarshalling.EqualityPredicate.class,
            org.infinispan.stream.StreamMarshalling.IdentityFunction.class,
            org.infinispan.stream.StreamMarshalling.EntryToKeyFunction.class,
            org.infinispan.stream.StreamMarshalling.NonNullPredicate.class,
            org.infinispan.stream.impl.CacheBiConsumers.CacheObjBiConsumer.class,
            org.infinispan.stream.impl.CacheBiConsumers.CacheDoubleConsumer.class,
            org.infinispan.stream.impl.CacheBiConsumers.CacheIntConsumer.class,
            org.infinispan.stream.impl.CacheBiConsumers.CacheLongConsumer.class,
            org.infinispan.stream.impl.CacheIntermediatePublisher.class,
            org.infinispan.stream.impl.CacheStreamIntermediateReducer.class,
            org.infinispan.stream.impl.LockedStreamImpl.CacheEntryFunction.class,
            org.infinispan.stream.impl.LockedStreamImpl.CacheEntryConsumer.class,
            org.infinispan.stream.impl.intops.object.DistinctOperation.class,
            org.infinispan.stream.impl.intops.object.FilterOperation.class,
            org.infinispan.stream.impl.intops.object.FlatMapOperation.class,
            org.infinispan.stream.impl.intops.object.FlatMapToDoubleOperation.class,
            org.infinispan.stream.impl.intops.object.FlatMapToIntOperation.class,
            org.infinispan.stream.impl.intops.object.FlatMapToLongOperation.class,
            org.infinispan.stream.impl.intops.object.LimitOperation.class,
            org.infinispan.stream.impl.intops.object.MapOperation.class,
            org.infinispan.stream.impl.intops.object.MapToDoubleOperation.class,
            org.infinispan.stream.impl.intops.object.MapToIntOperation.class,
            org.infinispan.stream.impl.intops.object.MapToLongOperation.class,
            org.infinispan.stream.impl.intops.object.PeekOperation.class,
            org.infinispan.stream.impl.intops.object.SortedComparatorOperation.class,
            org.infinispan.stream.impl.intops.object.SortedOperation.class,
            org.infinispan.stream.impl.intops.primitive.d.BoxedDoubleOperation.class,
            org.infinispan.stream.impl.intops.primitive.d.DistinctDoubleOperation.class,
            org.infinispan.stream.impl.intops.primitive.d.FilterDoubleOperation.class,
            org.infinispan.stream.impl.intops.primitive.d.FlatMapDoubleOperation.class,
            org.infinispan.stream.impl.intops.primitive.d.LimitDoubleOperation.class,
            org.infinispan.stream.impl.intops.primitive.d.MapDoubleOperation.class,
            org.infinispan.stream.impl.intops.primitive.d.MapToIntDoubleOperation.class,
            org.infinispan.stream.impl.intops.primitive.d.MapToLongDoubleOperation.class,
            org.infinispan.stream.impl.intops.primitive.d.MapToObjDoubleOperation.class,
            org.infinispan.stream.impl.intops.primitive.d.PeekDoubleOperation.class,
            org.infinispan.stream.impl.intops.primitive.d.SortedDoubleOperation.class,
            org.infinispan.stream.impl.intops.primitive.i.BoxedIntOperation.class,
            org.infinispan.stream.impl.intops.primitive.i.DistinctIntOperation.class,
            org.infinispan.stream.impl.intops.primitive.i.FilterIntOperation.class,
            org.infinispan.stream.impl.intops.primitive.i.FlatMapIntOperation.class,
            org.infinispan.stream.impl.intops.primitive.i.LimitIntOperation.class,
            org.infinispan.stream.impl.intops.primitive.i.MapIntOperation.class,
            org.infinispan.stream.impl.intops.primitive.i.MapToDoubleIntOperation.class,
            org.infinispan.stream.impl.intops.primitive.i.MapToLongIntOperation.class,
            org.infinispan.stream.impl.intops.primitive.i.MapToObjIntOperation.class,
            org.infinispan.stream.impl.intops.primitive.i.PeekIntOperation.class,
            org.infinispan.stream.impl.intops.primitive.i.SortedIntOperation.class,
            org.infinispan.stream.impl.intops.primitive.l.BoxedLongOperation.class,
            org.infinispan.stream.impl.intops.primitive.l.DistinctLongOperation.class,
            org.infinispan.stream.impl.intops.primitive.l.FilterLongOperation.class,
            org.infinispan.stream.impl.intops.primitive.l.FlatMapLongOperation.class,
            org.infinispan.stream.impl.intops.primitive.l.LimitLongOperation.class,
            org.infinispan.stream.impl.intops.primitive.l.MapLongOperation.class,
            org.infinispan.stream.impl.intops.primitive.l.MapToObjLongOperation.class,
            org.infinispan.stream.impl.intops.primitive.l.PeekLongOperation.class,
            org.infinispan.stream.impl.intops.primitive.l.SortedLongOperation.class,
            org.infinispan.topology.CacheJoinInfo.class,
            org.infinispan.topology.CacheStatusResponse.class,
            org.infinispan.topology.CacheTopology.class,
            org.infinispan.topology.CacheTopology.Phase.class,
            org.infinispan.topology.HeartBeatCommand.class,
            org.infinispan.topology.ManagerStatusResponse.class,
            org.infinispan.topology.PersistentUUID.class,
            org.infinispan.topology.RebalancingStatus.class,
            org.infinispan.transaction.xa.GlobalTransaction.class,
            org.infinispan.transaction.xa.recovery.InDoubtTxInfo.class,
            org.infinispan.util.KeyValuePair.class,
            org.infinispan.xsite.events.XSiteEvent.class,
            org.infinispan.xsite.events.XSiteEventType.class,
            org.infinispan.xsite.SingleXSiteRpcCommand.class,
            org.infinispan.xsite.commands.remote.IracClearKeysRequest.class,
            org.infinispan.xsite.commands.remote.IracPutManyRequest.class,
            org.infinispan.xsite.commands.remote.IracPutManyRequest.Expire.class,
            org.infinispan.xsite.commands.remote.IracPutManyRequest.Remove.class,
            org.infinispan.xsite.commands.remote.IracPutManyRequest.Write.class,
            org.infinispan.xsite.commands.remote.IracTombstoneCheckRequest.class,
            org.infinispan.xsite.commands.remote.IracTouchKeyRequest.class,
            org.infinispan.xsite.commands.remote.XSiteRemoteEventCommand.class,
            org.infinispan.xsite.commands.remote.XSiteStatePushRequest.class,
            org.infinispan.xsite.commands.remote.XSiteStateTransferControlRequest.class,
            org.infinispan.xsite.commands.XSiteAutoTransferStatusCommand.class,
            org.infinispan.xsite.commands.XSiteAmendOfflineStatusCommand.class,
            org.infinispan.xsite.commands.XSiteBringOnlineCommand.class,
            org.infinispan.xsite.commands.XSiteOfflineStatusCommand.class,
            org.infinispan.xsite.commands.XSiteSetStateTransferModeCommand.class,
            org.infinispan.xsite.commands.XSiteStateTransferCancelSendCommand.class,
            org.infinispan.xsite.commands.XSiteStateTransferClearStatusCommand.class,
            org.infinispan.xsite.commands.XSiteStateTransferFinishReceiveCommand.class,
            org.infinispan.xsite.commands.XSiteStateTransferFinishSendCommand.class,
            org.infinispan.xsite.commands.XSiteStateTransferRestartSendingCommand.class,
            org.infinispan.xsite.commands.XSiteStateTransferStartReceiveCommand.class,
            org.infinispan.xsite.commands.XSiteStateTransferStartSendCommand.class,
            org.infinispan.xsite.commands.XSiteStateTransferStatusRequestCommand.class,
            org.infinispan.xsite.commands.XSiteStatusCommand.class,
            org.infinispan.xsite.commands.XSiteTakeOfflineCommand.class,
            org.infinispan.xsite.irac.IracManagerKeyInfo.class,
            org.infinispan.xsite.response.AutoStateTransferResponse.class,
            org.infinispan.xsite.statetransfer.StateTransferStatus.class,
            org.infinispan.xsite.statetransfer.XSiteState.class,
            org.infinispan.xsite.statetransfer.XSiteState.class,
            org.infinispan.xsite.statetransfer.XSiteStatePushCommand.class,
            org.infinispan.xsite.status.SiteState.class,
            org.infinispan.xsite.status.BringSiteOnlineResponse.class,
            org.infinispan.xsite.status.TakeSiteOfflineResponse.class,
      },
      schemaFileName = "global.core.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = GlobalContextInitializer.PACKAGE_NAME)
public interface GlobalContextInitializer extends SerializationContextInitializer {
   String PACKAGE_NAME = "org.infinispan.global.core";

   SerializationContextInitializer INSTANCE = new org.infinispan.marshall.core.next.impl.GlobalContextInitializerImpl();

   static String getFqTypeName(Class clazz) {
      return PACKAGE_NAME + "." + clazz.getSimpleName();
   }
}
