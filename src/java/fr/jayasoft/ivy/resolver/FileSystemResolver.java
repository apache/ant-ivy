/*
 * This file is subject to the license found in LICENCE.TXT in the root directory of the project.
 * 
 * #SNAPSHOT#
 */
package fr.jayasoft.ivy.resolver;

import fr.jayasoft.ivy.repository.file.FileRepository;

/**
 * @author X.Hanin
 *
 */
public class FileSystemResolver extends RepositoryResolver {
    public FileSystemResolver() {
        setRepository(new FileRepository());
    }
    public String getTypeName() {
        return "file";
    }
}
