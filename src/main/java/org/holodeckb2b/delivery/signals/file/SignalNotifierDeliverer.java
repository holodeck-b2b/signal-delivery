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
package org.holodeckb2b.delivery.signals.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.holodeckb2b.delivery.signals.smd.ObjectFactory;
import org.holodeckb2b.delivery.signals.smd.SignalMessage;
import org.holodeckb2b.delivery.signals.utils.SMDFactory;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;
import org.holodeckb2b.interfaces.messagemodel.IErrorMessage;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Is the {@link IMessageDeliverer} implementation for notifying business applications about signal messages.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class SignalNotifierDeliverer implements IMessageDeliverer {

    Logger log = LoggerFactory.getLogger(SignalNotifierDeliverer.class);

    /**
     * The directory where to write the signal meta-data file
     */
    private String    deliveryDir;
    
    /**
     * Indicates whether the content of the Receipt should be included in the notification. Default it is not.
     */
    private boolean includeReceiptContent;
    
    /**
     * Creates the deliver and sets the destination directory for the meta-data documents and indicator whether the
     * Receipt content should be included in the notifications.
     * 
     * @param deliveryDir               Path of the directory where to write the signal meta-data documents.
     * @param includeReceiptContent     Indication whether to include the Receipt content in the notification.
     */
    public SignalNotifierDeliverer(final String deliveryDir, final boolean includeReceiptContent) {
        this.deliveryDir = deliveryDir;
        this.includeReceiptContent = includeReceiptContent;        
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
        File file = null;
        try {
            file = ensureUniqueFile(deliveryDir + sigMsgId.replaceAll("[^a-zA-Z0-9.-]", "_") + ".smd.xml");
        } catch (IOException ex) {
            log.error("Can not create a file in output directory {} to write Signal meta-data (msgId={})",
                        deliveryDir, sigMsgId);
            throw new MessageDeliveryException("Could not create file for delivery of Signal", ex);
        }
        log.debug("Write SMD for {} (msgId= {}) to {}", sigType, sigMsgId, file.getAbsolutePath());
        
        try {
            JAXBContext jbCtx = JAXBContext.newInstance(SignalMessage.class.getPackage().getName());
            Marshaller jbMarshaller = jbCtx.createMarshaller();
            JAXBElement<SignalMessage> smdDoc = new ObjectFactory().createSignalMessage(smd);
            jbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jbMarshaller.marshal(smdDoc, file);
        } catch (JAXBException e) {
            log.error("Could not write SMD for " + sigType + "(msgId= " + sigMsgId + ")! Details: " 
                        + e.getErrorCode() + " - " + e.getMessage());
            throw new MessageDeliveryException("Could not write the SMD file!", e);
        }
    }

    /**
     * Helper method to ensure the file for writing the Signal to is unique.
     * 
     * @param basePath      The default file name to use for writing the Signal meta-data
     * @return              A {@link File} object for the file that the meta-data can be written to
     * @throws IOException  When the file can not be created
     */
    private File ensureUniqueFile(final String basePath) throws IOException {
        // Split the given path into name and extension part (if possible)
        String nameOnly = basePath;
        String ext = "";
        int startExt = basePath.lastIndexOf(".");
        if (startExt > 0) { 
            nameOnly = basePath.substring(0, startExt);
            ext = basePath.substring(startExt);
        }
        
        File f = null;
        Path targetPath = Paths.get(basePath);
        int i = 1;
        while (f == null) {
            try {
                f = Files.createFile(targetPath).toFile();                
            } catch (FileAlreadyExistsException faee) {
                // Okay, the file already exists, try with increased sequence number
                targetPath = Paths.get(nameOnly + "-" + i++ + ext);
            }
        }
        
        return f;
    }
    
}
