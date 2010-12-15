package com.thesquareplanet.webapi;


import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class WebAPITest {
    private WebAPI web;
    
    @Before
    public void setUp ( ) {
        this.web = new WebAPI ( new DefaultHttpClient() );
    }
    
    @Test
    public void fetchWikipedia () {
        try {
            this.web.navigate ( "http://www.wikipedia.org" );
        } catch ( ClientProtocolException e ) {
        } catch ( IOException e ) {
        } catch ( ParserConfigurationException e ) {
        } catch ( SAXException e ) {
        } catch ( URISyntaxException e ) {
        }
        
        assert ( this.web.getSource ( ).contains ( "the free encyclopedia" ) );
    }

    @Test
    public void searchWikipedia () {
        try {
            this.web.navigate ( "http://www.wikipedia.org" );
        } catch ( ClientProtocolException e ) {
        } catch ( IOException e ) {
        } catch ( ParserConfigurationException e ) {
        } catch ( SAXException e ) {
        } catch ( URISyntaxException e ) {
        }
        
        assert ( this.web.getSource ( ).contains ( "the free encyclopedia" ) );
    }
}
