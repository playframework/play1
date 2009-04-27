%{ 
    if(play.modules.gae.GAE.userService.userAdmin) { 
        play.templates.TagContext.parent().data.put("_executeNextElse", false);
%}
    #{doBody /}
%{ 
    } else {
        play.templates.TagContext.parent().data.put("_executeNextElse", true);
    }
}%