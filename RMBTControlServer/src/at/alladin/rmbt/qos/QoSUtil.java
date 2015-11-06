/*******************************************************************************
 * Copyright 2013-2015 alladin-IT GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package at.alladin.rmbt.qos;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import at.alladin.rmbt.controlServer.ErrorList;
import at.alladin.rmbt.db.Client;
import at.alladin.rmbt.db.QoSTestResult;
import at.alladin.rmbt.db.QoSTestResult.TestType;
import at.alladin.rmbt.db.QoSTestTypeDesc;
import at.alladin.rmbt.db.Test;
import at.alladin.rmbt.db.dao.QoSTestDescDao;
import at.alladin.rmbt.db.dao.QoSTestResultDao;
import at.alladin.rmbt.db.dao.QoSTestTypeDescDao;
import at.alladin.rmbt.qos.testscript.TestScriptInterpreter;
import at.alladin.rmbt.shared.hstoreparser.Hstore;
import at.alladin.rmbt.shared.hstoreparser.HstoreParseException;

/**
 * 
 * @author lb
 *
 */
public class QoSUtil {
    public static final Hstore HSTORE_PARSER = new Hstore(HttpProxyResult.class, NonTransparentProxyResult.class, 
    		DnsResult.class, TcpResult.class, UdpResult.class, WebsiteResult.class);


	/**
	 * 
	 * @author lb
	 *
	 */
	public static class TestUuid {
		public static enum UuidType {
			TEST_UUID, OPEN_TEST_UUID
		}
		
		protected UuidType type;
		protected String uuid;
		
		public TestUuid(String uuid, UuidType type) {
			this.type = type;
			this.uuid = uuid;
		}

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public UuidType getType() {
			return type;
		}

		public void setType(UuidType type) {
			this.type = type;
		}
	}
	
	/**
	 * 
	 * @param settings
	 * @param conn
	 * @param answer
	 * @param lang
	 * @param errorList
	 * @throws SQLException 
	 * @throws JSONException 
	 * @throws HstoreParseException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static void evaluate(final ResourceBundle settings, final Connection conn, final TestUuid uuid,
			final JSONObject answer, String lang, final ErrorList errorList) throws SQLException, HstoreParseException, JSONException, IllegalArgumentException, IllegalAccessException {
        // Load Language Files for Client
        
        final List<String> langs = Arrays.asList(settings.getString("RMBT_SUPPORTED_LANGUAGES").split(",\\s*"));
        
        if (langs.contains(lang)) {
            errorList.setLanguage(lang);
        }
        else {
            lang = settings.getString("RMBT_DEFAULT_LANGUAGE");
        }
        
        
        if (conn != null)
        {
        	
            final Client client = new Client(conn);
            final Test test = new Test(conn);
            
            boolean necessaryDataAvailable = false;
            
            if (uuid != null && uuid.getType() != null && uuid.getUuid() != null) {
            	switch (uuid.getType()) {
            	case OPEN_TEST_UUID:
            		if (test.getTestByOpenTestUuid(UUID.fromString(uuid.getUuid())) > 0
            				&& client.getClientByUid(test.getField("client_id").intValue())) {
            			necessaryDataAvailable = true;
            		}
            		break;
            	case TEST_UUID:
            		if (test.getTestByUuid(UUID.fromString(uuid.getUuid())) > 0
            				&& client.getClientByUid(test.getField("client_id").intValue())) {
            			necessaryDataAvailable = true;
            		}
            		break;
            	}
            }
            
            if (necessaryDataAvailable)
            {
                                      
                final Locale locale = new Locale(lang);
                final ResultOptions resultOptions = new ResultOptions(locale);
                final JSONArray resultList = new JSONArray();
                
                QoSTestResultDao resultDao = new QoSTestResultDao(conn);
                List<QoSTestResult> testResultList = resultDao.getByTestUid(test.getUid());
                if (testResultList == null || testResultList.isEmpty()) {
                	throw new UnsupportedOperationException();
                }
                //map that contains all test types and their result descriptions determined by the test result <-> test objectives comparison
            	Map<TestType,TreeSet<ResultDesc>> resultKeys = new HashMap<>();
            	
            	//test description set:
            	Set<String> testDescSet = new TreeSet<>();
            	//test summary set:
            	Set<String> testSummarySet = new TreeSet<>();
            	
                //iterate through all result entries
                for (final QoSTestResult testResult : testResultList) {
                	
                	//reset test counters
                	testResult.setFailureCounter(0);
                	testResult.setSuccessCounter(0);
                	
                	//get the correct class of the result;
                	TestType testType = null;
                	try {
                		testType = TestType.valueOf(testResult.getTestType().toUpperCase(Locale.US));
                	}
                	catch(IllegalArgumentException e) {
                		final String errorMessage = "WARNING: QoS TestType '" + testResult.getTestType().toUpperCase(Locale.US) + "' not supported by ControlServer. Test with UID: " + testResult.getUid() + " skipped.";
                		System.out.println(errorMessage);
                		errorList.addErrorString(errorMessage);
                		testType = null;
                	}
                	
                	if (testType == null) {
                		continue;
                	}
                	
                	Class<? extends AbstractResult<?>> clazz = testType.getClazz();
                	//parse hstore data
                	AbstractResult<?> result = QoSUtil.HSTORE_PARSER.fromJSON(new JSONObject(testResult.getResults()), clazz);
                	if (result != null) {
                		//add each test description key to the testDescSet (to fetch it later from the db)
                		if (testResult.getTestDescription() != null) {
                    		testDescSet.add(testResult.getTestDescription());	
                		}
                		if (testResult.getTestSummary() != null) {
                			testSummarySet.add(testResult.getTestSummary());
                		}
                		testResult.setResult(result);

                	}
                	//if expected resuls not null, compare them to the test results
                	if (testResult.getExpectedResults()!=null) {
                		
                		//compare the test results with all expected results: 
                		for (String expectedResults : testResult.getExpectedResults()) {
                			//parse hstore string to object
                			AbstractResult<?> expResult = QoSUtil.HSTORE_PARSER.fromString(expectedResults, clazz);
                			//compare expected result to test result and save the returned id
                			ResultDesc resultDesc = ResultComparer.compare(result, expResult, QoSUtil.HSTORE_PARSER, resultOptions);
                			if (resultDesc != null) {
                    			resultDesc.addTestResultUid(testResult.getUid());
                    			resultDesc.setTestType(testType);
                    			TreeSet<ResultDesc> resultDescSet;
                    			if (resultKeys.containsKey(testType)) {
                    				resultDescSet = resultKeys.get(testType);
                    			}
                    			else {
                    				resultDescSet = new TreeSet<>();
                    				resultKeys.put(testType, resultDescSet);
                    			}
                    			resultDescSet.add(resultDesc);
                    			                        			
                    			//increase the failure or success counter of this result object
                    			if (resultDesc.getStatusCode().equals(ResultDesc.STATUS_CODE_SUCCESS)) {
                    				if (expResult.getOnSuccess() != null) {
                    					testResult.setSuccessCounter(testResult.getSuccessCounter()+1);
                    				}
                    			}
                    			else {
                    				if (expResult.getOnFailure() != null) {
                    					testResult.setFailureCounter(testResult.getFailureCounter()+1);
                    				}
                    			}
                			}
                		}
                	}
                	//resultList.put(testResult.toJson());
                	
                    //save all test results after the success and failure counters have been set
                	//resultDao.updateCounter(testResult);
                }
                                       
                //-------------------------------------------------------------
                //fetch all result strings from the db
                QoSTestDescDao descDao = new QoSTestDescDao(conn, locale);

                //FIRST: get all test descriptions
                Set<String> testDescToFetchSet = testDescSet;
                testDescToFetchSet.addAll(testSummarySet);
                
                Map<String, String> testDescMap = descDao.getAllByKeyToMap(testDescToFetchSet);
                
                for (QoSTestResult testResult : testResultList) {
                	
                    //and set the test results + put each one to the result list json array
                	String preParsedDesc = testDescMap.get(testResult.getTestDescription());
                	if (preParsedDesc != null) {
                    	String description = String.valueOf(TestScriptInterpreter.interprete(testDescMap.get(testResult.getTestDescription()), 
                    			QoSUtil.HSTORE_PARSER, testResult.getResult(), true, resultOptions));
                    	testResult.setTestDescription(description);
                	}

                	//do the same for the test summary:
                	String preParsedSummary = testDescMap.get(testResult.getTestSummary());
                	if (preParsedSummary != null) {
                    	String description = String.valueOf(TestScriptInterpreter.interprete(testDescMap.get(testResult.getTestSummary()), 
                    			QoSUtil.HSTORE_PARSER, testResult.getResult(), true, resultOptions));
                    	testResult.setTestSummary(description);
                	}

                	resultList.put(testResult.toJson(uuid.getType()));
                }
                
                //finally put results to json
                answer.put("testresultdetail", resultList);
                
                JSONArray resultDescArray = new JSONArray();
                
                //SECOND: fetch all test result descriptions 
                for (TestType testType : resultKeys.keySet()) {
                	TreeSet<ResultDesc> descSet = resultKeys.get(testType);
                	//fetch results to same object
                    descDao.loadToTestDesc(descSet);

                    //another tree set for duplicate entries:
                    //TODO: there must be a better solution 
                    //(the issue is: compareTo() method returns differnt values depending on the .value attribute (if it's set or not))
                    TreeSet<ResultDesc> descSetNew = new TreeSet<>();
                    //add fetched results to json
                                        
                    for (ResultDesc desc : descSet) {
                    	if (!descSetNew.contains(desc)) {
                        	descSetNew.add(desc);
                    	}
                    	else {
                    		for (ResultDesc d : descSetNew) {
                    			if (d.compareTo(desc) == 0) {
                    				d.getTestResultUidList().addAll(desc.getTestResultUidList());
                    			}
                    		}
                    	}
                    }
                    
                    for (ResultDesc desc : descSetNew) {
                    	if (desc.getValue() != null) {
                            resultDescArray.put(desc.toJson());	
                    	}	
                    }
                    
                }
                //System.out.println(resultDescArray);
                //put result descriptions to json
                answer.put("testresultdetail_desc", resultDescArray);

                QoSTestTypeDescDao testTypeDao = new QoSTestTypeDescDao(conn, locale);
                JSONArray testTypeDescArray = new JSONArray();
                for (QoSTestTypeDesc desc : testTypeDao.getAll()) {
                	final JSONObject testTypeDesc = desc.toJson();
                	if (testTypeDesc != null) {
                		testTypeDescArray.put(testTypeDesc);
                	}
                }

                //put result descriptions to json
                answer.put("testresultdetail_testdesc", testTypeDescArray);

                //System.out.println(answer);
            }
            else
                errorList.addError("ERROR_REQUEST_TEST_RESULT_DETAIL_NO_UUID");
            
        }
        else
            errorList.addError("ERROR_DB_CONNECTION");
	}
}
