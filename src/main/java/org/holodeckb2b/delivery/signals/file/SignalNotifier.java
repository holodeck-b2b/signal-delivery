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

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.holodeckb2b.backend.file.NotifyOperation;
import org.holodeckb2b.interfaces.delivery.MessageDeliveryException;

/**
 * @deprecated This class is only included for back-ward compatibility. Configurations <b>should</b> be changed asap.
 */
@Deprecated
public class SignalNotifier extends NotifyOperation {

	@Override
	public void init(Map<String, ?> settings) throws MessageDeliveryException {
		LogManager.getLogger().warn("org.holodeckb2b.delivery.signals.SignalNotifier is deprecated! Use org.holodeckb2b.backend.file.NotifyOperation instead");
		super.init(settings);
	}	   
}
