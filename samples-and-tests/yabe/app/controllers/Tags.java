package controllers;

import play.*;
import play.mvc.*;

@Check("admin")
@With(Secure.class)
public class Tags extends CRUD {    
}