/*
 * Copyright (c) 2021 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import com.google.common.util.concurrent.Futures;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.spi.FixedDOMSchemaService;
import org.opendaylight.mdsal.dom.spi.SimpleDOMActionResult;
import org.opendaylight.restconf.api.ApiPath;
import org.opendaylight.restconf.api.query.PrettyPrintParam;
import org.opendaylight.restconf.nb.rfc8040.AbstractInstanceIdentifierTest;
import org.opendaylight.restconf.nb.rfc8040.AbstractJukeboxTest;
import org.opendaylight.restconf.server.mdsal.MdsalDatabindProvider;
import org.opendaylight.restconf.server.mdsal.MdsalRestconfServer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;

@ExtendWith(MockitoExtension.class)
class Netconf799Test extends AbstractInstanceIdentifierTest {
    private static final QName OUTPUT_QNAME = QName.create(CONT_QNAME, "output");
    private static final Absolute RESET_PATH = Absolute.of(CONT_QNAME, CONT1_QNAME, RESET_QNAME);

    @Mock
    private UriInfo uriInfo;
    @Mock
    private DOMDataBroker dataBroker;
    @Mock
    private DOMActionService actionService;
    @Mock
    private DOMRpcService rpcService;
    @Mock
    private DOMMountPointService mountPointService;
    @Mock
    private AsyncResponse asyncResponse;
    @Captor
    private ArgumentCaptor<Response> captor;

    @Test
    void testInvokeAction() throws Exception {
        doReturn(Futures.immediateFuture(new SimpleDOMActionResult(ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(NodeIdentifier.create(OUTPUT_QNAME))
            .build())))
            .when(actionService).invokeAction(eq(RESET_PATH), any(), any());

        final var restconf = new JaxRsRestconf(
            new MdsalRestconfServer(new MdsalDatabindProvider(new FixedDOMSchemaService(IID_SCHEMA)),
                dataBroker, rpcService, actionService, mountPointService),
            PrettyPrintParam.FALSE);
        doReturn(new MultivaluedHashMap<>()).when(uriInfo).getQueryParameters();
        doReturn(true).when(asyncResponse).resume(captor.capture());
        restconf.postDataJSON(ApiPath.parse("instance-identifier-module:cont/cont1/reset"),
            stringInputStream("""
            {
              "instance-identifier-module:input": {
                "delay": 600
              }
            }"""), uriInfo, asyncResponse);
        final var response = captor.getValue();
        assertEquals(204, response.getStatus());
        assertNull(response.getEntity());
    }

    @Test
    void testInvokeActionOutput() throws Exception {
        doReturn(Futures.immediateFuture(new SimpleDOMActionResult(ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(NodeIdentifier.create(OUTPUT_QNAME))
            .withChild(ImmutableNodes.leafNode(QName.create(OUTPUT_QNAME, "timestamp"), "somevalue"))
            .build())))
            .when(actionService).invokeAction(eq(RESET_PATH), any(), any());

        final var restconf = new JaxRsRestconf(
            new MdsalRestconfServer(new MdsalDatabindProvider(new FixedDOMSchemaService(IID_SCHEMA)),
                dataBroker, rpcService, actionService, mountPointService),
            PrettyPrintParam.FALSE);
        doReturn(new MultivaluedHashMap<>()).when(uriInfo).getQueryParameters();

        final var apiPath = ApiPath.parse("instance-identifier-module:cont/cont1/reset");
        final var body = AbstractRestconfTest.assertFormattableBody(200, ar -> {
            restconf.postDataJSON(apiPath,
                stringInputStream("""
                    {
                      "instance-identifier-module:input": {
                        "delay": 600
                      }
                    }"""), uriInfo, ar);
        });

        AbstractJukeboxTest.assertFormat("""
            {"instance-identifier-module:output":{"timestamp":"somevalue"}}""", body::formatToJSON, false);
        AbstractJukeboxTest.assertFormat("""
            <output xmlns="instance:identifier:module"><timestamp>somevalue</timestamp></output>""", body::formatToXML,
            false);
    }
}
