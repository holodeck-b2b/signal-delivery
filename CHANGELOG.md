# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## 2.0.0
##### 2023-03-08
### Changed
* Compatibility with the new Holodeck B2B v6.x API
* Name of the delivery method changed to `org.holodeckb2b.backend.file.NotifyOperation`. 
* Name of the parameter indicating directory where to save Signal meta-data to _targetDirectory_. 

### Deprecated
* The old delivery method name `org.holodeckb2b.delivery.signals.file.SignalNotifier` and its
  _deliveryDirectory_ parameter. 

## 1.1.1
##### 2021-11-22
### Added
* JAXB dependencies to enable building and running on Java 9 or later.

## 1.1.0
##### 2020-09-03
### Added
* Generic method to `org.holodeckb2b.delivery.signals.utils.SMDFactory` to create XML representation of a Signal

### Changed
* Updated dependencies to Holodeck B2B 5.0.0
* When the the _"Include Receipt content"_ parameter is set to _false_ only the first child element of the original 
  Receipt is included without its children.  
 
## 1.0.0
##### 2016-04-08 
### Added
* Initial release. 

