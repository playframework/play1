%{
 // @since play rev138  
 js_messages=fr.zenexity.json.JSON.toJSON( play.i18n.Messages.all("fr"));
 
}%
<script type="text/javascript" language="javascript" charset="utf-8">
    
var i18nMessages = ${js_messages};

var i18n=function(code) {
      if( arguments.length > 1 ) {
          var message = i18nMessages[code] || "";
          for( var i=1; i< arguments.length; i++ ) {
              // raw replacement - fixme : support for special formatters
              message = message.replace( /%\S+/, arguments[i]); 
          }
          return message;
      }
      return i18nMessages[code] || "!"+code+"!";
 };

</script>