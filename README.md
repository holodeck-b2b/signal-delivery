# Holodeck B2B Signal Delivery
This project is an example of a custom extension to the Holodeck B2B Core platform. It implements a _message delivery
method_ for ebMS Signal message units.
The ebMS data from the Signal messages is written to an XML document using the same structure as the ebMS header. A
simple XML schema definition is provided for "cleaner" generated code for XML processing.
As this delivery method is only able to process Signal message units it should only be used in One-Way P-Modes that are
used for sending User Messages.

__________________
For more information on using Holodeck B2B visit the website at http://holodeck-b2b.org  
Developer: Sander Fieten  
Code hosted at https://github.com/holodeck-b2b/signal-delivery  
Issue tracker https://github.com/holodeck-b2b/signal-delivery/issues

## Installation
Copy the jar file into the `lib` directory of the Holodeck B2B installation **before** the server is started.

### Prerequisites
This extension has no specific requirements and can be installed in any Holodeck B2B version 2.0.0 or later running on
Java 7 or later.

### Configuration
Delivery methods are configured in the P-Mode.
To enable this signal delivery method set the `DeliveryMethod` element to `org.holodeckb2b.delivery.signals.file.SignalNotifier`.
The delivery method has two parameters:

1. _deliveryDirectory_ : should contain the path where the files containing the signal meta-data should be written to;
2. _includeReceiptContent_ : indicates whether the complete content of a _Receipt_ signal should be included in the output
or only the first child element. This is an optional parameter with default value _false_.

## Contributing
We’re using the simplified Github workflow to accept modifications which means you’ll have to:
* create an issue related to the problem you want to fix or the function you want to add (good for traceability and cross-reference)
* fork the repository
* create a branch (optionally with the reference to the issue in the name)
* write your code
* commit incrementally with readable and detailed commit messages
* submit a pull-request against the master branch of this repository

### Submitting bugs
Please note that this project is provided as an example and is **not actively supported** by the Holodeck B2B dev team.
You can still report issues on the [project Issue Tracker](https://github.com/holodeck-b2b/signal-delivery/issues)
Please document the steps to reproduce your problem as much as possible.

## Versioning
Version numbering follows the [Semantic versioning](http://semver.org/) approach.

## Licensing
The licence used for this extension is the [European Union Public License](https://joinup.ec.europa.eu/community/eupl/home).
Although Holodeck B2B Core is licensed under the General Public License V3 this is possible because this extension only
uses the *interface module* of the main Holodeck B2B project which is licensed under the Lesser General Public License V3.

