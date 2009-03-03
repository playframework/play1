// Registration functions for the Bespin front page.

var svr = new Bespin.Server();
var go = Bespin.Navigate;

function whenLoginSucceeded() {
    go.dashboard();
}

function whenLoginFailed() {
    showStatus("Sorry, login didn't work. Try again? Caps lock on?");
}

function whenUsernameInUse() {
    showStatus("The username is taken. Please choose another.");
}

function showStatus(msg) {
    $("status").innerHTML = msg;
    $("status").show();
}

function login() {
    if (showingBrowserCompatScreen()) return;

    if ($("username").value && $("password").value) {
        // try to find the httpSessionId
        var cookies = document.cookie.split(';');
        var foundValue = "";
        for (var i = 0; i < cookies.length; i++) {
            var cookie = cookies[i];
            while (cookie.charAt(0) == ' ') cookie = cookie.substring(1, cookie.length);
            if (cookie.indexOf("anticsrf=") == 0) {
                foundValue = cookie.substring(dwr.engine._sessionCookieName.length + 1, cookie.length);
                break;
            }
        }

        svr.login($("username").value, $("password").value, foundValue, whenLoginSucceeded, whenLoginFailed)
    } else {
        showStatus("Please give me both a username and a password");
    }
}

function whenAlreadyLoggedIn(userinfo) {
    $('display_username').innerHTML = userinfo.username;
    $('logged_in').show();
}

function whenNotAlreadyLoggedIn() {
    $('not_logged_in').show();
}

function logout() {
    svr.logout();
    $('logged_in').hide();
    $('not_logged_in').show();
}

function centerOnScreen(el) {
    // retrieve required dimensions
    var elDims = el.getDimensions();

    if (navigator.appName === "Microsoft Internet Explorer") {
        var y = (document.documentElement.clientHeight - elDims.height) / 2;
        var x = (document.documentElement.clientWidth - elDims.width) / 2;
    } else {
        var browserDims = document.body.getDimensions();

        // calculate the center of the page using the browser and element dimensions
        var y = (browserDims.height - elDims.height) / 2;
        var x = (browserDims.width - elDims.width) / 2;
    }

    // set the style of the element so it is centered
    var styles = {
        position: 'absolute',
        top: y + 'px',
        left : x + 'px'
    };
    el.setStyle(styles);
}

// make sure that the browser can do our wicked shizzle
function checkBrowserAbility() {
    if (typeof $('testcanvas').getContext != "function") return false; // no canvas

    var ctx = $('testcanvas').getContext("2d");

    if (ctx.fillText || ctx.mozDrawText)
        return true; // you need text support my friend
    else
        return false;
}

function showingBrowserCompatScreen() {
    if (!checkBrowserAbility()) { // if you don't have the ability
        centerOnScreen($('browser_not_compat'));
        $('browser_not_compat').show();
        $('opaque').show();
        return true;
    } else {
        return false;
    }
}

function hideBrowserCompatScreen() {
    $('browser_not_compat').hide();
    $('opaque').hide();
}

function validateEmail(str) {
    var filter=/^([\w-]+(?:\.[\w-]+)*)@((?:[\w-]+\.)*\w[\w-]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$/i
    return filter.test(str);
}

var register = {
    checkUsername:function() {
        $('register_username_error').innerHTML = ($F('register_username').length < 4) ? "Usernames must be at least 4 characters long" : "";
    },
    checkPassword:function() {
        $('register_password_error').innerHTML = ($F('register_password').length < 6) ? "Passwords must be at least 6 characters long" : "";
    },
    checkConfirm:function() {
        $('register_confirm_error').innerHTML = ($F('register_password') != $F('register_confirm')) ? "Passwords do not match" : "";
    },
    checkEmail:function() {
        $('register_email_error').innerHTML = (!validateEmail($F('register_email'))) ? "Invalid email address" : "";
    },
    showForm:function() {
        if (showingBrowserCompatScreen()) return;
        $('logged_in').hide();
        $('not_logged_in').hide();
        centerOnScreen($('register_border'));
        $('register_border').show();
        $('opaque').show();
    },
    hideForm:function() {
        $('opaque').hide();
        $('register_border').hide();
        svr.currentuser(whenAlreadyLoggedIn, whenNotAlreadyLoggedIn);
    },
    send:function() {
        register.hideForm();
        svr.signup($("register_username").value, $("register_password").value, $('register_email').value, whenLoginSucceeded, whenLoginFailed, whenUsernameInUse);
    },
    cancel:function() {
        register.hideForm();
    }
};

Event.observe(document, "dom:loaded", function() {
    Bespin.displayVersion();
    svr.currentuser(whenAlreadyLoggedIn, whenNotAlreadyLoggedIn);
});

