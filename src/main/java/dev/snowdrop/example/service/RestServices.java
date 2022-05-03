/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.snowdrop.example.service;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.ApplicationScope;

@Path("/")
@ApplicationScope
@Component
@Produces(MediaType.TEXT_PLAIN)
public class RestServices {

    private static final Log LOG = LogFactory.getLog(RestServices.class);
    private Response.Status currentStatus = Response.Status.OK;
    private String currentMessage = "correct";
    private int delayTime = 0;
    private int counter = 0;

    @Value("${serviceDefinition.i-am:Property \"serviceDefinition.i-am\" not set\" }")
    private String serviceDefinitionIAm;

    @Value("${serviceDefinition.host-to-consume:}")
    private String serviceDefinitionHostPortToConsume; //this can be empty, it would mean that this services does not consume another

    private final String URI_GENERIC_TEMPLATE = "/api/business";

    @GET
    @Path("/behave")
    public String behave() {
        currentStatus = Response.Status.OK;
        currentMessage = "correct";
        return "behaving correctly now!";
    }

    @GET
    @Path("/misbehave")
    public String misbehave() {
        currentStatus = Response.Status.SERVICE_UNAVAILABLE;
        currentMessage = "not-correct";
        return "Misbehaving badly now!";
    }

    @GET
    @Path("/delay")
    public String delay(@QueryParam("timeInSeconds") @DefaultValue("5") Integer timeInSeconds) {
        LOG.info("Delay time configured: Waiting "+timeInSeconds+" seconds");
        this.delayTime = timeInSeconds;
        return "Added "+ timeInSeconds +" seconds";
    }

    @GET
    @Path("/business")
    public Response business() {
        counter++;
        if (delayTime > 0){
            try {
                LOG.info("Delay time configured: Waiting "+delayTime+" seconds");
                Thread.sleep(delayTime * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String responseBody = this.getBusinessMessage();   
        if (!"".equals(this.serviceDefinitionHostPortToConsume)){
            LOG.info("Consuming other service at: \""+this.getServiceEndpoint()+"\"");
            RestTemplate restTemplate = new RestTemplate();
            try {
                String serviceResponse = restTemplate.getForObject(getServiceEndpoint(), String.class);
                LOG.info("service \""+this.getServiceEndpoint()+"\" responded: "+ serviceResponse);
                responseBody = this.getBusinessMessage(serviceResponse);                 
            
            }catch (HttpStatusCodeException e) {
                responseBody = this.getBusinessMessage("["+e.getRawStatusCode()+"] "+e.getResponseBodyAsString());  
            }catch (Exception e) {
                responseBody = this.getBusinessMessage("[unknwon error: "+e.getMessage()+
                "]");  

            }
        }

        LOG.info("About to reply this \""+this.getBusinessMessage()+"\"");
        return Response.status(this.currentStatus).entity(responseBody).build();
    }

    private String getBusinessMessage(String ... serviceResponseBody){
        return  String.format(
            this.serviceDefinitionIAm+"{"+this.currentMessage + "(count: %s) %s}",
            counter, 
            serviceResponseBody.length != 0 ? " \n => "+ serviceResponseBody[0] : "");
    }

    private String getServiceEndpoint(){
        return String.format("http://%s%s",this.serviceDefinitionHostPortToConsume,URI_GENERIC_TEMPLATE );
    }

}
