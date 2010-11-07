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

Selenium.prototype.getCacheEntry = function(key) {
    var request = new Ajax.Request('/@tests/cache?key='+key, {
        method: 'get',
        asynchronous: false
    });
    if(request.transport.status == 200) {
        return request.transport.responseText;
    }
    return null;
}

Selenium.prototype.assertEquals = function(a, b) {
    if(a != b) {
        Assert.fail(a+' != '+b);
    }
};

Selenium.prototype.assertNotEquals = function(a, b) {
    if(a == b) {
        Assert.fail(a+' == '+b);
    }
};

Selenium.prototype.assertTextLike = function(a, b) {
    a = a.replace(/\n/g, '').replace(/&nbsp;/g, ' ').replace(/\s+/g, ' ')
    b = b.replace(/\n/g, '').replace(/&nbsp;/g, ' ').replace(/\s+/g, ' ')
    if(a == b) {
        Assert.fail(a+' == '+b);
    }
};

Selenium.prototype.assertPath = function(expectedPath) {
    var path = window.document.location.path;
    if(path == expectedPath) {
        Assert.fail('Expected path '+expectedPath+' but was '+path);
    }
};