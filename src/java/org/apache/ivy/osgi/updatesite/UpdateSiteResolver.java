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
package org.apache.ivy.osgi.updatesite;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

import org.apache.ivy.core.cache.CacheResourceOptions;
import org.apache.ivy.osgi.repo.RepoDescriptorBasedResolver;
import org.xml.sax.SAXException;

public class UpdateSiteResolver extends RepoDescriptorBasedResolver {

    private String url;

    private Long metadataTtl;

    private Boolean forceMetadataUpdate;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMetadataTtl(Long metadataTtl) {
        this.metadataTtl = metadataTtl;
    }

    public void setForceMetadataUpdate(Boolean forceMetadataUpdate) {
        this.forceMetadataUpdate = forceMetadataUpdate;
    }

    protected void init() {
        if (url == null) {
            throw new RuntimeException("Missing url");
        }
        CacheResourceOptions options = new CacheResourceOptions();
        if (metadataTtl != null) {
            options.setTtl(metadataTtl.longValue());
        }
        if (forceMetadataUpdate != null) {
            options.setForce(forceMetadataUpdate.booleanValue());
        }
        UpdateSiteLoader loader = new UpdateSiteLoader(getRepositoryCacheManager(),
                getEventManager(), options);
        try {
            setRepoDescriptor(loader.load(new URI(url)));
        } catch (IOException e) {
            throw new RuntimeException("IO issue while trying to read the update site ("
                    + e.getMessage() + ")");
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse the updatesite (" + e.getMessage() + ")");
        } catch (SAXException e) {
            throw new RuntimeException("Illformed updatesite (" + e.getMessage() + ")");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Illformed url (" + e.getMessage() + ")");
        }
    }
}
