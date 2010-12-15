package com.thesquareplanet.webapi;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A class for extracting information about HTML Forms
 */
class RemoteForm {
    public enum METHOD_TYPE {
        GET,
        POST
    };

    /**
     * @var Form action (URL)
     */
    private URI action = null;

    /**
     * @var Form method (GET/POST)
     */
    private METHOD_TYPE method = METHOD_TYPE.GET;

    /**
     * @var This form
     */
    private Element form = null;

    /**
     * @var XPath context
     */
    private XPath navigator = null;

    /**
     * @var All the attributes that have been loaded from this form or set by the script
     */
    private Map<String, String> attributes = null;

    /**
     * Constructor
     * 
     * @param The form to extract data from
     * @throws RemoteFormException If the remote form contains invalid markup (e.g. invalid URI as action)
     */
    public RemoteForm ( Element form ) throws RemoteFormException {
        this.form = form;
        this.attributes = new HashMap<String, String> ( );

        XPathFactory factory = XPathFactory.newInstance ( );
        this.navigator = factory.newXPath ( );

        try {
            this.action = new URI ( "" );

            // If we have a non-empty action attribute, we set the action to the value of the action attribute
            if ( !this.form.getAttribute ( "action" ).isEmpty ( ) ) {
                this.action = new URI ( this.form.getAttribute ( "action" ) );
            }
        } catch ( URISyntaxException e ) {
            throw new RemoteFormException ( "Invalid URI in action field of form" );
        }

        // If the method attribute contains a legal value, we set that as well
        if ( this.form.getAttribute ( "method" ).toLowerCase ( ) == "post" ) {
            this.method = METHOD_TYPE.POST;
        }

        // And finally, we try to find all the fields in the current form
        this.discoverParameters ( );
    }

    /**
     * Discovers form fields in this form
     * 
     * Extracts all form fields that are selected/checked by default,
     * or are text-input fields ( hidden|password|text|textarea )
     * Currently supprts:
     * - Select
     * - Input [button,submit,text,hidden,checkbox,radio,password]
     * - Textarea
     */
    private void discoverParameters ( ) {
        try {
            XPathExpression xpath = this.navigator.compile ( "//input | //select | //textarea" );
            NodeList fields = (NodeList) xpath.evaluate ( this.form, XPathConstants.NODESET );

            /**
             * Loops through all known form fields
             */
            for ( int i = 0; i < fields.getLength ( ); i++ ) {
                Element field = (Element) fields.item ( i );
                String fieldType = field.getTagName ( ).toLowerCase ( );

                if ( fieldType.equals ( "input" ) ) {
                    String inputType = field.getAttribute ( "type" );

                    if ( inputType.equals ( "submit" ) ) {
                    } else if ( inputType.equals ( "button" ) ) {
                    } else if ( inputType.equals ( "text" ) || inputType.equals ( "password" ) || inputType.equals ( "hidden" ) ) {
                        this.setAttributeByString ( field.getAttribute ( "name" ), field.getAttribute ( "value" ) );
                    } else if ( inputType.equals ( "checkbox" ) || inputType.equals ( "radio" ) ) {
                        if ( !field.getAttribute ( "checked" ).trim ( ).isEmpty ( ) ) {
                            this.setAttributeByString ( field.getAttribute ( "name" ), field.getAttribute ( "value" ) );
                        }
                    }
                } else if ( fieldType.equals ( "select" ) ) {
                    NodeList options;

                    options = (NodeList) this.navigator.compile ( "//option[@selected != \"\"]" ).evaluate ( this.form, XPathConstants.NODESET );

                    // In a select, loops through all options, and sets the attribute named by the select if the option
                    // is
                    // selected by default
                    for ( int j = 0; j < options.getLength ( ); j++ ) {
                        Element option = (Element) options.item ( j );
                        this.setAttributeByString ( option.getAttribute ( "name" ), option.hasAttribute ( "value" ) ? option.getAttribute ( "value" ) : option.getNodeValue ( ) );
                    }
                } else if ( fieldType.equals ( "textarea" ) ) {
                    // Textareas should always be set, even if empty
                    this.setAttributeByString ( field.getAttribute ( "name" ), field.getNodeValue ( ) );
                }
            }
        } catch ( XPathExpressionException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace ( );
        }
    }

    public RemoteForm setAttributeByName ( String fieldName, String value ) {
        return this.setAttributeByString ( fieldName, value );
    }

    private RemoteForm setAttributeByString ( String fieldName, String fieldValue ) {
        fieldName = fieldName.trim ( );
        this.attributes.put ( fieldName, fieldValue );

        // Chain
        return this;
    }

    /**
     * Gets all attributes
     */
    public Map<String, String> getParameters ( ) {
        return this.attributes;
    }

    /**
     * Gets this form"s action
     */
    public URI getAction ( ) {
        return this.action;
    }

    /**
     * Gets this form's method
     */
    public METHOD_TYPE getMethod ( ) {
        return this.method;
    }

    public Element getForm ( ) {
        return this.form;
    }
}