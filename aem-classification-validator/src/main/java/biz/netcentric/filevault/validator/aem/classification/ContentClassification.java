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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * 
 *  This always refers to a classification of a {@code sling:resourceType}.
 *  
 *  @see <a href="https://helpx.adobe.com/experience-manager/6-4/sites/deploying/using/sustainable-upgrades.html">Content
 *      Classification</a> 
 */
public enum ContentClassification {

    // the order is from most restricted to least restricted
    INTERNAL("granite:InternalArea", true, null, ContentUsage.OVERLAY, ContentUsage.INHERIT, ContentUsage.REFERENCE),
    /**
     * Used to mark child content of {@link FINAL}, this is not having a dedicated mixin!
     */
    INTERNAL_CHILD("granite:InternalArea (derived from parent content classification)", false, null, ContentUsage.OVERLAY, ContentUsage.INHERIT, ContentUsage.REFERENCE),
    /**
     * Used to mark content as deprecated (i.e. no access whatsoever allowed) due to cq:deprecated property for areas which are not {@link INTERNAL} or {@link INTERNAL_CHILD} according to their mixins yet!
     */
    INTERNAL_DEPRECATED_ANNOTATION("granite:InternalArea (derived from cq:deprecated property)", false, null, ContentUsage.OVERLAY, ContentUsage.INHERIT, ContentUsage.REFERENCE),
    /**
     * Used to mark content as deprecated (i.e. no access whatsoever allowed) for areas which are not {@link INTERNAL} or {@link INTERNAL_CHILD} according to their mixins yet!
     */
    INTERNAL_DEPRECATED("granite:InternalArea (derived from deprecation)", false, null, ContentUsage.OVERLAY, ContentUsage.INHERIT, ContentUsage.REFERENCE),
    FINAL("granite:FinalArea", true, INTERNAL_CHILD, ContentUsage.OVERLAY, ContentUsage.INHERIT),
    ABSTRACT("granite:AbstractArea", true, null, ContentUsage.REFERENCE), 
    PUBLIC("granite:PublicArea", true, null);
    
    private final String label;
    private final boolean labelIsMixin;
    private final List<ContentUsage> disallowedUsages;
    private final ContentClassification childNodeClassification;

    ContentClassification(String label, boolean labelIsMixin, ContentClassification childNodeClassification, ContentUsage... disallowedUsages) {
        this.label = label;
        this.labelIsMixin = labelIsMixin;
        this.disallowedUsages = Arrays.asList(disallowedUsages);
        this.childNodeClassification = childNodeClassification;
    }

    public ContentClassification getChildNodeClassification() {
        if (childNodeClassification == null) {
            return this;
        } else {
            return this.childNodeClassification;
        }
    }

    public boolean isAllowed(ContentUsage contentUsage) {
        return !disallowedUsages.contains(contentUsage);
    }

    /**
     * 
     * @param contentUsages the usages to check for
     * @return only {@code true} in case all {@code contentUsages} are allowed
     */
    public boolean isAllowed(EnumSet<ContentUsage> contentUsages) {
        return Collections.disjoint(disallowedUsages, contentUsages);
    }

    /**
     * 
     * @return the label which in some cases is the mixin
     * @see #isLabelMixin()
     */
    public String getLabel() {
        return label;
    }

    /**
     * 
     * @return {@code true} in case the label is a mixin, otherwise {@code false}
     * @see #getLabel()
     */
    public boolean isLabelMixin() {
        return labelIsMixin;
    }

    
}
