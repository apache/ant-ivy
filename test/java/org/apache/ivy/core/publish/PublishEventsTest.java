/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.ivy.core.publish;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.IvyEvent;
import org.apache.ivy.core.event.publish.EndArtifactPublishEvent;
import org.apache.ivy.core.event.publish.PublishEvent;
import org.apache.ivy.core.event.publish.StartArtifactPublishEvent;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorParser;
import org.apache.ivy.plugins.resolver.MockResolver;
import org.apache.ivy.plugins.trigger.AbstractTrigger;

import junit.framework.TestCase;

public class PublishEventsTest extends TestCase {

    // maps ArtifactRevisionId to PublishTestCase instance.
    private HashMap expectedPublications;

    // expected values for the current artifact being published.
    private PublishTestCase currentTestCase;

    private boolean expectedOverwrite;

    // number of times PrePublishTrigger has been invoked successfully
    private int preTriggers;

    // number of times PostPublishTrigger has been invoked successfully
    private int postTriggers;

    // number of times an artifact has been successfully published by the resolver
    private int publications;

    // dummy test data that is reused by all cases.
    private File ivyFile;

    private Artifact ivyArtifact;

    private File dataFile;

    private Artifact dataArtifact;

    private ModuleDescriptor publishModule;

    private Collection publishSources;

    private PublishOptions publishOptions;

    // if non-null, InstrumentedResolver will throw this exception during publish
    private IOException publishError;

    // the ivy instance under test
    private Ivy ivy;

    private PublishEngine publishEngine;

    protected void setUp() throws Exception {
        super.setUp();

        // reset test case state.
        resetCounters();

        // this ivy settings should configure an InstrumentedResolver, PrePublishTrigger, and
        // PostPublishTrigger
        // (see inner classes below).
        ivy = Ivy.newInstance();
        ivy.configure(PublishEventsTest.class.getResource("ivysettings-publisheventstest.xml"));
        ivy.pushContext();
        publishEngine = ivy.getPublishEngine();

        // setup dummy ivy and data files to test publishing. since we're testing the engine and not
        // the resolver,
        // we don't really care whether the file actually gets published. we just want to make sure
        // that the engine calls the correct methods in the correct order, and fires required
        // events.
        ivyFile = new File("test/java/org/apache/ivy/core/publish/ivy-1.0-dev.xml");
        assertTrue("path to ivy file not found in test environment", ivyFile.exists());
        // the contents of the data file don't matter.
        dataFile = File.createTempFile("ivydata", ".jar");
        dataFile.deleteOnExit();

        publishModule = XmlModuleDescriptorParser.getInstance().parseDescriptor(ivy.getSettings(),
            ivyFile.toURI().toURL(), false);
        // always use the same source data file, no pattern substitution is required.
        publishSources = Collections.singleton(dataFile.getAbsolutePath());
        // always use the same ivy file, no pattern substitution is required.
        publishOptions = new PublishOptions();
        publishOptions.setSrcIvyPattern(ivyFile.getAbsolutePath());

        // set up our expectations for the test. these variables will
        // be checked by the resolver and triggers during publication.
        dataArtifact = publishModule.getAllArtifacts()[0];
        assertEquals("sanity check", "foo", dataArtifact.getName());
        ivyArtifact = MDArtifact.newIvyArtifact(publishModule);

        expectedPublications = new HashMap();
        expectedPublications.put(dataArtifact.getId(), new PublishTestCase(dataArtifact, dataFile,
                true));
        expectedPublications.put(ivyArtifact.getId(), new PublishTestCase(ivyArtifact, ivyFile,
                true));
        assertEquals("hashCode sanity check:  two artifacts expected during publish", 2,
            expectedPublications.size());

        // push the TestCase instance onto the context stack, so that our
        // triggers and resolver instances can interact with it it.
        IvyContext.getContext().push(PublishEventsTest.class.getName(), this);
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        // reset test state.
        resetCounters();

        // test case is finished, pop the test context off the stack.
        IvyContext.getContext().pop(PublishEventsTest.class.getName());

        // cleanup ivy resources
        if (ivy != null) {
            ivy.popContext();
            ivy = null;
        }
        publishEngine = null;
        if (dataFile != null) {
            dataFile.delete();
        }
        dataFile = null;
        ivyFile = null;
    }

    protected void resetCounters() {
        preTriggers = 0;
        postTriggers = 0;
        publications = 0;

        expectedPublications = null;
        expectedOverwrite = false;
        publishError = null;
        currentTestCase = null;

        ivyArtifact = null;
        dataArtifact = null;
    }

    /**
     * Test a simple artifact publish, without errors or overwrite settings.
     */
    public void testPublishNoOverwrite() throws IOException {
        // no modifications to input required for this case -- call out to the resolver, and verify
        // that
        // all of our test counters have been incremented.
        Collection missing = publishEngine.publish(publishModule.getModuleRevisionId(),
            publishSources, "default", publishOptions);
        assertEquals("no missing artifacts", 0, missing.size());

        // if all tests passed, all of our counter variables should have been updated.
        assertEquals("pre-publish trigger fired and passed all tests", 2, preTriggers);
        assertEquals("post-publish trigger fired and passed all tests", 2, postTriggers);
        assertEquals("resolver received a publish() call, and passed all tests", 2, publications);
        assertEquals("all expected artifacts have been published", 0, expectedPublications.size());
    }

    /**
     * Test a simple artifact publish, with overwrite set to true.
     */
    public void testPublishWithOverwrite() throws IOException {
        // we expect the overwrite settings to be passed through the event listeners and into the
        // publisher.
        this.expectedOverwrite = true;

        // set overwrite to true. InstrumentedResolver will verify that the correct argument value
        // was provided.
        publishOptions.setOverwrite(true);
        Collection missing = publishEngine.publish(publishModule.getModuleRevisionId(),
            publishSources, "default", publishOptions);
        assertEquals("no missing artifacts", 0, missing.size());

        // if all tests passed, all of our counter variables should have been updated.
        assertEquals("pre-publish trigger fired and passed all tests", 2, preTriggers);
        assertEquals("post-publish trigger fired and passed all tests", 2, postTriggers);
        assertEquals("resolver received a publish() call, and passed all tests", 2, publications);
        assertEquals("all expected artifacts have been published", 0, expectedPublications.size());
    }

    /**
     * Test an attempted publish with an invalid data file path.
     */
    public void testPublishMissingFile() throws IOException {
        // delete the datafile. the publish should fail
        // and the ivy artifact should still publish successfully.
        assertTrue("datafile has been destroyed", dataFile.delete());
        PublishTestCase dataPublish = (PublishTestCase) expectedPublications.get(dataArtifact
                .getId());
        dataPublish.expectedSuccess = false;
        Collection missing = publishEngine.publish(publishModule.getModuleRevisionId(),
            publishSources, "default", publishOptions);
        assertEquals("one missing artifact", 1, missing.size());
        assertSameArtifact("missing artifact was returned", dataArtifact, (Artifact) missing
                .iterator().next());

        // if all tests passed, all of our counter variables should have been updated.
        assertEquals("pre-publish trigger fired and passed all tests", 1, preTriggers);
        assertEquals("post-publish trigger fired and passed all tests", 1, postTriggers);
        assertEquals("only the ivy file published successfully", 1, publications);
        assertEquals("publish of all expected artifacts has been attempted", 1,
            expectedPublications.size());
    }

    /**
     * Test an attempted publish in which the target resolver throws an IOException.
     */
    public void testPublishWithException() {
        // set an error to be thrown during publication of the data file.
        this.publishError = new IOException("boom!");
        // we don't care which artifact is attempted; either will fail with an IOException.
        for (Iterator it = expectedPublications.values().iterator(); it.hasNext();) {
            ((PublishTestCase) it.next()).expectedSuccess = false;
        }

        try {
            publishEngine.publish(publishModule.getModuleRevisionId(), publishSources, "default",
                publishOptions);
            fail("if the resolver throws an exception, the engine should too");
        } catch (IOException expected) {
            assertSame("exception thrown by the resolver should be propagated by the engine",
                this.publishError, expected);
        }

        // the publish engine gives up after the resolver throws an exception on the first artifact,
        // so only one set of events should have been fired.
        // note that the initial publish error shouldn't prevent the post-publish trigger from
        // firing.
        assertEquals("pre-publish trigger fired and passed all tests", 1, preTriggers);
        assertEquals("post-publish trigger fired and passed all tests", 1, postTriggers);
        assertEquals("resolver never published successfully", 0, publications);
        assertEquals("publication aborted after first failure", 1, expectedPublications.size());
    }

    /**
     * Assert that two Artifact instances refer to the same artifact and contain the same metadata.
     */
    public static void assertSameArtifact(String message, Artifact expected, Artifact actual) {
        assertEquals(message + ": name", expected.getName(), actual.getName());
        assertEquals(message + ": id", expected.getId(), actual.getId());
        assertEquals(message + ": moduleRevisionId", expected.getModuleRevisionId(),
            actual.getModuleRevisionId());
        assertTrue(message + ": configurations",
            Arrays.equals(expected.getConfigurations(), actual.getConfigurations()));
        assertEquals(message + ": type", expected.getType(), actual.getType());
        assertEquals(message + ": ext", expected.getExt(), actual.getExt());
        assertEquals(message + ": publicationDate", expected.getPublicationDate(),
            actual.getPublicationDate());
        assertEquals(message + ": attributes", expected.getAttributes(), actual.getAttributes());
        assertEquals(message + ": url", expected.getUrl(), actual.getUrl());
    }

    public static class PublishTestCase {
        public Artifact expectedArtifact;

        public File expectedData;

        public boolean expectedSuccess;

        public boolean preTriggerFired;

        public boolean published;

        public boolean postTriggerFired;

        public PublishTestCase(Artifact artifact, File data, boolean success) {
            this.expectedArtifact = artifact;
            this.expectedData = data;
            this.expectedSuccess = success;
        }
    }

    /**
     * Base class for pre- and post-publish-artifact triggers. When the trigger receives an event,
     * the contents of the publish event are examined to make sure they match the variable settings
     * on the calling {@link PublishEventsTest#currentTestCase} instance.
     */
    public static class TestPublishTrigger extends AbstractTrigger {

        public void progress(IvyEvent event) {
            PublishEventsTest test = (PublishEventsTest) IvyContext.getContext().peek(
                PublishEventsTest.class.getName());
            InstrumentedResolver resolver = (InstrumentedResolver) test.ivy.getSettings()
                    .getResolver("default");

            assertNotNull("instrumented resolver configured", resolver);
            assertNotNull("got a reference to the current unit test case", test);

            // test the proper sequence of events by comparing the number of pre-events,
            // post-events, and actual publications.
            assertTrue("event is of correct base type", event instanceof PublishEvent);

            PublishEvent pubEvent = (PublishEvent) event;
            Artifact expectedArtifact = test.currentTestCase.expectedArtifact;
            File expectedData = test.currentTestCase.expectedData;

            assertSameArtifact("event records correct artifact", expectedArtifact,
                pubEvent.getArtifact());
            try {
                assertEquals("event records correct file", expectedData.getCanonicalPath(),
                    pubEvent.getData().getCanonicalPath());

                assertEquals("event records correct overwrite setting", test.expectedOverwrite,
                    pubEvent.isOverwrite());
                assertSame("event presents correct resolver", resolver, pubEvent.getResolver());

                String[] attributes = {"organisation", "module", "revision", "artifact", "type",
                        "ext", "resolver", "overwrite"};
                String[] values = {"apache", "PublishEventsTest", "1.0-dev",
                        expectedArtifact.getName(), expectedArtifact.getType(),
                        expectedArtifact.getExt(), "default",
                        String.valueOf(test.expectedOverwrite)};

                for (int i = 0; i < attributes.length; ++i) {
                    assertEquals("event declares correct value for " + attributes[i], values[i],
                        event.getAttributes().get(attributes[i]));
                }
                // we test file separately, since it is hard to guaranteean exact path match, but we
                // want
                // to make sure that both paths point to the same canonical location on the
                // filesystem
                String filePath = event.getAttributes().get("file").toString();
                assertEquals("event declares correct value for file",
                    expectedData.getCanonicalPath(), new File(filePath).getCanonicalPath());
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

    }

    /**
     * Extends the tests done by {@link TestPublishTrigger} to check that pre-publish events are
     * fired before DependencyResolver.publish() is called, and before post-publish events are
     * fired.
     */
    public static class PrePublishTrigger extends TestPublishTrigger {

        public void progress(IvyEvent event) {

            PublishEventsTest test = (PublishEventsTest) IvyContext.getContext().peek(
                PublishEventsTest.class.getName());
            assertTrue("event is of correct concrete type",
                event instanceof StartArtifactPublishEvent);
            StartArtifactPublishEvent startEvent = (StartArtifactPublishEvent) event;

            // verify that the artifact being publish was in the expected set. set the
            // 'currentTestCase'
            // pointer so that the resolver and post-publish trigger can check against it.
            Artifact artifact = startEvent.getArtifact();
            assertNotNull("event defines artifact", artifact);

            PublishTestCase currentTestCase = (PublishTestCase) test.expectedPublications
                    .remove(artifact.getId());
            assertNotNull("artifact " + artifact.getId() + " was expected for publication",
                currentTestCase);
            assertFalse("current publication has not been visited yet",
                currentTestCase.preTriggerFired);
            assertFalse("current publication has not been visited yet", currentTestCase.published);
            assertFalse("current publication has not been visited yet",
                currentTestCase.postTriggerFired);
            test.currentTestCase = currentTestCase;

            // superclass tests common attributes of publish events
            super.progress(event);

            // increment the call counter in the test
            currentTestCase.preTriggerFired = true;
            ++test.preTriggers;
        }

    }

    /**
     * Extends the tests done by {@link TestPublishTrigger} to check that post-publish events are
     * fired after DependencyResolver.publish() is called, and that the "status" attribute is set to
     * the correct value.
     */
    public static class PostPublishTrigger extends TestPublishTrigger {

        public void progress(IvyEvent event) {
            // superclass tests common attributes of publish events
            super.progress(event);

            PublishEventsTest test = (PublishEventsTest) IvyContext.getContext().peek(
                PublishEventsTest.class.getName());

            // test the proper sequence of events by comparing the current count of pre-events,
            // post-events, and actual publications.
            assertTrue("event is of correct concrete type",
                event instanceof EndArtifactPublishEvent);
            assertTrue("pre-publish event has been triggered", test.preTriggers > 0);

            // test sequence of events
            assertTrue("pre-trigger event has already been fired for this artifact",
                test.currentTestCase.preTriggerFired);
            assertEquals("publication has been done if possible",
                test.currentTestCase.expectedSuccess, test.currentTestCase.published);
            assertFalse("post-publish event has not yet been fired for this artifact",
                test.currentTestCase.postTriggerFired);

            // test the "status" attribute of the post- event.
            EndArtifactPublishEvent endEvent = (EndArtifactPublishEvent) event;
            assertEquals("status bit is set correctly", test.currentTestCase.expectedSuccess,
                endEvent.isSuccessful());

            String expectedStatus = test.currentTestCase.expectedSuccess ? "successful" : "failed";
            assertEquals("status attribute is set to correct value", expectedStatus, endEvent
                    .getAttributes().get("status"));

            // increment the call counter in the wrapper test
            test.currentTestCase.postTriggerFired = true;
            ++test.postTriggers;
        }

    }

    /**
     * When publish() is called, verifies that a pre-publish event has been fired, and also verifies
     * that the method arguments have the correct value. Also simulates an IOException if the
     * current test case demands it.
     */
    public static class InstrumentedResolver extends MockResolver {

        public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {

            // verify that the data from the current test case has been handed down to us
            PublishEventsTest test = (PublishEventsTest) IvyContext.getContext().peek(
                PublishEventsTest.class.getName());

            // test sequence of events.
            assertNotNull(test.currentTestCase);
            assertTrue("preTrigger has already fired", test.currentTestCase.preTriggerFired);
            assertFalse("postTrigger has not yet fired", test.currentTestCase.postTriggerFired);
            assertFalse("publish has not been called", test.currentTestCase.published);

            // test event data
            assertSameArtifact("publisher has received correct artifact",
                test.currentTestCase.expectedArtifact, artifact);
            assertEquals("publisher has received correct datafile",
                test.currentTestCase.expectedData.getCanonicalPath(), src.getCanonicalPath());
            assertEquals("publisher has received correct overwrite setting",
                test.expectedOverwrite, overwrite);
            assertTrue("publisher only invoked when source file exists",
                test.currentTestCase.expectedData.exists());

            // simulate a publisher error if the current test case demands it.
            if (test.publishError != null) {
                throw test.publishError;
            }

            // all assertions pass. increment the publication count
            test.currentTestCase.published = true;
            ++test.publications;
        }
    }

}