package models;

import java.util.*;
import play.data.validation.*;

public class UserInformation {

	@Required public String firstname; 
	@Required public String lastname;
	@Required @Range(min=16, max=120) public Integer age;
    
}

