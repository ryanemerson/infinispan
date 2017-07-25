package org.infinispan.server.commons.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.infinispan.server.commons.controller.Operations;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

public abstract class ClusteringSubsystemTest extends AbstractSubsystemBaseTest {
    private final String path;
    private final String xsdPath;
    private final int operations;
    private final String[] templates;

    protected ClusteringSubsystemTest(String name, int operations, String xsdPath, Extension extension, String path, String[] templates) {
        super(name, extension);
        this.path = path;
        this.operations  = operations;
        this.xsdPath = xsdPath;
        this.templates = templates;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource(path);
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return xsdPath;
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return templates;
    }

    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        // TODO: implement once the schema validator supports supplements
    }

    protected ValidationConfiguration getModelValidationConfiguration() {
        return new ValidationConfiguration();
    }

    /**
     * Need to override the XML comparison in the case where the input xsd and the output xsd differ.
     *
     * @param configId   the id of the xml configuration
     * @param original   the original subsystem xml
     * @param marshalled the marshalled subsystem xml
     * @throws Exception
     */
    @Override
    protected void compareXml(String configId, final String original, final String marshalled) throws Exception {

        final XMLStreamReader originalReader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(original));
        final XMLStreamReader marshalledReader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(marshalled));

        String originalNS = null;
        if (originalReader.next() == XMLStreamConstants.START_ELEMENT) {
            originalNS = originalReader.getNamespaceURI();
        }
        String marshalledNS = null;
        if (marshalledReader.next() == XMLStreamConstants.START_ELEMENT) {
            marshalledNS = marshalledReader.getNamespaceURI();
        }

        // only compare if namespace URIs are the same
        if (originalNS.equals(marshalledNS)) {
            compareXml(configId, original, marshalled, true);
        }
    }


    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {
            @Override
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }

            @Override
            protected ValidationConfiguration getModelValidationConfiguration() {
                return ClusteringSubsystemTest.this.getModelValidationConfiguration();
            }
        };
    }

    protected abstract PathElement getSubsystemPath();

    /**
     * Tests that the xml is parsed into the correct operations
     */
    @Test
    public void testParseSubsystem() throws Exception {
        // Parse the subsystem xml into operations
        List<ModelNode> operations = super.parse(getSubsystemXml());

        // Check that we have the expected number of operations
        // one for each resource instance
        Assert.assertEquals(this.operations, operations.size());

        // Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(getMainSubsystemName(), element.getValue());
    }

    /**
     * Test that the model created from the xml looks as expected
     */
    @Test
    public void testInstallIntoController() throws Exception {
        // Parse the subsystem xml and install into the controller
        KernelServices services = createKernelServicesBuilder().setSubsystemXml(getSubsystemXml()).build();

        // Read the whole model and make sure it looks as expected
        ModelNode model = services.readWholeModel();

        Assert.assertTrue(model.get(SUBSYSTEM).hasDefined(getMainSubsystemName()));
    }

    /**
     * Starts a controller with a given subsystem xml and then checks that a second controller
     * started with the xml marshalled from the first one results in the same model
     */
    @Test
    public void testParseAndMarshalModel() throws Exception {
        // Parse the subsystem xml and install into the first controller

        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(this.getSubsystemXml()).build();

        // Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();

        String marshalled = servicesA.getPersistedSubsystemXml();

        // Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = this.createKernelServicesBuilder().setSubsystemXml(marshalled).build();
        ModelNode modelB = servicesB.readWholeModel();

        // Make sure the models from the two controllers are identical
        this.compare(modelA, modelB);
    }

    /**
     * Starts a controller with the given subsystem xml and then checks that a second controller
     * started with the operations from its describe action results in the same model
     */
    @Test
    public void testDescribeHandler() throws Exception {
        // Parse the subsystem xml and install into the first controller
        KernelServices servicesA = this.createKernelServicesBuilder().setSubsystemXml(this.getSubsystemXml()).build();
        // Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();

        ModelNode operation = Operations.createDescribeOperation(PathAddress.pathAddress(getSubsystemPath()));
        List<ModelNode> operations = checkResultAndGetContents(servicesA.executeOperation(operation)).asList();

        // Install the describe options from the first controller into a second controller
        KernelServices servicesB = this.createKernelServicesBuilder().setBootOperations(operations).build();
        ModelNode modelB = servicesB.readWholeModel();

        // Make sure the models from the two controllers are identical
        this.compare(modelA, modelB);
    }

    private KernelServicesBuilder createKernelServicesBuilder() throws Exception {
        return this.createKernelServicesBuilder(this.createAdditionalInitialization());
    }
}
