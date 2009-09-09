package controllers;

import play.*;
import play.mvc.*;

@Check("isAdmin")
@With(Secure.class)
public class Users extends CRUD {    
}