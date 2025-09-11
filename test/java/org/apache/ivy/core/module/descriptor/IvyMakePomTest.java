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

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.xpath.XPathConstants;

import org.apache.ivy.TestHelper;
import org.apache.ivy.ant.IvyMakePom;
import org.apache.ivy.util.TestXmlHelper;

import org.apache.tools.ant.Project;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeLines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link IvyMakePom}.
 */
public class IvyMakePomTest {

    private Project project = TestHelper.newProject();

    @Rule
    public TemporaryFolder workdir = new TemporaryFolder();

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/IVY-1528">IVY-1528</a>.
     * <p>
     * An Ivy file containing a <code>classifier</code> extra attribute in its
     * dependency, must retain the <code>classifier</code> in the generated POM
     * when converted to a POM file through {@link IvyMakePom}.
     */
    @Test
    public void testMakePom1528() throws Exception {
        File ivyFile = new File(IvyMakePomTest.class.getResource("ivy-to-pom-classifier.xml").toURI());
        assertTrue(ivyFile + " is either missing or not a file", ivyFile.isFile());
        File pomFile = workdir.newFile("test-ivy-to-pom-classifier.pom");

        IvyMakePom task = new IvyMakePom();
        task.setIvyFile(ivyFile);
        task.setPomFile(pomFile);
        task.setProject(project);
        task.execute();

        NodeList dependencies = (NodeList) TestXmlHelper.evaluateXPathExpr(pomFile, "/project/dependencies/dependency", XPathConstants.NODESET);
        assertNotNull("Dependencies element wasn't found in the generated POM file", dependencies);
        assertEquals("Unexpected number of dependencies in the generated POM file", 2, dependencies.getLength());

        Set<String> expectedPomArtifactIds = new HashSet<>();
        expectedPomArtifactIds.add("foo");
        expectedPomArtifactIds.add("bar");
        for (int i = 0; i < dependencies.getLength(); i++) {
            PomDependency pomDependency = PomDependency.parse(dependencies.item(i));
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

    /**
     * Test case for <a href="https://issues.apache.org/jira/browse/IVY-1653">IVY-1653</a>.
     */
    @Test
    public void testMakePom1653() throws Exception {
        File ivyFile = workdir.newFile("ivy-1653.xml");
        writeLines(ivyFile, "UTF-8", Arrays.asList(
            "<ivy-module version='2.0'>",
            "  <info module='name' organisation='org' revision='1.0.0-SNAPSHOT' />",
            "  <configurations>",
            "    <conf name='default' />",
            "  </configurations>",
            "  <dependencies defaultconf='default' defaultconfmapping='*->master,runtime()'>",
            "    <dependency org='org.springframework' name='spring-aop' rev='6.2.9' />",
            "    <override org='org.aspectj' module='aspectjrt' rev='1.9.24' />",
            "  </dependencies>",
            "</ivy-module>"
        ));

        File pomFile = workdir.newFile("ivy-1653.pom");

        IvyMakePom task = new IvyMakePom();
        task.setIvyFile(ivyFile);
        task.setPomFile(pomFile);
        task.setPrintIvyInfo(false);
        task.setProject(project);

        IvyMakePom.Mapping mapping = task.createMapping();
        mapping.setConf("default");
        mapping.setScope("compile");

        task.execute();

        String[] expect = {
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
            "    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
            "",
            "  <modelVersion>4.0.0</modelVersion>",
            "  <groupId>org</groupId>",
            "  <artifactId>name</artifactId>",
            "  <packaging>jar</packaging>",
            "  <version>1.0.0-SNAPSHOT</version>",
            "  <dependencies>",
            "    <dependency>",
            "      <groupId>org.springframework</groupId>",
            "      <artifactId>spring-aop</artifactId>",
            "      <version>6.2.9</version>",
            "      <scope>compile</scope>",
            "    </dependency>",
            "  </dependencies>",
            "  <dependencyManagement>",
            "    <dependencies>",
            "      <dependency>",
            "        <groupId>org.aspectj</groupId>",
            "        <artifactId>aspectjrt</artifactId>",
            "        <version>1.9.24</version>",
            "      </dependency>",
            "    </dependencies>",
            "  </dependencyManagement>",
            "</project>",
            ""
        };

        assertEquals(String.join(System.lineSeparator(), expect), readFileToString(pomFile, "UTF-8"));
    }

    @Test
    public void testMakePomWithTemplate() throws Exception {
        File ivyFile = workdir.newFile("ivy.xml");
        writeLines(ivyFile, "UTF-8", Arrays.asList(
            "<ivy-module version='2.0'>",
            "  <info module='name' organisation='org' revision='1.0.0-SNAPSHOT' />",
            "  <configurations>",
            "    <conf name='default' />",
            "  </configurations>",
            "  <dependencies defaultconf='default' defaultconfmapping='*->master,runtime()'>",
            "    <dependency org='org.springframework' name='spring-aop' rev='6.2.9' />",
            "  </dependencies>",
            "</ivy-module>"
        ));

        File pomFile = workdir.newFile("ivy.pom");

        File templateFile = workdir.newFile("the.pom");
        writeLines(templateFile, "UTF-8", Arrays.asList(
            "<project>",
            "   <groupId>${ivy.pom.groupId}</groupId>",
            "   <artifactId>${ivy.pom.artifactId}</artifactId>",
            "   <version>${ivy.pom.version}</version>",
            "   <dependencies>",
            "      <dependency>",
            "         <groupId>org.springframework</groupId>",
            "         <artifactId>spring-core</artifactId>",
            "         <version>6.2.9</version>",
            "         <scope>compile</scope>",
            "      </dependency>",
            "   </dependencies>",
            "</project>"
        ));

        IvyMakePom task = new IvyMakePom();
        task.setIvyFile(ivyFile);
        task.setPomFile(pomFile);
        task.setPrintIvyInfo(false);
        task.setProject(project);
        task.setTemplateFile(templateFile);

        IvyMakePom.Mapping mapping = task.createMapping();
        mapping.setConf("default");
        mapping.setScope("compile");

        task.execute();

        String[] expect = {
            "<project>",
            "   <groupId>org</groupId>",
            "   <artifactId>name</artifactId>",
            "   <version>1.0.0-SNAPSHOT</version>",
            "   <dependencies>",
            "      <dependency>",
            "         <groupId>org.springframework</groupId>",
            "         <artifactId>spring-core</artifactId>",
            "         <version>6.2.9</version>",
            "         <scope>compile</scope>",
            "      </dependency>",
            "      <dependency>",
            "         <groupId>org.springframework</groupId>",
            "         <artifactId>spring-aop</artifactId>",
            "         <version>6.2.9</version>",
            "         <scope>compile</scope>",
            "      </dependency>",
            "   </dependencies>",
            "</project>",
            ""
        };

        assertEquals(String.join(System.lineSeparator(), expect), readFileToString(pomFile, "UTF-8"));
    }

    @Test
    public void testMakePomWithTemplate2() throws Exception {
        File ivyFile = workdir.newFile("ivy.xml");
        writeLines(ivyFile, "UTF-8", Arrays.asList(
            "<ivy-module version='2.0'>",
            "  <info module='name' organisation='org' revision='1.0.0-SNAPSHOT' />",
            "  <configurations>",
            "    <conf name='default' />",
            "  </configurations>",
            "  <dependencies defaultconf='default' defaultconfmapping='*->master,runtime()'>",
            "    <dependency org='org.springframework' name='spring-aop' rev='6.2.9' />",
            "  </dependencies>",
            "</ivy-module>"
        ));

        File pomFile = workdir.newFile("ivy.pom");

        File templateFile = workdir.newFile("the.pom");
        writeLines(templateFile, "UTF-8", Arrays.asList(
            "<project>",
            "   <groupId>${ivy.pom.groupId}</groupId>",
            "   <artifactId>${ivy.pom.artifactId}</artifactId>",
            "   <version>${ivy.pom.version}</version>",
            "   <dependencyManagement>",
            "      <dependencies>",
            "         <dependency>",
            "            <groupId>org.aspectj</groupId>",
            "            <artifactId>aspectjrt</artifactId>",
            "            <version>1.9.24</version>",
            "         </dependency>",
            "      </dependencies>",
            "   </dependencyManagement>",
            "</project>"
        ));

        IvyMakePom task = new IvyMakePom();
        task.setIvyFile(ivyFile);
        task.setPomFile(pomFile);
        task.setPrintIvyInfo(false);
        task.setProject(project);
        task.setTemplateFile(templateFile);

        IvyMakePom.Mapping mapping = task.createMapping();
        mapping.setConf("default");
        mapping.setScope("compile");

        task.execute();

        String[] expect = {
            "<project>",
            "   <groupId>org</groupId>",
            "   <artifactId>name</artifactId>",
            "   <version>1.0.0-SNAPSHOT</version>",
            "   <dependencyManagement>",
            "      <dependencies>",
            "         <dependency>",
            "            <groupId>org.aspectj</groupId>",
            "            <artifactId>aspectjrt</artifactId>",
            "            <version>1.9.24</version>",
            "         </dependency>",
            "      </dependencies>",
            "   </dependencyManagement>",
            "   <dependencies>",
            "      <dependency>",
            "         <groupId>org.springframework</groupId>",
            "         <artifactId>spring-aop</artifactId>",
            "         <version>6.2.9</version>",
            "         <scope>compile</scope>",
            "      </dependency>",
            "   </dependencies>",
            "</project>",
            ""
        };

        assertEquals(String.join(System.lineSeparator(), expect), readFileToString(pomFile, "UTF-8"));
    }

    //--------------------------------------------------------------------------

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
