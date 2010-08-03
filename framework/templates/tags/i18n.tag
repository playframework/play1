%{
js_messages=new com.google.gson.Gson().toJson(play.i18n.Messages.all(play.i18n.Lang.get()));
}%
<script type="text/javascript" charset="utf-8">

var i18nMessages = ${js_messages};

/**
 * Fixme : only parse single char formatters eg. %s
 */
var i18n=function(code) {
    if( arguments.length > 1 ) {
        var message = i18nMessages[code] || "";
        for( var i=1; i< arguments.length; i++ ) {
            message = message.replace( /%\w/, arguments[i]); 
        }
        return message;
    }
    return i18nMessages && i18nMessages[code] || code;
};

</script>
