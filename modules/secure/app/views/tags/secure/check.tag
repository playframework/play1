#{if session.username && controllers.Secure.Security.invoke("check", _arg)}
    #{doBody /}
#{/if}