// Empty constructor
function DnsPlugin() {}

DnsPlugin.prototype.activate = function(successCallback, errorCallback) {
  cordova.exec(successCallback, errorCallback, 'DnsPlugin', 'activate');
}

DnsPlugin.prototype.deactivate = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'DnsPlugin', 'deactivate');
}

DnsPlugin.prototype.config = function(option, successCallback, errorCallback) {
    var options = {};
    options.dnsServer = option.dnsServer;
    options.port = option.port;
    options.VPNSessionTitle = option.VPNSessionTitle;
    cordova.exec(successCallback, errorCallback, 'DnsPlugin', 'config', [options]);
}

DnsPlugin.prototype.addEDNSOption = function(optionCode, message, successCallback, errorCallback) {
    var options = {};
    options.optionCode = optionCode;
    options.message = message;
    cordova.exec(successCallback, errorCallback, 'DnsPlugin', 'addEDNSOption', [options]);
}

DnsPlugin.prototype.removeAllEDNSOption = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'DnsPlugin', 'removeAllEDNSOption');
}

DnsPlugin.prototype.isActivated = function(successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'DnsPlugin', 'isActivated');
}

// Installation constructor that binds DnsPlugin to window
DnsPlugin.install = function() {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.dnsPlugin = new DnsPlugin();
  return window.plugins.dnsPlugin;
};
cordova.addConstructor(DnsPlugin.install);
