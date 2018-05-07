# cordova-plugin-keyboard 

# Installation

From github latest (may not be stable)

`cordova plugin add https://github.com/RoqosInc/cordova-plugin-dnsproxy`

# Supported Platform
- Android

# Methods

## DnsPlugin.config
Config the DNS Server

window.plugins.dnsPlugin.config({
    dnsServer: "dnsServer",
    port: "as a string",
    VPNSessionTitle: "title to show in the session pop up"
});

#### Quick Example
window.plugins.dnsPlugin.config({
    dnsServer: "124.15.25.65",
    port: "53",
    VPNSessionTitle: "Roqos"
});

## DnsPlugin.isActivated
Check the activation of the dns configuration

window.plugins.dnsPlugin.isActivated(success, error);

#### Quick Example
window.plugins.dnsPlugin.isActivated(function(status){
    console.log(status);
});

## DnsPlugin.activate
Activate the custom dns configuration

window.plugins.dnsPlugin.activate(success, error);

#### Quick Example
window.plugins.dnsPlugin.activate();

## DnsPlugin.deactivate
Deactivate the custom dns configuration

window.plugins.dnsPlugin.deactivate(success, error);

#### Quick Example
window.plugins.dnsPlugin.deactivate();

## DnsPlugin.addEDNSOption
Add an EDNS option.

window.plugins.dnsPlugin.addEDNSOption(optionCode, message, success, error);

#### Quick Example
window.plugins.dnsPlugin.addEDNSOption(65073, "545e5f");

## DnsPlugin.removeAllEDNSOption
remove all the added EDS Options

window.plugins.dnsPlugin.removeAllEDNSOption(success, error);

#### Quick Example
window.plugins.dnsPlugin.removeAllEDNSOption();