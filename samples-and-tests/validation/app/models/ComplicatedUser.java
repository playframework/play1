package models;

import java.util.*;
import play.data.validation.*;

public class ComplicatedUser {

	@Required @MinSize(6) public String username;
	@Valid public UserInformation information;
	@Required @MinSize(6) public String password;
	@Required @Equals("password") public String passwordConfirm;
	@Required @Email public String email;
	@Required @Equals("email") public String emailConfirm;
	@Required @IsTrue public boolean termsOfUse;
    
}

