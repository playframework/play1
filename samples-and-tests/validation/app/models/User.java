package models;

import java.util.*;
import play.data.validation.*;

public class User {

	@Required @MinSize(6) public String username;
	@Required public String firstname; 
	@Required public String lastname;
	@Required @Range(min=16, max=120) public Integer age;
	@Required @MinSize(6) public String password;
	@Required @Equals("password") public String passwordConfirm;
	@Required @Email public String email;
	@Required @Equals("email") public String emailConfirm;
	@Required @IsTrue public boolean termsOfUse;
    
}

