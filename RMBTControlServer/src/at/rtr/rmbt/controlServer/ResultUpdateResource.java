/*******************************************************************************
 * Copyright 2013-2014 alladin-IT GmbH
 * Copyright 2013-2014 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
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
package at.rtr.rmbt.controlServer;

import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import at.rtr.rmbt.db.Client;
import at.rtr.rmbt.db.GeoLocation;
import at.rtr.rmbt.db.Test;
import at.rtr.rmbt.db.fields.DoubleField;
import at.rtr.rmbt.db.fields.IntField;

public class ResultUpdateResource extends ServerResource
{
	private final static String GEO_PROVIDER_MANUAL = "manual";
	private final static String GEO_PROVIDER_GEOCODER = "geocoder";
	
    @Post("json|txt")
    public String request(final String entity)
    {
        addAllowRestrictedOrigin();
        
        JSONObject request = null;
        
        final ErrorList errorList = new ErrorList();
        final JSONObject answer = new JSONObject();
        
        final String ip = getIP();
        
        System.out.println(MessageFormat.format(labels.getString("NEW_RESULT_UPDATE"), ip));
        
        if (entity != null && !entity.isEmpty())
        {
            try
            {
                request = new JSONObject(entity);
                //System.out.println(request.toString(4));
                /*
                Test result update from 192.168.1.2
                {
                    "test_uuid": "3d26f187-1777-48d0-89e4-2892c7d66926",
                    "geo_lat": 48.20845270000001,
                    "provider": "geocoder",
                    "accuracy": 100,
                    "uuid": "1db50fc4-1234-4931-8cda-3adf07912345",
                    "geo_long": 16.373108099999968
                }
                 */

                final UUID clientUUID = UUID.fromString(request.getString("uuid"));
                final UUID testUUID = UUID.fromString(request.getString("test_uuid"));
                final Boolean aborted = request.has("aborted") && request.getBoolean("aborted");

                final Test test = new Test(conn);
                if (test.getTestByUuid(testUUID) < 0)
                    throw new IllegalArgumentException("error while loading test");

                final UUID openTestUUID = UUID.fromString(test.getField("open_test_uuid").toString());

                final Client client = new Client(conn);
                final long clientId = client.getClientByUuid(clientUUID);
                if (clientId < 0)
                    throw new IllegalArgumentException("error while loading client");

                if (test.getField("client_id").longValue() != clientId)
                    throw new IllegalArgumentException("client UUID does not match test");

                //either do geolocation update or mark test as ABORTED
                //Test was aborted - mark test as "ABORTED" in the database
                if (aborted) {
                    test.getField("status").setString("ABORTED");
                }
                else {
                    System.out.println("updating geoloc");
                    //a manual geolocation was provided
                    final double geoLat = request.optDouble("geo_lat", Double.NaN);
                    final double geoLong = request.optDouble("geo_long", Double.NaN);
                    final float geoAccuracy = (float) request.optDouble("accuracy", 0);
                    final String provider = request.optString("provider").toLowerCase();


                    if (!Double.isNaN(geoLat) && !Double.isNaN(geoLong) &&
                            (provider.equals(GEO_PROVIDER_GEOCODER) || provider.equals(GEO_PROVIDER_MANUAL))) {
                        final GeoLocation geoloc = new GeoLocation(conn);
                        geoloc.setTest_id(test.getUid());

                        final Timestamp tstamp = java.sql.Timestamp.valueOf(new Timestamp(
                                (new Date().getTime())).toString());
                        geoloc.setTime(tstamp, TimeZone.getDefault().toString());
                        geoloc.setAccuracy(geoAccuracy);
                        geoloc.setGeo_lat(geoLat);
                        geoloc.setGeo_long(geoLong);
                        geoloc.setProvider(provider);
                        geoloc.setOpenTestUuid(openTestUUID);
                        // use start of test as time of location information (0 ns)
                        geoloc.setTime_ns(0);
                        final UUID geoRefUuid = geoloc.getGeoLocationUuid();
                        geoloc.storeLocation();

                        ((DoubleField) test.getField("geo_lat")).setValue(geoLat);
                        ((DoubleField) test.getField("geo_long")).setValue(geoLong);
                        ((DoubleField) test.getField("geo_accuracy")).setValue(geoAccuracy);
                        test.getField("geo_provider").setString(provider);
                        test.getField("geo_location_uuid").setString(geoRefUuid.toString());
                    }
                }

                test.storeTestResults(true);
                
                if (test.hasError())
                    errorList.addError(test.getError());
            }
            catch (final JSONException e)
            {
                errorList.addError("ERROR_REQUEST_JSON");
                System.out.println("Error parsing JSDON Data " + e.toString());
            }
            catch (final IllegalArgumentException e)
            {
                errorList.addError("ERROR_REQUEST_JSON");
                System.out.println("Error parsing JSDON Data " + e.toString());
            }
        }
        
        return answer.toString();
    }
    
    @Get("json")
    public String retrieve(final String entity)
    {
        return request(entity);
    }
    
}
