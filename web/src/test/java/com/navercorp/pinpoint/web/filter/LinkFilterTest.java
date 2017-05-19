package com.navercorp.pinpoint.web.filter;

import com.navercorp.pinpoint.common.server.bo.AnnotationBo;
import com.navercorp.pinpoint.common.server.bo.SpanEventBo;
import com.navercorp.pinpoint.common.service.AnnotationKeyRegistryService;
import com.navercorp.pinpoint.common.service.ServiceTypeRegistryService;
import com.navercorp.pinpoint.common.trace.AnnotationKey;
import com.navercorp.pinpoint.common.trace.AnnotationKeyFactory;
import com.navercorp.pinpoint.common.trace.ServiceType;
import com.navercorp.pinpoint.common.server.bo.SpanBo;
import com.navercorp.pinpoint.web.util.ServiceTypeRegistryMockFactory;
import org.apache.hadoop.hbase.util.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static com.navercorp.pinpoint.common.trace.ServiceTypeProperty.*;

/**
 * @author emeroad
 */
public class LinkFilterTest {

    private static final short UNKNOWN_TYPE_CODE = ServiceType.UNKNOWN.getCode();
    private static final String UNKNOWN_TYPE_NAME = ServiceType.UNKNOWN.getName();
    private static final short USER_TYPE_CODE = ServiceType.USER.getCode();
    private static final String USER_TYPE_NAME = ServiceType.USER.getName();
    private static final short TOMCAT_TYPE_CODE = 1010;
    private static final String TOMCAT_TYPE_NAME = "TOMCAT";
    private static final short RPC_TYPE_CODE = 9999;
    private static final String RPC_TYPE_NAME = "RPC";
    private static final short BACKEND_TYPE_CODE = 2100;
    private static final String BACKEND_TYPE_NAME = "BACKEND";
    private static final short MESSAGE_QUEUE_TYPE_CODE = 8310;
    private static final String MESSAGE_QUEUE_TYPE_NAME = "MESSAGE_QUEUE";

    private static final int RPC_ANNOTATION_CODE = -1;
    private static final String RPC_ANNOTATION_NAME = "rpc.url";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ServiceTypeRegistryService serviceTypeRegistryService = mockServiceTypeRegistryService();
    private final AnnotationKeyRegistryService annotationKeyRegistryService = mockAnnotationKeyRegistryService();

    private ServiceTypeRegistryService mockServiceTypeRegistryService() {

        ServiceTypeRegistryMockFactory mockFactory = new ServiceTypeRegistryMockFactory();
        mockFactory.addServiceTypeMock(UNKNOWN_TYPE_CODE, UNKNOWN_TYPE_NAME, RECORD_STATISTICS);
        mockFactory.addServiceTypeMock(USER_TYPE_CODE, USER_TYPE_NAME, RECORD_STATISTICS);
        mockFactory.addServiceTypeMock(TOMCAT_TYPE_CODE, TOMCAT_TYPE_NAME, RECORD_STATISTICS);
        mockFactory.addServiceTypeMock(RPC_TYPE_CODE, RPC_TYPE_NAME, RECORD_STATISTICS);
        mockFactory.addServiceTypeMock(BACKEND_TYPE_CODE, BACKEND_TYPE_NAME, TERMINAL, INCLUDE_DESTINATION_ID);
        mockFactory.addServiceTypeMock(MESSAGE_QUEUE_TYPE_CODE, MESSAGE_QUEUE_TYPE_NAME, QUEUE, RECORD_STATISTICS);

        return mockFactory.createMockServiceTypeRegistryService();
    }

    private AnnotationKeyRegistryService mockAnnotationKeyRegistryService() {
        final AnnotationKey rpcUrlAnnotationKey = AnnotationKeyFactory.of(RPC_ANNOTATION_CODE, RPC_ANNOTATION_NAME);
        return new AnnotationKeyRegistryService() {
            @Override
            public AnnotationKey findAnnotationKey(int annotationCode) {
                return rpcUrlAnnotationKey;
            }

            @Override
            public AnnotationKey findAnnotationKeyByName(String keyName) {
                return rpcUrlAnnotationKey;
            }

            @Override
            public AnnotationKey findApiErrorCode(int annotationCode) {
                return rpcUrlAnnotationKey;
            }
        };
    }


    @Test
    public void fromToFilterTest() {
        ServiceType tomcat = serviceTypeRegistryService.findServiceTypeByName(TOMCAT_TYPE_NAME);
        final short tomcatServiceType = tomcat.getCode();

        FilterDescriptor descriptor = new FilterDescriptor();
        descriptor.setFromApplicationName("APP_A");
        descriptor.setFromServiceType(tomcat.getName());
//        descriptor.setFromAgentId("AGENT_A");

        descriptor.setToApplicationName("APP_B");
        descriptor.setToServiceType(tomcat.getName());
//        descriptor.setToAgentId("AGENT_B");

        FilterHint hint = new FilterHint(Collections.emptyList());

        LinkFilter linkFilter = new LinkFilter(descriptor, hint, serviceTypeRegistryService, annotationKeyRegistryService);
        logger.debug(linkFilter.toString());

        SpanBo fromSpanBo = new SpanBo();
        fromSpanBo.setApplicationId("APP_A");

        fromSpanBo.setServiceType(tomcatServiceType);
        fromSpanBo.setAgentId("AGENT_A");
        fromSpanBo.setSpanId(100);

        SpanBo toSpanBO = new SpanBo();
        toSpanBO.setApplicationId("APP_B");
        toSpanBO.setServiceType(tomcatServiceType);
        toSpanBO.setAgentId("AGENT_B");
        toSpanBO.setParentSpanId(100);

        SpanBo spanBoC = new SpanBo();
        spanBoC.setApplicationId("APP_C");
        spanBoC.setServiceType(tomcatServiceType);
        spanBoC.setAgentId("AGENT_C");

        Assert.assertTrue(linkFilter.include(Arrays.asList(fromSpanBo, toSpanBO)));
        Assert.assertFalse(linkFilter.include(Arrays.asList(fromSpanBo, spanBoC)));

    }

    @Test
    public void fromToFilterAgentTest() {
        final ServiceType tomcat = serviceTypeRegistryService.findServiceTypeByName(TOMCAT_TYPE_NAME);
        final short tomcatServiceType = tomcat.getCode();

        FilterDescriptor descriptor = new FilterDescriptor();
        descriptor.setFromApplicationName("APP_A");
        descriptor.setFromServiceType(tomcat.getName());
        descriptor.setFromAgentId("AGENT_A");

        descriptor.setToApplicationName("APP_B");
        descriptor.setToServiceType(tomcat.getName());
        descriptor.setToAgentId("AGENT_B");

        FilterHint hint = new FilterHint(Collections.emptyList());

        LinkFilter linkFilter = new LinkFilter(descriptor, hint, serviceTypeRegistryService, annotationKeyRegistryService);
        logger.debug(linkFilter.toString());

        SpanBo fromSpanBo = new SpanBo();
        fromSpanBo.setApplicationId("APP_A");

        fromSpanBo.setServiceType(tomcatServiceType);
        fromSpanBo.setAgentId("AGENT_A");
        fromSpanBo.setSpanId(100);

        SpanBo toSpanBO = new SpanBo();
        toSpanBO.setApplicationId("APP_B");
        toSpanBO.setServiceType(tomcatServiceType);
        toSpanBO.setAgentId("AGENT_B");
        toSpanBO.setParentSpanId(100);

        SpanBo spanBoC = new SpanBo();
        spanBoC.setApplicationId("APP_C");
        spanBoC.setServiceType(tomcatServiceType);
        spanBoC.setAgentId("AGENT_C");

        Assert.assertTrue(linkFilter.include(Arrays.asList(fromSpanBo, toSpanBO)));
        Assert.assertFalse(linkFilter.include(Arrays.asList(fromSpanBo, spanBoC)));
    }

    @Test
    public void userToWasFilter() {
        final ServiceType user = serviceTypeRegistryService.findServiceTypeByName(USER_TYPE_NAME);
        final ServiceType tomcat = serviceTypeRegistryService.findServiceTypeByName(TOMCAT_TYPE_NAME);

        FilterDescriptor descriptor = new FilterDescriptor();
        descriptor.setFromApplicationName("USER");
        descriptor.setFromServiceType(user.getName());
        descriptor.setToApplicationName("APP_A");
        descriptor.setToServiceType(tomcat.getName());

        FilterHint hint = new FilterHint(Collections.emptyList());

        LinkFilter linkFilter = new LinkFilter(descriptor, hint, serviceTypeRegistryService, annotationKeyRegistryService);
        logger.debug(linkFilter.toString());

        SpanBo user_appA = new SpanBo();
        user_appA.setSpanId(1);
        user_appA.setParentSpanId(-1);
        user_appA.setApplicationId("APP_A");
        user_appA.setApplicationServiceType(tomcat.getCode());
        SpanBo appA_appB = new SpanBo();
        appA_appB.setSpanId(2);
        appA_appB.setParentSpanId(1);
        appA_appB.setApplicationId("APP_B");
        appA_appB.setApplicationServiceType(tomcat.getCode());
        SpanBo appB_appA = new SpanBo();
        appB_appA.setSpanId(3);
        appB_appA.setParentSpanId(2);
        appB_appA.setApplicationId("APP_A");
        appB_appA.setApplicationServiceType(tomcat.getCode());

        Assert.assertTrue(linkFilter.include(Collections.singletonList(user_appA)));
        Assert.assertFalse(linkFilter.include(Collections.singletonList(appA_appB)));
        Assert.assertFalse(linkFilter.include(Collections.singletonList(appB_appA)));
        Assert.assertTrue(linkFilter.include(Arrays.asList(user_appA, appA_appB, appB_appA)));
    }

    @Test
    public void wasToUnknownFilter() {
        final ServiceType tomcat = serviceTypeRegistryService.findServiceTypeByName(TOMCAT_TYPE_NAME);
        final ServiceType unknown = serviceTypeRegistryService.findServiceTypeByName(UNKNOWN_TYPE_NAME);

        final String rpcHost = "some.domain.name";
        final String rpcUrl = "http://" + rpcHost + "/some/test/path";
        final String urlPattern = "/some/test/**";

        FilterDescriptor descriptor = new FilterDescriptor();
        descriptor.setFromApplicationName("APP_A");
        descriptor.setFromServiceType(tomcat.getName());
        descriptor.setToApplicationName(rpcHost);
        descriptor.setToServiceType(unknown.getName());
        descriptor.setUrl(encodeUrl(urlPattern));

        FilterHint hint = new FilterHint(Collections.emptyList());

        LinkFilter linkFilter = new LinkFilter(descriptor, hint, serviceTypeRegistryService, annotationKeyRegistryService);
        logger.debug(linkFilter.toString());

        // Reject - no rpc span event
        SpanBo spanBo = new SpanBo();
        spanBo.setSpanId(1);
        spanBo.setParentSpanId(-1);
        spanBo.setApplicationId("APP_A");
        spanBo.setApplicationServiceType(tomcat.getCode());
        Assert.assertFalse(linkFilter.include(Collections.singletonList(spanBo)));

        // Accept - has matching rpc span event
        AnnotationBo rpcAnnotation = new AnnotationBo();
        rpcAnnotation.setKey(RPC_ANNOTATION_CODE);
        rpcAnnotation.setValue(rpcUrl);
        SpanEventBo rpcSpanEvent = new SpanEventBo();
        rpcSpanEvent.setServiceType(RPC_TYPE_CODE);
        rpcSpanEvent.setDestinationId(rpcHost);
        rpcSpanEvent.setAnnotationBoList(Collections.singletonList(rpcAnnotation));
        spanBo.addSpanEvent(rpcSpanEvent);
        Assert.assertTrue(linkFilter.include(Collections.singletonList(spanBo)));
    }

    @Test
    public void wasToWasFilter_perfectMatch() {
        final ServiceType tomcat = serviceTypeRegistryService.findServiceTypeByName(TOMCAT_TYPE_NAME);

        FilterDescriptor descriptor = new FilterDescriptor();
        descriptor.setFromApplicationName("APP_A");
        descriptor.setFromServiceType(tomcat.getName());
        descriptor.setToApplicationName("APP_B");
        descriptor.setToServiceType(tomcat.getName());

        FilterHint hint = new FilterHint(Collections.emptyList());

        LinkFilter linkFilter = new LinkFilter(descriptor, hint, serviceTypeRegistryService, annotationKeyRegistryService);
        logger.debug(linkFilter.toString());

        // Accept - perfect match
        SpanBo user_appA = new SpanBo();
        user_appA.setSpanId(1);
        user_appA.setParentSpanId(-1);
        user_appA.setApplicationId("APP_A");
        user_appA.setApplicationServiceType(tomcat.getCode());
        SpanBo appA_appB = new SpanBo();
        appA_appB.setSpanId(2);
        appA_appB.setParentSpanId(1);
        appA_appB.setApplicationId("APP_B");
        appA_appB.setApplicationServiceType(tomcat.getCode());
        Assert.assertTrue(linkFilter.include(Arrays.asList(user_appA, appA_appB)));
    }

    @Test
    public void wasToWasFilter_noMatch() {
        final ServiceType tomcat = serviceTypeRegistryService.findServiceTypeByName(TOMCAT_TYPE_NAME);

        FilterDescriptor descriptor = new FilterDescriptor();
        descriptor.setFromApplicationName("APP_A");
        descriptor.setFromServiceType(tomcat.getName());
        descriptor.setToApplicationName("APP_B");
        descriptor.setToServiceType(tomcat.getName());

        FilterHint hint = new FilterHint(Collections.emptyList());

        LinkFilter linkFilter = new LinkFilter(descriptor, hint, serviceTypeRegistryService, annotationKeyRegistryService);
        logger.debug(linkFilter.toString());

        // Reject - fromNode different
        SpanBo user_appC = new SpanBo();
        user_appC.setSpanId(1);
        user_appC.setParentSpanId(-1);
        user_appC.setApplicationId("APP_C");
        user_appC.setApplicationServiceType(tomcat.getCode());
        SpanBo appC_appB = new SpanBo();
        appC_appB.setSpanId(2);
        appC_appB.setParentSpanId(1);
        appC_appB.setApplicationId("APP_B");
        appC_appB.setApplicationServiceType(tomcat.getCode());
        Assert.assertFalse(linkFilter.include(Arrays.asList(user_appC, appC_appB)));

        // Reject - toNode different
        SpanBo user_appA = new SpanBo();
        user_appA.setSpanId(1);
        user_appA.setParentSpanId(-1);
        user_appA.setApplicationId("APP_A");
        user_appA.setApplicationServiceType(tomcat.getCode());
        SpanBo appA_appC = new SpanBo();
        appA_appC.setSpanId(2);
        appA_appC.setParentSpanId(1);
        appA_appC.setApplicationId("APP_C");
        appA_appC.setApplicationServiceType(tomcat.getCode());
        Assert.assertFalse(linkFilter.include(Arrays.asList(user_appA, appA_appC)));
    }

    @Test
    public void wasToBackendFilter() {
        final ServiceType tomcat = serviceTypeRegistryService.findServiceTypeByName(TOMCAT_TYPE_NAME);
        final ServiceType backend = serviceTypeRegistryService.findServiceTypeByName(BACKEND_TYPE_NAME);

        final String destinationA = "BACKEND_A";
        final String destinationB = "BACKEND_B";

        FilterDescriptor descriptor = new FilterDescriptor();
        descriptor.setFromApplicationName("APP_A");
        descriptor.setFromServiceType(tomcat.getName());
        descriptor.setToApplicationName(destinationA);
        descriptor.setToServiceType(backend.getName());

        FilterHint hint = new FilterHint(Collections.emptyList());

        LinkFilter linkFilter = new LinkFilter(descriptor, hint, serviceTypeRegistryService, annotationKeyRegistryService);
        logger.debug(linkFilter.toString());

        SpanBo matchingSpan = new SpanBo();
        matchingSpan.setApplicationId("APP_A");
        matchingSpan.setApplicationServiceType(tomcat.getCode());
        SpanEventBo spanEventDestinationA = new SpanEventBo();
        spanEventDestinationA.setDestinationId(destinationA);
        spanEventDestinationA.setServiceType(BACKEND_TYPE_CODE);
        matchingSpan.addSpanEvent(spanEventDestinationA);
        Assert.assertTrue(linkFilter.include(Collections.singletonList(matchingSpan)));

        SpanBo unmatchingSpan = new SpanBo();
        unmatchingSpan.setApplicationId("APP_A");
        unmatchingSpan.setApplicationServiceType(tomcat.getCode());
        SpanEventBo spanEventDestinationB = new SpanEventBo();
        spanEventDestinationB.setDestinationId(destinationB);
        spanEventDestinationB.setServiceType(BACKEND_TYPE_CODE);
        unmatchingSpan.addSpanEvent(spanEventDestinationB);
        Assert.assertFalse(linkFilter.include(Collections.singletonList(unmatchingSpan)));

        Assert.assertTrue(linkFilter.include(Arrays.asList(matchingSpan, unmatchingSpan)));

        SpanBo bothSpan = new SpanBo();
        bothSpan.setApplicationId("APP_A");
        bothSpan.setApplicationServiceType(tomcat.getCode());
        bothSpan.addSpanEventBoList(Arrays.asList(spanEventDestinationA, spanEventDestinationB));
        Assert.assertTrue(linkFilter.include(Collections.singletonList(bothSpan)));
    }

    @Test
    public void wasToQueueFilter() {
        final ServiceType tomcat = serviceTypeRegistryService.findServiceTypeByName(TOMCAT_TYPE_NAME);
        final ServiceType messageQueue = serviceTypeRegistryService.findServiceTypeByName(MESSAGE_QUEUE_TYPE_NAME);

        final String messageQueueA = "QUEUE_A";
        final String messageQueueB = "QUEUE_B";

        FilterDescriptor descriptor = new FilterDescriptor();
        descriptor.setFromApplicationName("APP_A");
        descriptor.setFromServiceType(tomcat.getName());
        descriptor.setToApplicationName(messageQueueA);
        descriptor.setToServiceType(messageQueue.getName());

        FilterHint hint = new FilterHint(Collections.emptyList());

        LinkFilter linkFilter = new LinkFilter(descriptor, hint, serviceTypeRegistryService, annotationKeyRegistryService);
        logger.debug(linkFilter.toString());

        SpanBo matchingSpan = new SpanBo();
        matchingSpan.setApplicationId("APP_A");
        matchingSpan.setApplicationServiceType(tomcat.getCode());
        SpanEventBo spanEventDestinationA = new SpanEventBo();
        spanEventDestinationA.setDestinationId(messageQueueA);
        spanEventDestinationA.setServiceType(MESSAGE_QUEUE_TYPE_CODE);
        matchingSpan.addSpanEvent(spanEventDestinationA);
        Assert.assertTrue(linkFilter.include(Collections.singletonList(matchingSpan)));

        SpanBo unmatchingSpan = new SpanBo();
        unmatchingSpan.setApplicationId("APP_A");
        unmatchingSpan.setApplicationServiceType(tomcat.getCode());
        SpanEventBo spanEventDestinationB = new SpanEventBo();
        spanEventDestinationB.setDestinationId(messageQueueB);
        spanEventDestinationB.setServiceType(MESSAGE_QUEUE_TYPE_CODE);
        unmatchingSpan.addSpanEvent(spanEventDestinationB);
        Assert.assertFalse(linkFilter.include(Collections.singletonList(unmatchingSpan)));

        Assert.assertTrue(linkFilter.include(Arrays.asList(matchingSpan, unmatchingSpan)));

        SpanBo bothSpan = new SpanBo();
        bothSpan.setApplicationId("APP_A");
        bothSpan.setApplicationServiceType(tomcat.getCode());
        bothSpan.addSpanEventBoList(Arrays.asList(spanEventDestinationA, spanEventDestinationB));
        Assert.assertTrue(linkFilter.include(Collections.singletonList(bothSpan)));
    }

    @Test
    public void queueToWasFilter() {
        final ServiceType tomcat = serviceTypeRegistryService.findServiceTypeByName(TOMCAT_TYPE_NAME);
        final ServiceType messageQueue = serviceTypeRegistryService.findServiceTypeByName(MESSAGE_QUEUE_TYPE_NAME);

        final String messageQueueA = "QUEUE_A";
        final String messageQueueB = "QUEUE_B";

        FilterDescriptor descriptor = new FilterDescriptor();
        descriptor.setFromApplicationName(messageQueueA);
        descriptor.setFromServiceType(messageQueue.getName());
        descriptor.setToApplicationName("APP_A");
        descriptor.setToServiceType(tomcat.getName());

        FilterHint hint = new FilterHint(Collections.emptyList());

        LinkFilter linkFilter = new LinkFilter(descriptor, hint, serviceTypeRegistryService, annotationKeyRegistryService);
        logger.debug(linkFilter.toString());

        SpanBo matchingSpan = new SpanBo();
        matchingSpan.setApplicationId("APP_A");
        matchingSpan.setApplicationServiceType(tomcat.getCode());
        matchingSpan.setAcceptorHost(messageQueueA);
        Assert.assertTrue(linkFilter.include(Collections.singletonList(matchingSpan)));

        SpanBo unmatchingSpan = new SpanBo();
        unmatchingSpan.setApplicationId("APP_A");
        unmatchingSpan.setApplicationServiceType(tomcat.getCode());
        unmatchingSpan.setAcceptorHost(messageQueueB);
        Assert.assertFalse(linkFilter.include(Collections.singletonList(unmatchingSpan)));
    }

    private String encodeUrl(String value) {
        return Base64.encodeBytes(value.getBytes(StandardCharsets.UTF_8));
    }

}