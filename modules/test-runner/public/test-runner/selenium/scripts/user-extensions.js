// For Play!

Selenium.prototype.doClearSession = function() {
    this.doDeleteAllVisibleCookies();
};

Selenium.prototype.getLastReceivedEmailBy = function(by) {
    var request = new Ajax.Request('/@tests/emails?by='+by, {
        method: 'get',
        asynchronous: false
    });
    if(request.transport.status == 200) {
        return request.transport.responseText;
    }
    return null;
}

Selenium.prototype.assertNotEquals = function(var1, var2) {
    var a = storedVars[var1];
    var b = storedVars[var2];
    if(a == b) {
        Assert.fail('Expected differents values, but both variables equals to '+a);
    }
};