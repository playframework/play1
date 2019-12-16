*{
 *  create a link element for a CSS file
 *    src   (required) filename without the leading path
 *    id    (optional) id attribute for the generated link tag
 *    media (optional) media attribute value: screen, print, aural, projection...
 *    title (optional) title attribute value (or description)
 *    path  (optional) leading path (default: "/public/stylesheets/")
 *    ${stylesheet src:'default.css', media:'screen,print' /}
}*
%{
    (_arg ) && (_src = _arg);

    if (!_src) {
        throw new play.exceptions.TagInternalException("src attribute cannot be empty for stylesheet tag");
    }
    _src = (_path ? _path : "/public/stylesheets/") + _src
    try {
        _abs = play.mvc.Router.reverseWithCheck(_src, play.Play.getVirtualFile(_src), false);
    } catch (Exception ex) {
        throw new play.exceptions.TagInternalException("File not found: " + _src, ex);
    }}%
<link rel="stylesheet" type="text/css"#{if _id} id="${_id}"#{/if}#{if _title} title="${_title}"#{/if} href="${_abs}"#{if _media} media="${_media}"#{/if} charset="${_response_encoding}" ></link>
