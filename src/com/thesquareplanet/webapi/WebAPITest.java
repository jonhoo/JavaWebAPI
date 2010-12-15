package com.thesquareplanet.webapi;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.xpath.XPathExpressionException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.junit.Test;

public class WebAPITest {
    private WebAPI web;

    @Before
    public void setUp ( ) {
        this.web = new WebAPI ( new DefaultHttpClient ( ) );
    }

    @Test
    public void navigate ( ) throws ClientProtocolException, IOException, URISyntaxException {
        this.web.navigate ( "http://www.wikipedia.org" );

        assert ( this.web.getSource ( ).contains ( "the free encyclopedia" ) );
    }

    @Test
    public void formSubmissionGet ( ) throws XPathExpressionException, ClientProtocolException, AmbiguityException, IOException, URISyntaxException, RemoteFormException {
        this.web.navigate ( "http://www.wikipedia.org" );
        RemoteForm search = this.web.getFormById ( "searchform" );
        search.setAttributeByName ( "search", "java" );
        this.web.submitForm ( search, "go" );

        assert ( this.web.getSource ( ).contains ( "This article is about the Indonesian island of Java" ) );
    }
    
    @Test
    public void formSubmissionPost ( ) throws XPathExpressionException, ClientProtocolException, AmbiguityException, IOException, URISyntaxException, RemoteFormException {
        this.web.navigate ( "http://www.php.net" );
        RemoteForm search = this.web.getFormById ( "topsearch" );
        search.setAttributeByName ( "pattern", "exit" );
        this.web.submitForm ( search );

        assert ( this.web.getSource ( ).contains ( "Output a message and terminate the current script" ) );
    }
}
