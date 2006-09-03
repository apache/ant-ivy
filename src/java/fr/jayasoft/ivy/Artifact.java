/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy;

import java.net.URL;
import java.util.Date;

import fr.jayasoft.ivy.extendable.ExtendableItem;


/**
 * @author x.hanin
 *
 */
public interface Artifact extends ExtendableItem {
    /**
     * Returns the resolved module revision id for this artifact
     * @return
     */
    ModuleRevisionId getModuleRevisionId();
    /**
     * Returns the resolved publication date for this artifact
     * @return the resolved publication date
     */
    Date getPublicationDate();
    String getName();
    String getType();
    String getExt();
    /**
     * Returns the url at which this artifact can be found independently of ivy configuration.
     * This can be null (and is usually for standard artifacts)
     * @return url at which this artifact can be found independently of ivy configuration
     */
    URL getUrl();
    String[] getConfigurations();

    /**
     * @return the id of the artifact
     */
    ArtifactRevisionId getId();
}
