
package org.ejbca.core.protocol.ws.client.gen;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6
 * Generated source version: 2.1
 * 
 */
@WebFault(name = "UnknownProfileTypeException", targetNamespace = "http://ws.protocol.core.ejbca.org/")
public class UnknownProfileTypeException_Exception
    extends Exception
{

    private static final long serialVersionUID = 1L;
    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private UnknownProfileTypeException faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public UnknownProfileTypeException_Exception(String message, UnknownProfileTypeException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public UnknownProfileTypeException_Exception(String message, UnknownProfileTypeException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: org.ejbca.core.protocol.ws.client.gen.UnknownProfileTypeException
     */
    public UnknownProfileTypeException getFaultInfo() {
        return faultInfo;
    }

}
