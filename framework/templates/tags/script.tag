%{
    if(! _src) {
        throw new play.exceptions.TagInternalException("src attribute cannot be empty for script tag");
    }
}%
<script type="text/javascript" language="javascript" charset="utf-8" src="/public/javascripts/${_src}" ></script>
