/* 
* Copyright 2023 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.backend.file;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.commons.util.FileUtils;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.delivery.signals.smd.ObjectFactory;
import org.holodeckb2b.delivery.signals.smd.SignalMessage;
import org.holodeckb2b.delivery.signals.utils.SMDFactory;
import org.holodeckb2b.interfaces.delivery.IDeliveryMethod;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;

/**
 * Is a {@link IDeliveryMethod} implementation that notifies the business application about received <i>Signal 
 * Messages</i> by writing the signal meta-data (SMD) to file.
 * <p>The XML format of the meta data document is based on the ebMS header for signals and is defined in the 
 * <code>http://holodeck-b2b.org/schemas/2015/08/smd</code> xml schema definition.
 * <p>The delivery method takes two parameters:<ol>
 * <li><i>targetDirectory</i> : the path to the directory where the SMD files should be written.</li>
 * <li><i>includeReceiptContent</i> : a boolean that indicate whether the complete Receipt content from the ebMS message
 * should be included (<i>"true"</i>) or only the first child element (<i>"false"</i>). Default is <i>false</i> (only
 * first child included).</li>
 * </ol>
 * <p>NOTE: This delivery method can only be used for Signals!
 */
public class NotifyOperation implements IDeliveryMethod {
    private final Logger log = LogManager.getLogger(NotifyOperation.class);

    /**
     * The path to the directory where to save the notifications
     */
    private String  deliveryDir = null;
    
    /**
     * Indicates whether the content of the Receipt should be included in the notification. Default it is not.
     */
    private boolean includeReceiptContent = false;
	
    /**
     * Initializes the delivery method. Ensures that the specified directory is available for delivery of the Signals,
     * i.e. if the path already exists checks that it is a writable directory or if it does not exist yet create it.
     * 
     * @param settings  The settings to use for the notifications. MUST contain at least on entry with key 
     *                  <code>deliveryDirectoy</code> holding the path to the delivery directory. 
     * @throws MessageDeliveryException When there the specified directory is not writable or can not be created
     */
	@Override
	public void init(Map<String, ?> settings) throws MessageDeliveryException {
        String includeRcptCont = null;
        
        if (settings != null) {
            try {
               deliveryDir = (String) settings.get("targetDirectory");                
               includeRcptCont = (String) settings.get("includeReceiptContent");
            } catch (ClassCastException ex) {
                throw new MessageDeliveryException("Configuration error! No directory specified!");
            }
        }        
        if (!checkDirectory())
            throw new MessageDeliveryException("Configuration error! Specified directory [" + deliveryDir
                                                                        + "] is not available!");
        // Ensure directory path ends with separator
        deliveryDir = (deliveryDir.endsWith(FileSystems.getDefault().getSeparator()) ? deliveryDir 
                              : deliveryDir + FileSystems.getDefault().getSeparator());
        
        // Should we include receipt content?
        includeReceiptContent = Utils.isTrue(includeRcptCont);
	}

	 /**
     * Helper method that checks if the specified path for delivery of the Signals is a writable directory or if the
     * path does not exist a directory can be created.
     * 
     * @return  <code>true</code> when the specified path can be used for delivery,<br><code>false</code> otherwise
     */
    private boolean checkDirectory() {
        try {
            Path path = Paths.get(deliveryDir);                        

            // Test if given path exists and is a writable directory
            if (Files.exists(path))
                // The path exists, it can be used for delivery if it is a writable directory
                return Files.isDirectory(path) && Files.isWritable(path);
            else {
                // The path does not exist yet, so try to create it
                Files.createDirectories(path);            
                return true;
            }
        } catch (Exception ex) {
            return false;
        }
    }
    
	@Override
	public boolean supportsAsyncDelivery() {
		return false;
	}

	/**
     * Does the actual delivery of the signal message to the business application.
     *
     * @param rcvdMsgUnit   The signal message to be delivered to the business application
     * @throws MessageDeliveryException When the given message unit is not a signal or when the signal meta-data
     *                                  document can not be created or written to file.
     */
    @Override
    public void deliver(IMessageUnit rcvdMsgUnit) throws MessageDeliveryException {
        String sigType = rcvdMsgUnit.getClass().getSimpleName();
        String sigMsgId = rcvdMsgUnit.getMessageId();

        log.debug("Create SMD for " + sigType + ", msgId= " + sigMsgId);
        SignalMessage smd;
        if (rcvdMsgUnit instanceof IReceipt)
            smd = SMDFactory.createSMD((IReceipt) rcvdMsgUnit, includeReceiptContent);
        else if (rcvdMsgUnit instanceof IErrorMessage)
            smd = SMDFactory.createSMD((IErrorMessage) rcvdMsgUnit);
        else {
            log.warn("This delivery method can not be used for delivery of User Messages!");
            throw new MessageDeliveryException("This delivery method can not be used for User messages!");
        }

        // Check that SMD document could be created
        if (smd == null) {
            log.error("Failed to create the SMD for " + sigType + ", msgId= " + sigMsgId);
            throw new MessageDeliveryException("Could not create meta-data document for Signal message");
        }

        // Create the filename for the SMD file based on message id of signal
        Path file = null;
        try {
            file = FileUtils.createFileWithUniqueName(deliveryDir + sigMsgId.replaceAll("[^a-zA-Z0-9.-]", "_") + ".smd.xml");
        } catch (IOException ex) {
            log.error("Can not create a file in output directory {} to write Signal meta-data (msgId={})",
                        deliveryDir, sigMsgId);
            throw new MessageDeliveryException("Could not create file for delivery of Signal", ex);
        }
        log.debug("Write SMD for {} (msgId= {}) to {}", sigType, sigMsgId, file.toString());

        try {
            JAXBContext jbCtx = JAXBContext.newInstance(SignalMessage.class.getPackage().getName());
            Marshaller jbMarshaller = jbCtx.createMarshaller();
            JAXBElement<SignalMessage> smdDoc = new ObjectFactory().createSignalMessage(smd);
            jbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jbMarshaller.marshal(smdDoc, file.toFile());
        } catch (JAXBException e) {
            log.error("Could not write SMD for " + sigType + "(msgId= " + sigMsgId + ")! Details: "
                        + e.getErrorCode() + " - " + e.getMessage());
            throw new MessageDeliveryException("Could not write the SMD file!", e);
        }
    }   
}
