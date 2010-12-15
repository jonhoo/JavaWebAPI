package com.thesquareplanet.webapi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class that emulates a regular HTTP Web client, and allows scripts to execute
 * common user actions like clicking links and submitting forms
 * 
 * @author Jon Gjengset
 */
class WebAPI {
    protected HttpClient connection = null;
    protected Document currentDocument = null;
    protected XPath navigator = null;
    protected URI currentURI = null;

    /**
     * Constructor for the browsers
     * 
     * @param The HttpClient implementation to use
     */
    public WebAPI ( HttpClient client ) {
        this.connection = client;

        try {
            this.currentURI = new URI ( "" );
        } catch ( URISyntaxException e ) {
        }

        XPathFactory factory = XPathFactory.newInstance ( );
        this.navigator = factory.newXPath ( );
    }

    /**
     * Navigates to the given URI
     * 
     * @param The URI to navigate to, may be relative
     * @return The current object for chaining
     * @throws ClientProtocolException If a HTTP protocol error occurs
     * @throws ParserConfigurationException Thrown if a DOM Document Builder cannot be created
     * @throws SAXException If the HTML in the response cannot be parsed
     * @throws IOException If an IO error occurs when talking to the HTTP server
     */
    public WebAPI navigate ( URI uri ) throws ClientProtocolException, IOException, ParserConfigurationException, SAXException {
        /**
         * Resolve the URI
         */
        URI nextURI = this.currentURI.resolve ( uri );

        /**
         * Finally, fetch the URL, and handle the response
         */
        this.handleResponse ( this.connection.execute ( new HttpGet ( nextURI ) ), nextURI );

        /**
         * And make us chainable
         */
        return this;
    }

    /**
     * Navigates to the given URI
     * 
     * @param uri The URI to navigate to
     * @return The current object for chaining
     * 
     * @throws URISyntaxException If the URI is not valid
     * @throws ClientProtocolException If a HTTP protocol error occurs
     * @throws ParserConfigurationException Thrown if a DOM Document Builder cannot be created
     * @throws SAXException If the HTML in the response cannot be parsed
     * @throws IOException If an IO error occurs when talking to the HTTP server
     */
    public WebAPI navigate ( String uri ) throws ClientProtocolException, IOException, ParserConfigurationException, SAXException, URISyntaxException {
        return this.navigate ( new URI ( uri ) );
    }

    /**
     * Handles the HTTP response after navigating
     * 
     * @param response The HTTP response from the new URI
     * 
     * @throws ParserConfigurationException Thrown if a DOM Document Builder cannot be created
     * @throws SAXException If the HTML in the response cannot be parsed
     * @throws IOException If an IO error occurs when talking to the HTTP server
     */
    private void handleResponse ( HttpResponse response, URI nextURI ) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance ( );
        domFactory.setNamespaceAware ( true ); // never forget this!

        this.currentDocument = domFactory.newDocumentBuilder ( ).parse ( response.getEntity ( ).getContent ( ) );

        this.currentURI = nextURI;
    }

    /**
     * Navigates to the page pointed to by the href attribute of the element with the given label
     * 
     * @param Link label
     * @return The current object for chaining
     * 
     * @throws XPathExpressionException If the given label interferes with the XPath expression compiler
     * @throws AmbiguityException If the element to be clicked could not be unequivocally identified from the expression
     * @throws URISyntaxException If the URI is not valid
     * @throws ClientProtocolException If a HTTP protocol error occurs
     * @throws ParserConfigurationException Thrown if a DOM Document Builder cannot be created
     * @throws SAXException If the HTML in the response cannot be parsed
     * @throws IOException If an IO error occurs when talking to the HTTP server
     * @throws NoTargetException If the target element has no href attribute
     */
    public WebAPI click ( String label ) throws ClientProtocolException, XPathExpressionException, AmbiguityException, IOException, URISyntaxException, ParserConfigurationException, SAXException, NoTargetException {
        return this.clickXPath ( ""
                + "//a[text() = \"" + label.replace ( "\"", "\\\"" ) + "\"] | "
                + "//input[@type = \"submit\"][@value = \"" + label.replace ( "\"", "\\\"" ) + "\"]" );
    }

    /**
     * Navigates to the page pointed to by the href attribute of the element matched by the given XPath expression
     * 
     * @param expression XPath expression indicating the element to click
     * @return The current object for chaining
     * 
     * @throws URISyntaxException If the URI is not valid
     * @throws ClientProtocolException If a HTTP protocol error occurs
     * @throws ParserConfigurationException Thrown if a DOM Document Builder cannot be created
     * @throws SAXException If the HTML in the response cannot be parsed
     * @throws IOException If an IO error occurs when talking to the HTTP server
     * @throws AmbiguityException If the element to be clicked could not be unequivocally identified from the expression
     * @throws XPathExpressionException If the given expression does not compile
     * @throws NoTargetException If the target element has no href attribute
     */
    public WebAPI clickXPath ( String expression ) throws ClientProtocolException, XPathExpressionException, AmbiguityException, IOException, URISyntaxException, ParserConfigurationException, SAXException, NoTargetException {
        this.click ( this.findElement ( expression ) );
        return this;
    }

    /**
     * Interacts with the given element and navigates to the inferred next page
     * 
     * The href attribute of the matched element will be navigated to.
     * 
     * @param n The node to click
     * 
     * @throws NoTargetException If the indicated element has no href attribute
     * @throws URISyntaxException If the URI is not valid
     * @throws ClientProtocolException If a HTTP protocol error occurs
     * @throws ParserConfigurationException Thrown if a DOM Document Builder cannot be created
     * @throws SAXException If the HTML in the response cannot be parsed
     * @throws IOException If an IO error occurs when talking to the HTTP server
     * @throws RemoteFormException If the remote form contains invalid data (e.g. invalid URI as action)
     */
    private void click ( Element e ) throws NoTargetException, ClientProtocolException, IOException, URISyntaxException, ParserConfigurationException, SAXException {
        if ( !e.hasAttribute ( "href" ) ) {
            throw new NoTargetException ( e );
        }

        this.navigate ( e.getAttribute ( "href" ) );
    }

    /**
     * Downloads the target of the given link, image or other linking element
     * 
     * This method will search for the given matching criteria (XPath), and
     * download whatever is in the href or src attribute of the given element
     * 
     * @param expression XPath to element
     * @param saveAs Where to download the target to
     * @return The current object for chaining
     * 
     * @throws XPathExpressionException If the given expression does not compile
     * @throws NoTargetException If no href or src attribute is found on the indicated DOM element
     * @throws AmbiguityException If the target element could not be unequivocally identified from the expression
     * @throws IOException If an IO error occurs when talking to the HTTP server
     * @throws ClientProtocolException If a HTTP protocol error occurs
     */
    public WebAPI download ( String expression, File saveAs ) throws NoTargetException, XPathExpressionException, AmbiguityException, ClientProtocolException, IOException {
        Element element = this.findElement ( expression );

        // If we don"t have a linking attribute, we fail
        if ( !element.hasAttribute ( "src" ) && !element.hasAttribute ( "href" ) ) {
            throw new NoTargetException ( element );
        }

        // Resolve the linked resource
        URI url = this.currentURI.resolve ( element.hasAttribute ( "src" ) ? element.getAttribute ( "src" ) : element.getAttribute ( "href" ) );

        HttpResponse response = this.connection.execute ( new HttpGet ( url ) );
        InputStream in = response.getEntity ( ).getContent ( );
        OutputStream out = new FileOutputStream ( saveAs );

        // Write byte by byte
        byte[] buf = new byte[1024];
        int len;
        while ( ( len = in.read ( buf ) ) > 0 ) {
            out.write ( buf, 0, len );
        }
        in.close ( );
        out.close ( );

        // Chain!
        return this;
    }

    /**
     * Returns the element found by applying the given XPath expression to the current document
     * 
     * @param expression XPath expression to identify the node
     * @return The DOM Element found
     * 
     * @throws XPathExpressionException If the expression does not compile
     * @throws AmbiguityException If the element could not be unequivocally identified from the expression
     */
    private Element findElement ( String expression ) throws XPathExpressionException, AmbiguityException {
        XPathExpression xpath = this.navigator.compile ( expression );
        NodeList matches = (NodeList) xpath.evaluate ( this.currentDocument, XPathConstants.NODESET );

        if ( matches.getLength ( ) != 1 )
            throw new AmbiguityException ( matches, expression );

        // Fetch the element
        Node n = matches.item ( 0 );

        if ( !( n instanceof Element ) )
            throw new ClassCastException ( "Your search expression " + expression + " returned a DOM Node that was not a DOM Element!" );

        return (Element) n;
    }

    /**
     * Returns a RemoteForm for the given form
     * 
     * @param The DOM Element representing the form
     * @return The matched form
     * 
     * @throws RemoteFormException If the remote form contains invalid data (e.g. invalid URI as action)
     */
    public RemoteForm getForm ( Element form ) throws RemoteFormException {
        return new RemoteForm ( form );
    }

    /**
     * Returns a RemoteForm for the given form
     * 
     * @param expression XPath expression to locate the form
     * @return The form found by the XPath expression
     * 
     * @throws AmbiguityException If the form could not be unequivocally identified from the expression
     * @throws XPathExpressionException If the XPath expression is invalid
     * @throws RemoteFormException If the remote form contains invalid data (e.g. invalid URI as action)
     */
    public RemoteForm getForm ( String expression ) throws AmbiguityException, XPathExpressionException, RemoteFormException {
        Element form = this.findElement ( expression );

        if ( !form.getTagName ( ).toLowerCase ( ).equals ( "form" ) )
            throw new IllegalArgumentException ( "Did not find form by evaluating expression " + expression );

        return this.getForm ( form );
    }

    /**
     * Submits the given form
     * 
     * @param form The form to submit
     * @return The current object for chaining
     * @throws IOException If an IO error occurs when talking to the HTTP server
     * @throws ClientProtocolException If a HTTP protocol error occurs
     * @throws ParserConfigurationException Thrown if a DOM Document Builder cannot be created
     * @throws SAXException If the HTML in the response cannot be parsed
     */
    public WebAPI submitForm ( RemoteForm form ) throws ClientProtocolException, ParserConfigurationException, SAXException, IOException {
        try {
            this.submitForm ( form, null );
        } catch ( XPathExpressionException e ) {
            // We're not using the submit button feature, so this won't happen
        } catch ( AmbiguityException e ) {
            // We're not using the submit button feature, so this won't happen
        }

        return this;
    }

    /**
     * Submits a form by "pressing" the submit button with the given name
     * 
     * @param form The form to submit
     * @param submitButtonName Name of the submit button to "press"
     * @return The current object for chaining
     * 
     * @throws XPathExpressionException If the given submit button name interferes with the XPath expression used to
     *         find the button
     * @throws AmbiguityException If the submit button could not be unequivocally identified from the given label
     * @throws IOException If an IO error occurs when talking to the HTTP server
     * @throws ClientProtocolException If a HTTP protocol error occurs
     * @throws ParserConfigurationException Thrown if a DOM Document Builder cannot be created
     * @throws SAXException If the HTML in the response cannot be parsed
     */
    public WebAPI submitFormByButton ( RemoteForm form, String submitButtonName ) throws XPathExpressionException, AmbiguityException, ClientProtocolException, ParserConfigurationException, SAXException, IOException {
        return this.submitForm ( form, "//input[@type=\"submit\"][@name=\"" + submitButtonName.replace ( "\"", "\\\"" ) + "\"]" );
    }

    /**
     * Submits the given form.
     * 
     * If submitButtonName is given, that name is also submitted as a POST/GET value
     * This is available since some forms act differently based on which submit button
     * you press
     * 
     * @param form The form to submit
     * @param submitButtonXPath The submit button to click
     * @return Returns this WebAPI object for chaining
     * 
     * @throws AmbiguityException If the submit button could not be unequivocally identified from the expression
     * @throws XPathExpressionException If the given submit button
     * @throws IOException If an IO error occurs when talking to the HTTP server
     * @throws ClientProtocolException If a HTTP protocol error occurs
     * @throws ParserConfigurationException Thrown if a DOM Document Builder cannot be created
     * @throws SAXException If the HTML in the response cannot be parsed
     */
    public WebAPI submitForm ( RemoteForm form, String submitButtonXPath ) throws XPathExpressionException, AmbiguityException, ClientProtocolException, ParserConfigurationException, SAXException, IOException {
        // Find the button, and set the given attribute if we"re pressing a button
        if ( submitButtonXPath != null ) {
            NodeList matches = (NodeList) this.navigator.compile ( submitButtonXPath ).evaluate ( form.getForm ( ), XPathConstants.NODESET );

            if ( matches.getLength ( ) != 1 )
                throw new AmbiguityException ( matches, submitButtonXPath );

            // Fetch the element
            Node n = matches.item ( 0 );

            if ( !( n instanceof Element ) )
                throw new ClassCastException ( "Your search expression " + submitButtonXPath + " returned a DOM Node that was not a DOM Element!" );

            Element button = (Element) n;
            form.setAttributeByName ( button.getAttribute ( "name" ), button.getAttribute ( "value" ) );
        }

        // Handle get/post
        URI nextURI = this.currentURI.resolve ( form.getAction ( ) );
        HttpUriRequest request = null;

        switch ( form.getMethod ( ) ) {
            case POST:
                /**
                 * If we're posting, we send the form values encoded in the body (entity)
                 * of the POST request.
                 * Note that the UrlEncodedFormEntity class requires the input to be as
                 * a List of NameValuePair, so we need to "convert" from our parameter map
                 */
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair> ( );
                for ( String param : form.getParameters ( ).keySet ( ) ) {
                    nameValuePairs.add ( new BasicNameValuePair ( param, form.getParameters ( ).get ( param ) ) );
                }

                request = new HttpPost ( nextURI );
                try {
                    ( (HttpPost) request ).setEntity ( new UrlEncodedFormEntity ( nameValuePairs ) );
                } catch ( UnsupportedEncodingException e ) {
                    e.printStackTrace ( );
                }
                break;
            case GET:
            default:
                /**
                 * If we're dealing with GET, we add all parameters in the current next URL
                 * to the parameters to send with the next request, and then add all the ones
                 * present in the form.
                 * We then construct a new URI based on all the values in the form.
                 */
                List<NameValuePair> oldParameters = URLEncodedUtils.parse ( nextURI, "UTF-8" );
                for ( NameValuePair param : oldParameters )
                    if ( !form.getParameters ( ).containsKey ( param.getName ( ) ) )
                        form.getParameters ( ).put ( param.getName ( ), param.getValue ( ) );

                String newQueryString = "?";
                for ( String param : form.getParameters ( ).keySet ( ) ) {
                    try {
                        if ( !newQueryString.equals ( "?" ) )
                            newQueryString += "&";
                        newQueryString += URLEncoder.encode ( param, "UTF-8" ) + "=" + URLEncoder.encode ( form.getParameters ( ).get ( param ), "UTF-8" );
                    } catch ( UnsupportedEncodingException e ) {
                        throw new RuntimeException ( "Broken VM does not support UTF-8" );
                    }
                }

                request = new HttpGet ( nextURI.resolve ( newQueryString ) );
        }

        this.handleResponse ( this.connection.execute ( request ), request.getURI ( ) );

        // Chain
        return this;
    }

    /**
     * Returns the source of the current page
     * 
     * @return The current HTML
     */
    public String getSource ( ) {
        return this.currentDocument.getTextContent ( );
    }
}