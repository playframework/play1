*{
 *  insert a script tag in the template.
 *  by convention, referred script must be put under /public/javascripts
 *    src     (required)   : script filename, without the leading path (default: "/public/javascripts")
 *    id      (opt.)       : sets script id attribute
 *    charset (opt.)       : sets source encoding - defaults to current response encoding
 *    path    (opt.)       : sets the path defaults to "/public/javascripts"
 *
 *    #{script id:'datepicker' , src:'ui/ui.datepicker.js', charset:'${_response_encoding}' /}
}*
%{
    (_arg ) && (_src = _arg);

    if (!_src) {
        throw new play.exceptions.TagInternalException("src attribute cannot be empty for script tag");
    }
    _src = (_path ? _path : "/public/javascripts/") + _src
    try {
        _abs = play.mvc.Router.reverseWithCheck(_src, play.Play.getVirtualFile(_src), false);
    } catch (Exception ex) {
        throw new play.exceptions.TagInternalException("File not found: " + _src);
    }
}%
<script type="text/javascript" language="javascript"#{if _id} id="${_id}"#{/if}#{if _charset} charset="${_charset}"#{/if}  src="${_abs}"></script>
