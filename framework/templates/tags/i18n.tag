%{
if (_keys) {
    ymessages = play.i18n.Messages.find(play.i18n.Lang.get(), _keys as Set);
} else {
    ymessages = play.i18n.Messages.all(play.i18n.Lang.get());
}
js_messages=new com.google.gson.Gson().toJson(ymessages);
}%
#{if !_noScriptTag}
<script type="text/javascript">
#{/if}
var i18nMessages = ${js_messages};

/**
 * Fixme : only parse single char formatters eg. %s
 */
var i18n = function(code) {
    var message = i18nMessages && i18nMessages[code] || code;
    // Encode %% to handle it later
    message = message.replace(/%%/g, "\0%\0");
    if (arguments.length > 1) {
        // Explicit ordered parameters
        for (var i=1; i<arguments.length; i++) {
            var r = new RegExp("%" + i + "\\$\\w", "g");
            message = message.replace(r, arguments[i]);
        }
        // Standard ordered parameters
        for (var i=1; i<arguments.length; i++) {
            message = message.replace(/%\w/, arguments[i]);
        }
    }
    // Decode encoded %% to single %
    message = message.replace(/\0%\0/g, "%");
    // Imbricated messages
    var imbricated = message.match(/&\{.*?\}/g);
    if (imbricated) {
        for (var i=0; i<imbricated.length; i++) {
            var imbricated_code = imbricated[i].substring(2, imbricated[i].length-1).replace(/^\s*(.*?)\s*$/, "$1");
            message = message.replace(imbricated[i], i18nMessages[imbricated_code] || "");
        }
    }
    return message;
};
#{if !_noScriptTag}
</script>
#{/if}
