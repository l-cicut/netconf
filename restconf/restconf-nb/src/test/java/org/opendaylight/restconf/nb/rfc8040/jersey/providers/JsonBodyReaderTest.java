/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.restconf.nb.rfc8040.jersey.providers;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import javax.ws.rs.core.MediaType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.restconf.nb.rfc8040.legacy.NormalizedNodePayload;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.ActionDefinition;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public class JsonBodyReaderTest extends AbstractBodyReaderTest {
    private static final QNameModule INSTANCE_IDENTIFIER_MODULE_QNAME = QNameModule.create(
        XMLNamespace.of("instance:identifier:module"), Revision.of("2014-01-17"));
    private static final MediaType MEDIA_TYPE = new MediaType(MediaType.APPLICATION_JSON, null);

    private static EffectiveModelContext schemaContext;

    private final JsonNormalizedNodeBodyReader jsonBodyReader;

    public JsonBodyReaderTest() {
        super(schemaContext);
        jsonBodyReader = new JsonNormalizedNodeBodyReader(databindProvider, mountPointService);
    }

    @BeforeClass
    public static void initialization() throws FileNotFoundException {
        final var testFiles = loadFiles("/instanceidentifier/yang");
        testFiles.addAll(loadFiles("/modules"));
        schemaContext = YangParserTestUtils.parseYangFiles(testFiles);
    }

    @Test
    public void moduleSubContainerDataPostTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext
                .getDataChildByName(QName.create(INSTANCE_IDENTIFIER_MODULE_QNAME, "cont"));
        final QName cont1QName = QName.create(dataSchemaNode.getQName(), "cont1");
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName()).node(cont1QName);
        final String uri = "instance-identifier-module:cont";
        mockPostBodyReader(uri, jsonBodyReader);
        final NormalizedNodePayload payload = jsonBodyReader.readFrom(null, null, null, MEDIA_TYPE, null,
            JsonBodyReaderTest.class.getResourceAsStream("/instanceidentifier/json/json_sub_container.json"));
        checkNormalizedNodePayload(payload);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, payload, dataII);
    }

    @Test
    public void moduleSubContainerDataPostActionTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext
            .getDataChildByName(QName.create(INSTANCE_IDENTIFIER_MODULE_QNAME, "cont"));
        final QName cont1QName = QName.create(dataSchemaNode.getQName(), "cont1");
        final QName actionQName = QName.create(dataSchemaNode.getQName(), "reset");
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName())
            .node(cont1QName).node(actionQName);
        final String uri = "instance-identifier-module:cont/cont1/reset";
        mockPostBodyReader(uri, jsonBodyReader);
        final NormalizedNodePayload payload = jsonBodyReader.readFrom(null, null, null, MEDIA_TYPE, null,
            JsonBodyReaderTest.class.getResourceAsStream("/instanceidentifier/json/json_cont_action.json"));
        checkNormalizedNodePayload(payload);
        assertThat(payload.getInstanceIdentifierContext().getSchemaNode(), instanceOf(ActionDefinition.class));
    }

    @Test
    public void moduleSubContainerAugmentDataPostTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext
                .getDataChildByName(QName.create(INSTANCE_IDENTIFIER_MODULE_QNAME, "cont"));
        final Module augmentModule = schemaContext.findModules(XMLNamespace.of("augment:module")).iterator().next();
        final QName contAugmentQName = QName.create(augmentModule.getQNameModule(), "cont-augment");
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName(), contAugmentQName);
        final String uri = "instance-identifier-module:cont";
        mockPostBodyReader(uri, jsonBodyReader);
        final NormalizedNodePayload payload = jsonBodyReader.readFrom(null, null, null, MEDIA_TYPE, null,
            XmlBodyReaderTest.class.getResourceAsStream("/instanceidentifier/json/json_augment_container.json"));
        checkNormalizedNodePayload(payload);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, payload, dataII);
    }

    @Test
    public void moduleSubContainerChoiceAugmentDataPostTest() throws Exception {
        final DataSchemaNode dataSchemaNode = schemaContext
                .getDataChildByName(QName.create(INSTANCE_IDENTIFIER_MODULE_QNAME, "cont"));
        final Module augmentModule = schemaContext.findModules(XMLNamespace.of("augment:module")).iterator().next();
        final QName augmentChoice1QName = QName.create(augmentModule.getQNameModule(), "augment-choice1");
        final QName augmentChoice2QName = QName.create(augmentChoice1QName, "augment-choice2");
        final QName containerQName = QName.create(augmentChoice1QName, "case-choice-case-container1");
        final YangInstanceIdentifier dataII = YangInstanceIdentifier.of(dataSchemaNode.getQName())
                .node(augmentChoice1QName).node(augmentChoice2QName).node(containerQName);
        final String uri = "instance-identifier-module:cont";
        mockPostBodyReader(uri, jsonBodyReader);
        final NormalizedNodePayload payload = jsonBodyReader.readFrom(null, null, null, MEDIA_TYPE, null,
            XmlBodyReaderTest.class.getResourceAsStream("/instanceidentifier/json/json_augment_choice_container.json"));
        checkNormalizedNodePayload(payload);
        checkExpectValueNormalizeNodeContext(dataSchemaNode, payload, dataII);
    }

    private static void checkExpectValueNormalizeNodeContext(final DataSchemaNode dataSchemaNode,
            final NormalizedNodePayload nnContext, final YangInstanceIdentifier dataNodeIdent) {
        assertEquals(dataSchemaNode, nnContext.getInstanceIdentifierContext().getSchemaNode());
        assertEquals(dataNodeIdent, nnContext.getInstanceIdentifierContext().getInstanceIdentifier());
        assertNotNull(NormalizedNodes.findNode(nnContext.getData(), dataNodeIdent));
    }
}