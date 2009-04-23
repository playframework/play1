%{ 
    if(play.modules.gae.GAE.userService.userLoggedIn) { 
        play.templates.TagContext.parent().data.put("_executeNextElse", false);
%}
    #{doBody /}
%{ 
    } else {
        play.templates.TagContext.parent().data.put("_executeNextElse", true);
    }
}%