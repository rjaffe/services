/**
 * This document is a part of the source code and related artifacts
 * for CollectionSpace, an open source collections management system
 * for museums and related institutions:
 *
 * http://www.collectionspace.org
 * http://wiki.collectionspace.org
 *
 * Copyright © 2009 Regents of the University of California
 *
 * Licensed under the Educational Community License (ECL), Version 2.0.
 * You may not use this file except in compliance with this License.
 *
 * You may obtain a copy of the ECL 2.0 License at
 * https://source.collectionspace.org/collection-space/LICENSE.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.collectionspace.services.client.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.collectionspace.services.PersonJAXBSchema;
import org.collectionspace.services.client.LoanoutClient;
import org.collectionspace.services.client.PersonAuthorityClient;
import org.collectionspace.services.client.PersonAuthorityClientUtils;
import org.collectionspace.services.common.authorityref.AuthorityRefList;
import org.collectionspace.services.common.authorityref.AuthorityRefList.AuthorityRefItem;
import org.collectionspace.services.loanout.LoansoutCommon;
import org.collectionspace.services.loanout.LoansoutCommonList;

import org.jboss.resteasy.client.ClientResponse;

import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.jboss.resteasy.plugins.providers.multipart.MultipartOutput;
import org.jboss.resteasy.plugins.providers.multipart.OutputPart;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoanoutAuthRefsTest, carries out Authority References tests against a
 * deployed and running Loanout (aka Loans Out) Service.
 *
 * $LastChangedRevision: 1327 $
 * $LastChangedDate: 2010-02-12 10:35:11 -0800 (Fri, 12 Feb 2010) $
 */
public class LoanoutAuthRefsTest extends BaseServiceTest {

   private final Logger logger =
       LoggerFactory.getLogger(LoanoutAuthRefsTest.class);

    // Instance variables specific to this test.
    final String SERVICE_PATH_COMPONENT = "loansout";
    final String PERSON_AUTHORITY_NAME = "TestPersonAuth";
    private String knownResourceId = null;
    private List<String> loanoutIdsCreated = new ArrayList();
    private List<String> personIdsCreated = new ArrayList();
    private int CREATED_STATUS = Response.Status.CREATED.getStatusCode();
    private int OK_STATUS = Response.Status.OK.getStatusCode();
    private String personAuthCSID = null;
    private String borrowersContactRefName = null;
    private String lendersAuthorizerRefName = null;
    private String lendersContactRefName = null;

    // FIXME: Can add 'borrower' - likely to be an organization
    // authority - as an authRef to tests below, and increase the
    // number of expected authRefs to 4.
    private final int NUM_AUTH_REFS_EXPECTED = 3;

    // ---------------------------------------------------------------
    // CRUD tests : CREATE tests
    // ---------------------------------------------------------------
    // Success outcomes
    @Test(dataProvider="testName", dataProviderClass=AbstractServiceTestImpl.class)
    public void createWithAuthRefs(String testName) throws Exception {

        testSetup(CREATED_STATUS, ServiceRequestType.CREATE,testName);

        // Submit the request to the service and store the response.
        String identifier = createIdentifier();

        // Create all the person refs and entities
        createPersonRefs();

        // Create a new Loans In resource.
        //
        // One or more fields in this resource will be PersonAuthority
        // references, and will refer to Person resources by their refNames.
        LoanoutClient loanoutClient = new LoanoutClient();
        MultipartOutput multipart = createLoanoutInstance(
                "loanOutNumber-" + identifier,
                "returnDate-" + identifier,
                borrowersContactRefName,
                lendersAuthorizerRefName,
                lendersContactRefName);
        ClientResponse<Response> res = loanoutClient.create(multipart);
        int statusCode = res.getStatus();

        // Check the status code of the response: does it match
        // the expected response(s)?
        //
        // Specifically:
        // Does it fall within the set of valid status codes?
        // Does it exactly match the expected status code?
        if(logger.isDebugEnabled()){
            logger.debug(testName + ": status = " + statusCode);
        }
        Assert.assertTrue(REQUEST_TYPE.isValidStatusCode(statusCode),
                invalidStatusCodeMessage(REQUEST_TYPE, statusCode));
        Assert.assertEquals(statusCode, EXPECTED_STATUS_CODE);

        // Store the ID returned from the first resource created
        // for additional tests below.
        if (knownResourceId == null){
            knownResourceId = extractId(res);
            if (logger.isDebugEnabled()) {
                logger.debug(testName + ": knownResourceId=" + knownResourceId);
            }
        }
        
        // Store the IDs from every resource created by tests,
        // so they can be deleted after tests have been run.
        loanoutIdsCreated.add(extractId(res));
    }

    protected void createPersonRefs(){

        PersonAuthorityClient personAuthClient = new PersonAuthorityClient();
        // Create a temporary PersonAuthority resource, and its corresponding
        // refName by which it can be identified.
    	String authRefName = 
    		PersonAuthorityClientUtils.createPersonAuthRefName(PERSON_AUTHORITY_NAME, false);
    	MultipartOutput multipart = PersonAuthorityClientUtils.createPersonAuthorityInstance(
    	    PERSON_AUTHORITY_NAME, authRefName, personAuthClient.getCommonPartName());
        ClientResponse<Response> res = personAuthClient.create(multipart);
        int statusCode = res.getStatus();

        Assert.assertTrue(REQUEST_TYPE.isValidStatusCode(statusCode),
            invalidStatusCodeMessage(REQUEST_TYPE, statusCode));
        Assert.assertEquals(statusCode, CREATED_STATUS);
        personAuthCSID = extractId(res);

        // Create temporary Person resources, and their corresponding refNames
        // by which they can be identified.
        borrowersContactRefName =
            PersonAuthorityClientUtils.createPersonRefName(authRefName, "Bradley BorrowersContact", true);
        personIdsCreated.add(createPerson("Bradley", "BorrowersContact", borrowersContactRefName));

        lendersAuthorizerRefName =
            PersonAuthorityClientUtils.createPersonRefName(authRefName, "Art Lendersauthorizor", true);
        personIdsCreated.add(createPerson("Art", "Lendersauthorizor", lendersAuthorizerRefName));

        lendersContactRefName =
            PersonAuthorityClientUtils.createPersonRefName(authRefName, "Larry Lenderscontact", true);
        personIdsCreated.add(createPerson("Larry", "Lenderscontact", lendersContactRefName));
    }
    
    protected String createPerson(String firstName, String surName, String refName ) {
        PersonAuthorityClient personAuthClient = new PersonAuthorityClient();
        Map<String, String> personInfo = new HashMap<String,String>();
        personInfo.put(PersonJAXBSchema.FORE_NAME, firstName);
        personInfo.put(PersonJAXBSchema.SUR_NAME, surName);
    	MultipartOutput multipart = 
    		PersonAuthorityClientUtils.createPersonInstance(personAuthCSID, 
    				refName, personInfo, personAuthClient.getItemCommonPartName());
        ClientResponse<Response> res = personAuthClient.createItem(personAuthCSID, multipart);
        int statusCode = res.getStatus();

        Assert.assertTrue(REQUEST_TYPE.isValidStatusCode(statusCode),
                invalidStatusCodeMessage(REQUEST_TYPE, statusCode));
        Assert.assertEquals(statusCode, CREATED_STATUS);
    	return extractId(res);
    }

    // Success outcomes
    @Test(dataProvider="testName", dataProviderClass=AbstractServiceTestImpl.class,
        dependsOnMethods = {"createWithAuthRefs"})
    public void readAndCheckAuthRefs(String testName) throws Exception {

        // Perform setup.
        testSetup(OK_STATUS, ServiceRequestType.READ,testName);

        // Submit the request to the service and store the response.
        LoanoutClient loanoutClient = new LoanoutClient();
        ClientResponse<MultipartInput> res = loanoutClient.read(knownResourceId);
        int statusCode = res.getStatus();

        // Check the status code of the response: does it match
        // the expected response(s)?
        if(logger.isDebugEnabled()){
            logger.debug(testName + ".read: status = " + statusCode);
        }
        Assert.assertTrue(REQUEST_TYPE.isValidStatusCode(statusCode),
            invalidStatusCodeMessage(REQUEST_TYPE, statusCode));
        Assert.assertEquals(statusCode, EXPECTED_STATUS_CODE);

        MultipartInput input = (MultipartInput) res.getEntity();
        LoansoutCommon loanout = (LoansoutCommon) extractPart(input,
            loanoutClient.getCommonPartName(), LoansoutCommon.class);
        Assert.assertNotNull(loanout);
        if(logger.isDebugEnabled()){
            logger.debug(objectAsXmlString(loanout, LoansoutCommon.class));
        }
        // Check a couple of fields
        // FIXME
        Assert.assertEquals(loanout.getLendersAuthorizer(), lendersAuthorizerRefName);
        Assert.assertEquals(loanout.getLendersContact(), lendersContactRefName);
        
        // Get the auth refs and check them
        ClientResponse<AuthorityRefList> res2 =
           loanoutClient.getAuthorityRefs(knownResourceId);
        statusCode = res2.getStatus();

        if(logger.isDebugEnabled()){
            logger.debug(testName + ".getAuthorityRefs: status = " + statusCode);
        }
        Assert.assertTrue(REQUEST_TYPE.isValidStatusCode(statusCode),
                invalidStatusCodeMessage(REQUEST_TYPE, statusCode));
        Assert.assertEquals(statusCode, EXPECTED_STATUS_CODE);
        AuthorityRefList list = res2.getEntity();

        // Optionally output additional data about list members for debugging.
        boolean iterateThroughList = true;
        if(iterateThroughList && logger.isDebugEnabled()){
            List<AuthorityRefList.AuthorityRefItem> items =
                    list.getAuthorityRefItem();
            int i = 0;
            for(AuthorityRefList.AuthorityRefItem item : items){
                logger.debug(testName + ": list-item[" + i + "] Field:" +
                		item.getSourceField() + "= " +
                        item.getAuthDisplayName() +
                        item.getItemDisplayName());
                logger.debug(testName + ": list-item[" + i + "] refName=" +
                        item.getRefName());
                logger.debug(testName + ": list-item[" + i + "] URI=" +
                        item.getUri());
                i++;
            }
            Assert.assertEquals(i, NUM_AUTH_REFS_EXPECTED, "Did not find all authrefs!");
        }
    }


    // ---------------------------------------------------------------
    // Cleanup of resources created during testing
    // ---------------------------------------------------------------

    /**
     * Deletes all resources created by tests, after all tests have been run.
     *
     * This cleanup method will always be run, even if one or more tests fail.
     * For this reason, it attempts to remove all resources created
     * at any point during testing, even if some of those resources
     * may be expected to be deleted by certain tests.
     */
    @AfterClass(alwaysRun=true)
    public void cleanUp() {
        if (logger.isDebugEnabled()) {
            logger.debug("Cleaning up temporary resources created for testing ...");
        }
        // Note: Any non-success responses are ignored and not reported.

        PersonAuthorityClient personAuthClient = new PersonAuthorityClient();
        // Delete Person resource(s) (before PersonAuthority resources).
        ClientResponse<Response> res;
        for (String resourceId : personIdsCreated) {
            res = personAuthClient.deleteItem(personAuthCSID, resourceId);
        }
        // Delete PersonAuthority resource(s).
        res = personAuthClient.delete(personAuthCSID);
        // Delete Loans In resource(s).
        LoanoutClient loanoutClient = new LoanoutClient();
        for (String resourceId : loanoutIdsCreated) {
            res = loanoutClient.delete(resourceId);
        }
    }

    // ---------------------------------------------------------------
    // Utility methods used by tests above
    // ---------------------------------------------------------------
    @Override
    public String getServicePathComponent() {
        return SERVICE_PATH_COMPONENT;
    }

   private MultipartOutput createLoanoutInstance(String loanoutNumber,
    		String returnDate,
                String borrowersContact,
                String lendersAuthorizer,
                String lendersContact) {
        LoansoutCommon loanout = new LoansoutCommon();
        loanout.setLoanOutNumber(loanoutNumber);
        loanout.setLoanReturnDate(returnDate);
        loanout.setBorrowersContact(lendersContact);
        loanout.setLendersAuthorizer(lendersAuthorizer);
        loanout.setLendersContact(lendersContact);
        MultipartOutput multipart = new MultipartOutput();
        OutputPart commonPart =
            multipart.addPart(loanout, MediaType.APPLICATION_XML_TYPE);
        commonPart.getHeaders().add("label", new LoanoutClient().getCommonPartName());

        if(logger.isDebugEnabled()){
            logger.debug("to be created, loanout common");
            logger.debug(objectAsXmlString(loanout, LoansoutCommon.class));
        }

        return multipart;
    }
}