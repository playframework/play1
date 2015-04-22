package controllers.security;

import play.mvc.Controller;



/**
 * Test controller to check response to Xss attempt.
 */
public class XssAttempts extends Controller {

  public static void testUrlParam(String url){  
    render(url);
  }

}
