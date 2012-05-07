/**
 *  This document is a part of the source code and related artifacts
 *  for CollectionSpace, an open source collections management system
 *  for museums and related institutions:

 *  http://www.collectionspace.org
 *  http://wiki.collectionspace.org

 *  Copyright 2009 University of California at Berkeley

 *  Licensed under the Educational Community License (ECL), Version 2.0.
 *  You may not use this file except in compliance with this License.

 *  You may obtain a copy of the ECL 2.0 License at

 *  https://source.collectionspace.org/collection-space/LICENSE.txt

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.collectionspace.services.location.nuxeo;

import java.util.List;
import java.util.regex.Pattern;
import org.collectionspace.services.common.api.Tools;
import org.collectionspace.services.common.document.InvalidDocumentException;
import org.collectionspace.services.common.document.ValidatorHandlerImpl;
import org.collectionspace.services.location.LocTermGroup;
import org.collectionspace.services.location.LocTermGroupList;
import org.collectionspace.services.location.LocationsCommon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LocationValidatorHandler
 * 
 * Validates data supplied when attempting to create and/or update Location records.
 */
public class LocationValidatorHandler extends ValidatorHandlerImpl {

    final Logger logger = LoggerFactory.getLogger(LocationValidatorHandler.class);
    // 'Bad pattern' for shortIdentifiers matches any non-word characters
    private static final Pattern SHORT_ID_BAD_PATTERN = Pattern.compile("[\\W]"); //.matcher(input).matches()
    private static final String VALIDATION_ERROR = "The record payload was invalid. See log file for more details.";
    private static final String SHORT_ID_BAD_CHARS_ERROR =
            "shortIdentifier must only contain standard word characters";
    private static final String HAS_NO_TERMS_ERROR =
            "Authority items must contain at least one term.";
    private static final String HAS_AN_EMPTY_TERM_ERROR =
            "Each term group in an authority item must contain "
            + "a non-empty term name or "
            + "a non-empty term display name.";

    @Override
    protected Class getCommonPartClass() {
        return LocationsCommon.class;
    }

    @Override
    protected void handleCreate() throws InvalidDocumentException {
        LocationsCommon location = (LocationsCommon) getCommonPart();
        // No guarantee that there is a common part in every post/update.
        if (location != null) {
            try {
                String shortId = location.getShortIdentifier();
                if (shortId != null) {
                    CS_ASSERT(shortIdentifierContainsOnlyValidChars(shortId), SHORT_ID_BAD_CHARS_ERROR);
                }
                CS_ASSERT(containsAtLeastOneTerm(location), HAS_NO_TERMS_ERROR);
                CS_ASSERT(allTermsContainNameOrDisplayName(location), HAS_AN_EMPTY_TERM_ERROR);
            } catch (AssertionError e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
                throw new InvalidDocumentException(VALIDATION_ERROR, e);
            }
        }
    }

    @Override
    protected void handleGet() throws InvalidDocumentException {
    }

    @Override
    protected void handleGetAll() throws InvalidDocumentException {
    }

    @Override
    protected void handleUpdate() throws InvalidDocumentException {
        LocationsCommon location = (LocationsCommon) getCommonPart();
        // No guarantee that there is a common part in every post/update.
        if (location != null) {
            try {
                // shortIdentifier is among a set of fields that are
                // prevented from being changed on an update, and thus
                // we don't need to check its value here.
                CS_ASSERT(containsAtLeastOneTerm(location), HAS_NO_TERMS_ERROR);
                CS_ASSERT(allTermsContainNameOrDisplayName(location), HAS_AN_EMPTY_TERM_ERROR);
            } catch (AssertionError e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
                throw new InvalidDocumentException(VALIDATION_ERROR, e);
            }
        }
    }

    @Override
    protected void handleDelete() throws InvalidDocumentException {
    }

    private boolean shortIdentifierContainsOnlyValidChars(String shortId) {
        // Check whether any characters match the 'bad' pattern
        if (SHORT_ID_BAD_PATTERN.matcher(shortId).find()) {
            return false;
        }
        return true;
    }

    private boolean containsAtLeastOneTerm(LocationsCommon person) {
        LocTermGroupList termGroupList = person.getLocTermGroupList();
        if (termGroupList == null) {
            return false;
        }
        List<LocTermGroup> termGroups = termGroupList.getLocTermGroup();
        if ((termGroups == null) || (termGroups.isEmpty())) {
            return false;
        }
        return true;
    }

    private boolean allTermsContainNameOrDisplayName(LocationsCommon person) {
        LocTermGroupList termGroupList = person.getLocTermGroupList();
        List<LocTermGroup> termGroups = termGroupList.getLocTermGroup();
        for (LocTermGroup termGroup : termGroups) {
            if (Tools.isBlank(termGroup.getTermName()) || Tools.isBlank(termGroup.getTermDisplayName())) {
                return false;
            }
        }
        return true;
    }
}