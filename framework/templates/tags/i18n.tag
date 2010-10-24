%{
js_messages=new com.google.gson.Gson().toJson(play.i18n.Messages.all(play.i18n.Lang.get()));
}%
<script type="text/javascript" charset="utf-8">

var i18nMessages = ${js_messages};

/**
 * Fixme : only parse single char formatters eg. %s
 */
var i18n = function(code) {
    var message;
    if (arguments.length > 1) {
        var message = i18nMessages[code] || "";
        for(var i=1; i< arguments.length; i++) {
            message = message.replace(/%\w/, arguments[i]); 
        }
    } else {
        message = i18nMessages && i18nMessages[code] || code;
    }
    // Imbricated messages
    var imbricated = message.match( /&\{.*?\}/g );
    alert(imbricated);
    for( var i=0; i<imbricated.length; i++ ) {
        var imbricated_code = imbricated[i].substring(2, imbricated[i].length-1).replace(/^\s*(.*?)\s*$/, "$1");
        message = message.replace(imbricated[i], i18nMessages[imbricated_code] || "");
    }
    return message;
};

</script>
