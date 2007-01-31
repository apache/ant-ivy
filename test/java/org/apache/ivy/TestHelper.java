package org.apache.ivy;

import java.io.File;
import java.util.Date;

import org.apache.ivy.core.cache.CacheManager;
import org.apache.ivy.core.module.descriptor.DefaultArtifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;

public class TestHelper {

	public static File getArchiveFileInCache(Ivy ivy, File cache, String organisation, String module, String revision, String artifact, String type, String ext) {
		return getArchiveFileInCache(ivy.getCacheManager(cache), organisation, module, revision, artifact, type, ext);
	}

	public static File getArchiveFileInCache(CacheManager cacheManager, String organisation, String module, String revision, String artifact, String type, String ext) {
		return cacheManager.getArchiveFileInCache(new DefaultArtifact(ModuleRevisionId.newInstance(organisation, module, revision), new Date(), artifact, type, ext));
	}

}
