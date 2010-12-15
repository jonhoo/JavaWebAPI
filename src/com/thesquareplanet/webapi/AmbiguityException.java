package com.thesquareplanet.webapi;
import org.w3c.dom.NodeList;


@SuppressWarnings ( "serial" )
public class AmbiguityException extends Exception {

    public AmbiguityException ( NodeList matches, String xpath ) {
        super ( "Could not find explicit matching element in group of " + matches.getLength() + " from expression " + xpath );
    }
}
