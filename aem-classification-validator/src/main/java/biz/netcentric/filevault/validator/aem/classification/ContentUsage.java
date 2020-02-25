package biz.netcentric.filevault.validator.aem.classification;

/*-
 * #%L
 * AEM Classification Validator
 * %%
 * Copyright (C) 2020 Netcentric - A Cognizant Digital Business
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */

public enum ContentUsage {
    OVERLAY("overlays"), INHERIT("inherits from"), REFERENCE("references");
    
    private final String label;
    ContentUsage(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}
