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

package org.apache.ivy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.ivy.util.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A {@link HttpHandler} that can be used in tests to serve local file system based resources over
 * HTTP NOTE: This handler reads the complete file contents, all into memory while serving it and
 * thus isn't suitable with very large files. Use this handler only in test cases where the files
 * to serve are reasonably small in size
 */
final class LocalFileRepoOverHttp implements HttpHandler {

    private final String webContextRoot;
    private final Path localFileRepoRoot;

    LocalFileRepoOverHttp(final String webContextRoot, final Path localFileRepoRoot) {
        if (!Files.isDirectory(localFileRepoRoot)) {
            throw new IllegalArgumentException(localFileRepoRoot + " is either missing or not a directory");
        }
        this.webContextRoot = webContextRoot;
        this.localFileRepoRoot = localFileRepoRoot;
    }

    @Override
    public void handle(final HttpExchange httpExchange) throws IOException {
        final URI requestURI = httpExchange.getRequestURI();
        Message.info("Handling " + httpExchange.getRequestMethod() + " request " + requestURI);
        final URI artifactURI;
        try {
            artifactURI = new URI(webContextRoot).relativize(requestURI);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        final Path localFilePath = localFileRepoRoot.resolve(artifactURI.toString());
        if (httpExchange.getRequestMethod().equals("HEAD")) {
            final boolean available = this.isPresent(localFilePath);
            if (!available) {
                httpExchange.sendResponseHeaders(404, -1);
            } else {
                httpExchange.sendResponseHeaders(200, -1);
            }
            return;
        }
        if (!httpExchange.getRequestMethod().equals("GET")) {
            throw new IOException("Cannot handle " + httpExchange.getRequestMethod() + " HTTP method");
        }
        final OutputStream responseStream = httpExchange.getResponseBody();
        @SuppressWarnings("unused")
        final int numBytes = this.serve(httpExchange, localFilePath, responseStream);
        responseStream.close();
    }

    private boolean isPresent(final Path localFile) {
        return Files.isRegularFile(localFile);
    }

    private int serve(final HttpExchange httpExchange, final Path localFile, final OutputStream os) throws IOException {
        if (!Files.isRegularFile(localFile)) {
            throw new IOException("No such file at path " + localFile);
        }
        Message.debug("Serving contents of " + localFile + " for request " + httpExchange.getRequestURI());
        final byte[] data = Files.readAllBytes(localFile);
        httpExchange.sendResponseHeaders(200, data.length);
        os.write(data);
        return data.length;
    }
}
