package controllers;

import play.*;
import play.mvc.*;

@Check("isAdmin")
@With(Secure.class)
public class Comments extends CRUD {    
}