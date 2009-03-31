*{
 *  insert a script element
 *  script must stay under /public/javascripts
 *    src (required) script filename, without the leading path "/public/javascripts"
 *   id (optional) DOM element id
 *
 *    #{script id:'datepicker' src:'ui/ui.datepicker.js' /}
}*
%{
    if(! _src) {
        throw new play.exceptions.TagInternalException("src attribute cannot be empty for script tag");
    }      
}%
<script type="text/javascript" language="javascript"#{if _id} id="${_id}"#{/if} charset="utf-8" src="/public/javascripts/${_src}"></script>
