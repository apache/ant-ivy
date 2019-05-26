/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.ivy.core.module.descriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.xml.xpath.XPathConstants;

import org.apache.ivy.TestHelper;
import org.apache.ivy.ant.IvyMakePom;
import org.apache.ivy.util.TestXmlHelper;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tests {@link IvyMakePom}
 */
public class IvyMakePomTest {

    private Project project;

    @Rule
    public TemporaryFolder workdir = new TemporaryFolder();

    @Before
    public void beforeTest() {
        this.project = TestHelper.newProject();
    }

    /**
     * Test case for IVY-1528. An Ivy file containing a <code>classifier</code> extra attribute in
     * its dependency, must retain the <code>classifier</code> in the generated POM when converted
     * to a POM file through {@link IvyMakePom}.
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://issues.apache.org/jira/browse/IVY-1528">IVY-1528</a>
     */
    @Test
    public void testClassifier() throws Exception {
        final File ivyFile = new File(IvyMakePomTest.class.getResource("ivy-to-pom-classifier.xml").toURI());
        assertTrue(ivyFile + " is either missing or not a file", ivyFile.isFile());
        final IvyMakePom makepom = new IvyMakePom();
        makepom.setProject(project);
        final File generatedPomFile = workdir.newFile("test-ivy-to-pom-classifier.pom");
        makepom.setPomFile(generatedPomFile);
        makepom.setIvyFile(ivyFile);
        // run the task
        makepom.execute();

        // read the generated pom
        final NodeList dependencies = (NodeList) TestXmlHelper.evaluateXPathExpr(generatedPomFile, "/project/dependencies/dependency", XPathConstants.NODESET);
        assertNotNull("Dependencies element wasn't found in the generated POM file", dependencies);
        assertEquals("Unexpected number of dependencies in the generated POM file", 2, dependencies.getLength());

        final Set<String> expectedPomArtifactIds = new HashSet<>();
        expectedPomArtifactIds.add("foo");
        expectedPomArtifactIds.add("bar");
        for (int i = 0; i < dependencies.getLength(); i++) {
            final PomDependency pomDependency = PomDependency.parse(dependencies.item(i));
            assertNotNull("Dependency generated was null", pomDependency);
            assertTrue("Unexpected dependency " + pomDependency, expectedPomArtifactIds.contains(pomDependency.artifactId));
            // we no longer expect this, so remove it
            expectedPomArtifactIds.remove(pomDependency.artifactId);

            if (pomDependency.artifactId.equals("foo")) {
                assertEquals("Unexpected group id for generated dependency " + pomDependency, "org", pomDependency.groupId);
                assertEquals("Unexpected version for generated dependency " + pomDependency, "1.2.3", pomDependency.version);
                assertNull("Classifier was expected to be absent for dependency " + pomDependency, pomDependency.classifier);
            } else if (pomDependency.artifactId.equals("bar")) {
                assertEquals("Unexpected group id for generated dependency " + pomDependency, "apache", pomDependency.groupId);
                assertEquals("Unexpected version for generated dependency " + pomDependency, "2.0.0", pomDependency.version);
                assertEquals("Unexpected classifier for dependency " + pomDependency, "class1", pomDependency.classifier);
            }
        }
        assertTrue("Some expected dependencies " + expectedPomArtifactIds + " were not found in the generated POM file", expectedPomArtifactIds.isEmpty());
    }

    private static final class PomDependency {
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String scope;
        private final String classifier;
        @SuppressWarnings("unused")
        private final boolean optional;

        private PomDependency(final String groupId, final String artifactId, final String version,
                              final String scope, final String classifier) {
            this(groupId, artifactId, version, scope, classifier, false);
        }

        private PomDependency(final String groupId, final String artifactId, final String version,
                              final String scope, final String classifier, final boolean optional) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.scope = scope;
            this.classifier = classifier;
            this.optional = optional;
        }

        static PomDependency parse(final Node dependencyNode) {
            if (dependencyNode == null) {
                return null;
            }
            final NodeList children = dependencyNode.getChildNodes();
            if (children == null) {
                return new PomDependency(null, null, null, null, null);
            }
            String groupId = null;
            String artifactId = null;
            String version = null;
            String scope = null;
            String classifier = null;
            String optional = null;
            Node nextChild = children.item(0);
            while (nextChild != null) {
                nextChild = skipIfTextNode(nextChild);
                if (nextChild == null) {
                    break;
                }
                final String nodeName = nextChild.getNodeName();
                switch (nodeName) {
                    case "groupId":
                        groupId = nextChild.getTextContent();
                        break;
                    case "artifactId":
                        artifactId = nextChild.getTextContent();
                        break;
                    case "version":
                        version = nextChild.getTextContent();
                        break;
                    case "classifier":
                        classifier = nextChild.getTextContent();
                        break;
                    case "scope":
                        scope = nextChild.getTextContent();
                        break;
                    case "optional":
                        optional = nextChild.getTextContent();
                        break;
                    default:
                        throw new RuntimeException("Unexpected child element "
                                + nextChild.getNodeName() + " under dependency element");
                }
                // move to next sibling
                nextChild = nextChild.getNextSibling();
            }
            return new PomDependency(groupId, artifactId, version, scope, classifier,
                    Boolean.parseBoolean(optional));
        }

        private static Node skipIfTextNode(final Node node) {
            if (node.getNodeType() == Node.TEXT_NODE) {
                return node.getNextSibling();
            }
            return node;
        }

        @Override
        public String toString() {
            return String.format("PomDependency{groupId='%s', artifactId='%s', version='%s', scope='%s', classifier='%s'}",
                    groupId, artifactId, version, scope, classifier);
        }
    }
}
