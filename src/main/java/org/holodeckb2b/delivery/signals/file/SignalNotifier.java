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

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.holodeckb2b.interfaces.delivery.IMessageDeliverer;
import org.holodeckb2b.interfaces.delivery.IMessageDelivererFactory;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;

/**
 * Is a {@link IMessageDelivererFactory} implementation for the delivery method that notifies the business application
 * about received Signal messages by writing the signal meta-data (SMD) to file.
 * <p>The XML format of the meta data document is based on the ebMS header for signals and is defined in the 
 * <code>http://holodeck-b2b.org/schemas/2015/08/smd</code> xml schema definition.
 * <p>The factory takes two parameters:<ol>
 * <li><i>deliveryDirectory</i> : the path to the directory where the SMD files should be written.</li>
 * <li><i>includeReceiptContent</i> : a boolean that indicate whether the complete Receipt content from the ebMS message
 * should be included (<i>"true"</i>) or only the first child element (<i>"false"</i>). Default is <i>false</i> (only
 * first child included).</li>
 * </ol>
 * <p>NOTE: This delivery method can only be used for Signals!
 */
public class SignalNotifier implements IMessageDelivererFactory {

    /**
     * The path to the directory where to save the notifications
     */
    private String  deliveryDir = null;
    
    /**
     * Indicates whether the content of the Receipt should be included in the notification. Default it is not.
     */
    private boolean includeReceiptContent = false;
    
    /**
     * Initializes the factory. Ensures that the specified directory is available for delivery of the Signals, i.e.
     * if the path already exists checks that it is a writable directory or if it does not exist yet create it.
     * 
     * @param settings  The settings to use for the factory. MUST contain at least on entry with key 
     *                  <code>deliveryDirectoy</code> holding the path to the delivery directory. 
     * @throws MessageDeliveryException When there the specified directory is not writable or can not be created
     */
    @Override
    public void init(Map<String, ?> settings) throws org.holodeckb2b.interfaces.delivery.MessageDeliveryException {
        String includeRcptCont = null;
        
        if (settings != null) {
            try {
               deliveryDir = (String) settings.get("deliveryDirectory");                
               includeRcptCont = (String) settings.get("includeReceiptContent");
            } catch (ClassCastException ex) {
                throw new MessageDeliveryException("Configuration error! No directory specified!");
            }
        }        
        if (!checkDirectory())
            throw new MessageDeliveryException("Configuration error! Specified directory [" + deliveryDir
                                                                        + " is not available!");
        // Ensure directory path ends with separator
        deliveryDir = (deliveryDir.endsWith(FileSystems.getDefault().getSeparator()) ? deliveryDir 
                              : deliveryDir + FileSystems.getDefault().getSeparator());
        
        // Should we include receipt content?
        includeReceiptContent = includeRcptCont != null && 
                                ( "yes".equalsIgnoreCase(includeRcptCont) 
                                || "Y".equalsIgnoreCase(includeRcptCont) 
                                || "true".equalsIgnoreCase(includeRcptCont) 
                                || "1".equalsIgnoreCase(includeRcptCont)
                                );
    }

    @Override
    public IMessageDeliverer createMessageDeliverer() throws MessageDeliveryException {
        return new SignalNotifierDeliverer(deliveryDir, includeReceiptContent);
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
}
