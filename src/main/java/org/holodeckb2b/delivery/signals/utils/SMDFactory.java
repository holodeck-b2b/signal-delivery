/* 
* Copyright 2015 The Holodeck B2B Team, Sander Fieten
*  
* Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent
* versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the Licence.
* You may obtain a copy of the Licence at: https://joinup.ec.europa.eu/software/page/eupl
*  
* Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on 
* an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*
* See the Licence for the specific language governing permissions and limitations under the Licence.
*/ 
package org.holodeckb2b.delivery.signals.utils;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.holodeckb2b.delivery.signals.smd.Description;
import org.holodeckb2b.delivery.signals.smd.Error;
import org.holodeckb2b.delivery.signals.smd.MessageInfo;
import org.holodeckb2b.delivery.signals.smd.Receipt;
import org.holodeckb2b.delivery.signals.smd.SignalMessage;
import org.holodeckb2b.interfaces.general.IDescription;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Is a factory class for creating {@link SignalMessage} objects based on the Holodeck B2B signal message objects.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class SMDFactory {
    
    /**
     * Creates a new {@link SignalMessage} object based on the given Holodeck B2B Error signal message.
     * 
     * @param error    The meta-data about the signal message unit as a {@link IErrorMessage}
     * @return         The new {@link SignalMessage} containing the signal meta-data for the given error signal
     */
    public static SignalMessage createSMD(final IErrorMessage error) {
        SignalMessage smd = new SignalMessage();
        
        // Copy the MessageInfo data
        MessageInfo msgInfo = new MessageInfo();
        msgInfo.setMessageId(error.getMessageId());
        msgInfo.setRefToMessageId(error.getRefToMessageId());        
        msgInfo.setTimestamp(convertDate(error.getTimestamp()));        
        smd.setMessageInfo(msgInfo);
        
        // Add the error information to the SMD
        List<Error> errors = smd.getError();
        for (IEbmsError e : error.getErrors()) 
            errors.add(createErrorElement(e));
        
        return smd;
    }
    
    /**
     * Helper method to create an Error element in the SMD based on the Error element from the ebMS header.
     * 
     * @param error     The error from the ebMS header
     * @return          The error meta-data to include in the SMD
     */
    protected static Error createErrorElement(final IEbmsError error) {
        Error   err = new Error();
        
        err.setErrorCode(error.getErrorCode());
        err.setSeverity(error.getSeverity().name());
        err.setErrorDetail(error.getErrorDetail());
        err.setShortDescription(error.getMessage());
        err.setCategory(error.getCategory());
        err.setOrigin(error.getOrigin());
        
        // Copy the extended description when available
        IDescription eDescr = error.getDescription();
        if (eDescr != null && eDescr.getText() != null && !eDescr.getText().isEmpty()) {
            Description description = new Description();
            description.setValue(eDescr.getText());
            description.setLang(eDescr.getLanguage());
            
            err.setDescription(description);
        }
            
        return err;
    }
    
    /**
     * Creates a new {@link SignalMessage} object based on the given Holodeck B2B Receipt signal message with only the
     * first child of the original Receipt included in the notification so the business application can determine what
     * kind of Receipt was received.
     * 
     * @param receipt  The meta-data about the signal message unit as a {@link IReceipt}
     * @return         The new {@link SignalMessage} containing the signal meta-data for the given receipt signal
     */
    public static SignalMessage createSMD(final IReceipt receipt) {
        return createSMD(receipt, false);
    }
    
    
    /**
     * Creates a new {@link SignalMessage} object based on the given Holodeck B2B Receipt signal message.
     * 
     * @param receipt               The meta-data about the signal message unit as a {@link IReceipt}
     * @param includeFullContent    Indicates whether the complete content from the Receipt as in the ebMS message
     *                              should be included in the SMD.
     * @return         The new {@link SignalMessage} containing the signal meta-data for the given receipt signal
     */
    public static SignalMessage createSMD(final IReceipt receipt, final boolean includeFullContent) {
        SignalMessage smd = new SignalMessage();
        
        // Copy the MessageInfo data
        MessageInfo msgInfo = new MessageInfo();
        msgInfo.setMessageId(receipt.getMessageId());
        msgInfo.setRefToMessageId(receipt.getRefToMessageId());        
        msgInfo.setTimestamp(convertDate(receipt.getTimestamp()));        
        smd.setMessageInfo(msgInfo);
        
        // Now copy the Receipt data
        Receipt         rcpt = new Receipt();
        List<Object>    content = rcpt.getAny();        
        List<OMElement> rcptContent = receipt.getContent();
        
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            // We can not create a valid SMD if we can not create an element to contain in the Receipt element
            return null;
        }
        Document doc = builder.newDocument();
        
        if (includeFullContent) {
            // Copy all child elements of the Receipt
            for (OMElement e : rcptContent)
                content.add(convertElement(e, doc));
        } else {
            // Only the first child element of the Receipt needs to be included
            content.add(convertElement(rcptContent.get(0), doc));            
        }
        
        // Add Receipt info to SignalMessage
        smd.setReceipt(rcpt);
            
        return smd;
    }
    
    
    /**
     * Helper method to convert a {@link Date} object to a {@link XMLGregorianCalendar} object.
     * 
     * @param date  The date to convert as {@link Date}
     * @return      The data as {@link XMLGregorianCalendar} if it could be converted, <code>null</code> otherwise
     */
    private static XMLGregorianCalendar convertDate(final Date date) {
        try {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(date);
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        } catch (DatatypeConfigurationException ex) {
            return null;
        }
    }
    
    /**
     * Helper method to convert an Axiom {@link OMElement} to a W3 DOM representation.
     * 
     * @param e     The Axiom representation of the element
     * @param doc   The W3 Document in which the DOM representation is to be inserted
     * @return      The converted element
     */
    private static Element convertElement(OMElement e, Document doc) {
        // Convert the element itself
        Element d = doc.createElementNS(e.getNamespaceURI(), e.getLocalName());
        String prefix = e.getPrefix();
        if (prefix != null)
            d.setPrefix(prefix);
        // And all its attributes
        Iterator<OMAttribute> attrs = e.getAllAttributes();
        for(OMAttribute a ; attrs.hasNext() ;) {
            a = attrs.next();
            String ns = a.getNamespaceURI();
            if (ns != null)
                d.setAttributeNS(ns, a.getLocalName(), a.getAttributeValue());
            else
                d.setAttribute(a.getLocalName(), a.getAttributeValue());
        }
        // And convert all content child nodes, calling this method recursively for child elements
        Iterator<OMNode> content = e.getChildren();
        while (content.hasNext()) {
            OMNode n = content.next();
            if (n instanceof OMElement)
                d.appendChild(convertElement((OMElement) n, doc));
            else if (n instanceof OMText)
                d.appendChild(doc.createTextNode(((OMText) n).getText()));
        }
            
        return d;
    }
}
