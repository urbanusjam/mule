/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.model;

import static org.mule.runtime.core.config.model.ParameterDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.core.config.model.ParameterDefinition.Builder.fromChildListConfiguration;
import static org.mule.runtime.core.config.model.ParameterDefinition.Builder.fromNoConfiguration;
import static org.mule.runtime.core.config.model.ParameterDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.core.config.model.ParameterDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.core.config.model.ParameterDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.core.config.model.TypeDefinitionBuilder.Builder.fromConfigurationAttribute;
import static org.mule.runtime.core.config.model.TypeDefinitionBuilder.Builder.fromType;
import org.mule.runtime.config.spring.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.ChoiceRouterFactoryBean;
import org.mule.runtime.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.MessageProcessorFilterPairFactoryBean;
import org.mule.runtime.config.spring.factories.PollingMessageSourceFactoryBean;
import org.mule.runtime.config.spring.factories.ResponseMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.ScatterGatherRouterFactoryBean;
import org.mule.runtime.config.spring.factories.SubflowMessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.TransactionalMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.WatermarkFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.schedule.SchedulerFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.config.model.ComponentBuildingDefinition;
import org.mule.runtime.core.config.model.ComponentBuildingDefinitionProvider;
import org.mule.runtime.core.config.model.MessageEnricherObjectFactory;
import org.mule.runtime.core.config.model.ParameterDefinition;
import org.mule.runtime.core.config.model.ReferenceMessageProcessor;
import org.mule.runtime.core.config.model.TransactionalMessageProcessor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.enricher.MessageEnricher;
import org.mule.runtime.core.exception.CatchMessagingExceptionStrategy;
import org.mule.runtime.core.exception.ChoiceMessagingExceptionStrategy;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.exception.MessagingExceptionStrategyRef;
import org.mule.runtime.core.exception.RedeliveryExceeded;
import org.mule.runtime.core.exception.RollbackMessagingExceptionStrategy;
import org.mule.runtime.core.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.routing.AggregationStrategy;
import org.mule.runtime.core.routing.ChoiceRouter;
import org.mule.runtime.core.routing.FirstSuccessful;
import org.mule.runtime.core.routing.Foreach;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.routing.MessageProcessorFilterPair;
import org.mule.runtime.core.routing.RoundRobin;
import org.mule.runtime.core.routing.ScatterGatherRouter;
import org.mule.runtime.core.routing.UntilSuccessful;
import org.mule.runtime.core.routing.WireTap;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.routing.outbound.MulticastingRouter;
import org.mule.runtime.core.source.polling.MessageProcessorPollingOverride;
import org.mule.runtime.core.source.polling.PollingMessageSource;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencySchedulerFactory;
import org.mule.runtime.core.source.polling.watermark.Watermark;
import org.mule.runtime.core.transaction.lookup.GenericTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.JBossTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.JRunTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.Resin3TransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.WeblogicTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.WebsphereTransactionManagerLookupFactory;
import org.mule.runtime.core.transformer.simple.SetPayloadMessageProcessor;

import java.util.LinkedList;
import java.util.List;

public class CoreComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider
{

    private ComponentBuildingDefinition.Builder baseDefinition;
    private ComponentBuildingDefinition.Builder transformerBaseDefinition;
    private ComponentBuildingDefinition.Builder transactionManagerBaseDefinition;

    @Override
    public void init(MuleContext muleContext)
    {
        baseDefinition = new ComponentBuildingDefinition.Builder().withNamespace("mule");
        transformerBaseDefinition = baseDefinition.copy()
                .withSetterParameterDefinition("ignoreBadInput", fromSimpleParameter("ignoreBadInput").build())
                .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
                .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build());

        transactionManagerBaseDefinition = baseDefinition.copy();

    }

    @Override
    public List<ComponentBuildingDefinition> getComponentBuildingDefinitions()
    {

        LinkedList<ComponentBuildingDefinition> componentBuildingDefinitions = new LinkedList<>();

        //Common elements
        //componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
        //                                         .withNamespace("mule")
        //                                         .withName("configuration")
        //                                         .withTypeDefinitionBuilder(fromType(DefaultMuleConfiguration.class))
        //                                         .withSetterParameterDefinition("defaultExceptionStrategyName", fromSimpleParameter("defaultExceptionStrategy-ref").build())
        //                                         .withSetterParameterDefinition("defaultObjectSerializer", fromSimpleReferenceParameter("defaultObjectSerializer").build())
        //                                         .build());

        ParameterDefinition messageProcessorListParameterDefinition = fromChildListConfiguration(MessageProcessor.class).build();
        ComponentBuildingDefinition.Builder exceptionStrategyBaseBuilder = new ComponentBuildingDefinition.Builder()
                .withSetterParameterDefinition("messageProcessors", messageProcessorListParameterDefinition)
                .withSetterParameterDefinition("globalName", fromSimpleParameter("name").build())
                .withNamespace("mule");
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("exception-strategy")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(MessagingExceptionStrategyRef.class))
                                                 .withConstructorParameterDefinition(fromSimpleReferenceParameter("ref").build())
                                                 .build());
        componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy()
                                                 .withName("catch-exception-strategy")
                                                 .withTypeDefinitionBuilder(fromType(CatchMessagingExceptionStrategy.class))
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("when", fromSimpleParameter("when").build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy()
                                                 .withName("rollback-exception-strategy")
                                                 .withTypeDefinitionBuilder(fromType(RollbackMessagingExceptionStrategy.class))
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("when", fromSimpleParameter("when").build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy()
                                                 .withName("default-exception-strategy")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(DefaultMessagingExceptionStrategy.class))
                                                 .withSetterParameterDefinition("globalName", fromSimpleParameter("name").build())
                                                 .withSetterParameterDefinition("stopMessageProcessing", fromSimpleParameter("stopMessageProcessing").build())
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("commitTxFilter", fromChildConfiguration(WildcardFilter.class).build())
                                                 .withSetterParameterDefinition("rollbackTxFilter", fromChildConfiguration(WildcardFilter.class).build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("custom-exception-strategy")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromConfigurationAttribute("class"))
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withNamespace("mule")
                                                 .withName("on-redelivery-attempts-exceeded")
                                                 .withTypeDefinitionBuilder(fromType(RedeliveryExceeded.class))
                                                 .withSetterParameterDefinition("messageProcessors", messageProcessorListParameterDefinition)
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withNamespace("mule")
                                                 .withName("choice-exception-strategy")
                                                 .withTypeDefinitionBuilder(fromType(ChoiceMessagingExceptionStrategy.class))
                                                 .withSetterParameterDefinition("globalName", fromSimpleParameter("name").build())
                                                 .withSetterParameterDefinition("exceptionListeners", fromChildListConfiguration(MessagingExceptionHandler.class).build())
                                                 .build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("set-payload")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(SetPayloadMessageProcessor.class))
                                                 .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
                                                 .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
                                                 .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
                                                 .build());

        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("jndi-transaction-manager", GenericTransactionManagerLookupFactory.class) //TODO add support for environment
                                                 .withSetterParameterDefinition("jndiName", fromSimpleParameter("jndiName").build())
                                                 .build());
        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("weblogic-transaction-manager", WeblogicTransactionManagerLookupFactory.class).build());
        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("jboss-transaction-manager", JBossTransactionManagerLookupFactory.class).build());
        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("jrun-transaction-manager", JRunTransactionManagerLookupFactory.class).build());
        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("resin-transaction-manager", Resin3TransactionManagerLookupFactory.class).build());
        componentBuildingDefinitions.add(createTransactionManagerDefinitionBuilder("websphere-transaction-manager", WebsphereTransactionManagerLookupFactory.class).build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("processor")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(ReferenceMessageProcessor.class))
                                                 .withSetterParameterDefinition("messageProcessor", fromSimpleReferenceParameter("ref").build())
                                                 .build());
        //TODO define what to do with this since enabling it cause a duplicate transformer to be registered. See SpringPrototypesLifecycleTestCase
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("transformer")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(ReferenceMessageProcessor.class))
                                                 .withSetterParameterDefinition("messageProcessor", fromSimpleReferenceParameter("ref").build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("filter")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(ReferenceMessageProcessor.class))
                                                 .withSetterParameterDefinition("filter", fromSimpleReferenceParameter("ref").build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("custom-processor")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromConfigurationAttribute("class"))
                                                 .build());
        //TODO see if we can remove the factory bean. Removing it brakes MessageProcessorNotificationPathTestCase.scopes
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("processor-chain")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(MessageProcessor.class))
                                                 .withObjectFactoryType(MessageProcessorChainFactoryBean.class)
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("sub-flow")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(MessageProcessor.class))
                                                 .withObjectFactoryType(SubflowMessageProcessorChainFactoryBean.class)
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
                                                 .asPrototype()
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("response")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(ResponseMessageProcessorAdapter.class))
                                                 .withConstructorParameterDefinition(fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("message-filter")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(MessageFilter.class))
                                                 .withConstructorParameterDefinition(fromChildConfiguration(Filter.class).build())
                                                 .withConstructorParameterDefinition(fromSimpleParameter("throwOnUnaccepted").withDefaultValue(false).build())
                                                 .withConstructorParameterDefinition(fromSimpleReferenceParameter("onUnaccepted").build())
                                                 .build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("flow")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(Flow.class))
                                                 .withConstructorParameterDefinition(fromSimpleParameter("name").build())
                                                 .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
                                                 .withSetterParameterDefinition("initialState", fromSimpleParameter("initialState").build())
                                                 .withSetterParameterDefinition("messageSource", fromChildConfiguration(MessageSource.class).build())
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("exceptionListener", fromChildConfiguration(MessagingExceptionHandler.class).build())
                                                 .withSetterParameterDefinition("processingStrategy", fromSimpleReferenceParameter("processingStrategy").build())
                                                 .build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("scatter-gather")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(ScatterGatherRouter.class))
                                                 .withObjectFactoryType(ScatterGatherRouterFactoryBean.class)
                                                 .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
                                                 .withSetterParameterDefinition("aggregationStrategy", fromChildConfiguration(AggregationStrategy.class).build())
                                                 .withSetterParameterDefinition("threadingProfile", fromChildConfiguration(ThreadingProfile.class).build())
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .asScope()
                                                 .build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("wire-tap")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(WireTap.class))
                                                 .withSetterParameterDefinition("tap", fromChildConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("filter", fromChildConfiguration(Filter.class).build())
                                                 .asScope()
                                                 .build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("enricher")
                                                 .withNamespace("mule")
                                                 .withObjectFactoryType(MessageEnricherObjectFactory.class)
                                                 .withTypeDefinitionBuilder(fromType(MessageEnricher.class))
                                                 .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("enrichExpressionPairs", fromChildListConfiguration(MessageEnricher.EnrichExpressionPair.class).build())
                                                 .withSetterParameterDefinition("source", fromSimpleParameter("source").build())
                                                 .withSetterParameterDefinition("target", fromSimpleParameter("target").build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("enrich")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(MessageEnricher.EnrichExpressionPair.class))
                                                 .withConstructorParameterDefinition(fromSimpleParameter("source").build())
                                                 .withConstructorParameterDefinition(fromSimpleParameter("target").build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("async")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(AsyncDelegateMessageProcessor.class))
                                                 .withObjectFactoryType(AsyncMessageProcessorsFactoryBean.class)
                                                 .withSetterParameterDefinition("processingStrategy", fromSimpleReferenceParameter("processingStrategy").build())
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("transactional")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(TransactionalMessageProcessor.class))
                                                 .withObjectFactoryType(TransactionalMessageProcessorsFactoryBean.class)
                                                 .withSetterParameterDefinition("exceptionListener", fromChildConfiguration(MessagingExceptionHandler.class).build())
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("action", fromSimpleParameter("action").build())
                                                 .build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("until-successful")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(UntilSuccessful.class))
                                                 .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("objectStore-ref").build())
                                                 .withSetterParameterDefinition("deadLetterQueue", fromSimpleReferenceParameter("deadLetterQueue-ref").build())
                                                 .withSetterParameterDefinition("maxRetries", fromSimpleParameter("maxRetries").build())
                                                 .withSetterParameterDefinition("millisBetweenRetries", fromSimpleParameter("millisBetweenRetries").build())
                                                 .withSetterParameterDefinition("secondsBetweenRetries", fromSimpleParameter("secondsBetweenRetries").build())
                                                 .withSetterParameterDefinition("failureExpression", fromSimpleParameter("failureExpression").build())
                                                 .withSetterParameterDefinition("ackExpression", fromSimpleParameter("ackExpression").build())
                                                 .withSetterParameterDefinition("synchronous", fromSimpleParameter("synchronous").build())
                                                 .withSetterParameterDefinition("threadingProfile", fromChildConfiguration(ThreadingProfile.class).build())
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("foreach")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(Foreach.class))
                                                 .withSetterParameterDefinition("collectionExpression", fromSimpleParameter("collection").build())
                                                 .withSetterParameterDefinition("batchSize", fromSimpleParameter("batchSize").build())
                                                 .withSetterParameterDefinition("rootMessageVariableName", fromSimpleParameter("rootMessageVariableName").build())
                                                 .withSetterParameterDefinition("counterVariableName", fromSimpleParameter("counterVariableName").build())
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("first-successful")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(FirstSuccessful.class))
                                                 .withSetterParameterDefinition("failureExpression", fromSimpleParameter("failureExpression").build())
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("round-robin")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(RoundRobin.class))
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("choice")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(ChoiceRouter.class))
                                                 .withObjectFactoryType(ChoiceRouterFactoryBean.class)
                                                 .withSetterParameterDefinition("routes", fromChildListConfiguration(MessageProcessorFilterPair.class).build())
                                                 .withSetterParameterDefinition("defaultRoute", fromChildConfiguration(MessageProcessorFilterPair.class).build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("when")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(MessageProcessorFilterPair.class))
                                                 .withObjectFactoryType(MessageProcessorFilterPairFactoryBean.class)
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("otherwise")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(MessageProcessorFilterPair.class))
                                                 .withObjectFactoryType(MessageProcessorFilterPairFactoryBean.class)
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("expression", fromNoConfiguration().withDefaultValue("true").build())
                                                 .build());
        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("all")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(MulticastingRouter.class))
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("response")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(ResponseMessageProcessorAdapter.class))
                                                 .withObjectFactoryType(ResponseMessageProcessorsFactoryBean.class)
                                                 .withSetterParameterDefinition("messageProcessors", fromChildListConfiguration(MessageProcessor.class).build())
                                                 .build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("poll")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(PollingMessageSource.class))
                                                 .withObjectFactoryType(PollingMessageSourceFactoryBean.class)
                                                 .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(MessageProcessor.class).build())
                                                 .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build())
                                                 .withSetterParameterDefinition("override", fromChildConfiguration(MessageProcessorPollingOverride.class).build())
                                                 .withSetterParameterDefinition("schedulerFactory", fromChildConfiguration(SchedulerFactory.class).build())
                                                 .build());

        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("fixed-frequency-scheduler")
                                                 .withNamespace("mule")
                                                 .withTypeDefinitionBuilder(fromType(FixedFrequencySchedulerFactory.class))
                                                 .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build())
                                                 .withSetterParameterDefinition("startDelay", fromSimpleParameter("startDelay").build())
                                                 .withSetterParameterDefinition("timeUnit", fromSimpleParameter("timeUnit").build())
                                                 .build());


        componentBuildingDefinitions.add(new ComponentBuildingDefinition.Builder()
                                                 .withName("watermark")
                                                 .withNamespace("mule")
                                                 .withSetterParameterDefinition("variable", fromSimpleParameter("variable").build())
                                                 .withSetterParameterDefinition("defaultExpression", fromSimpleParameter("default-expression").build())
                                                 .withSetterParameterDefinition("updateExpression", fromSimpleParameter("update-expression").build())
                                                 .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("object-store-ref").build())
                                                 .withSetterParameterDefinition("selector", fromSimpleParameter("selector").build())
                                                 .withSetterParameterDefinition("selectorExpression", fromSimpleParameter("selector-expression").build())
                                                 .withTypeDefinitionBuilder(fromType(Watermark.class))
                                                 .withObjectFactoryType(WatermarkFactoryBean.class)
                                                 .build());

        return componentBuildingDefinitions;
    }

    private ComponentBuildingDefinition.Builder createTransactionManagerDefinitionBuilder(String transactionManagerName, Class<?> transactionManagerClass)
    {
        return transactionManagerBaseDefinition.copy().withName(transactionManagerName).withTypeDefinitionBuilder(fromType(transactionManagerClass));
    }

    private ComponentBuildingDefinition.Builder createTransformerDefinitionBuilder(String transformerName, Class<?> transformerClass)
    {
        return transformerBaseDefinition.copy().withName(transformerName).withTypeDefinitionBuilder(fromType(transformerClass));
    }

    @Override
    public boolean isDefaultConfigsProvider()
    {
        return false;
    }

}
