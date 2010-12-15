package com.thesquareplanet.webapi;
import org.w3c.dom.Element;

@SuppressWarnings ( "serial" )
public class NoTargetException extends Exception {
    public NoTargetException ( Element e ) {
        super ( "No href or src attribute found for element " + e );
    }
}
