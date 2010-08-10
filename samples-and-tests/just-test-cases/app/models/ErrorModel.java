package models;

import play.db.jpa.Model;

import javax.persistence.*;

@Entity
@Table(name = "errors")
public class ErrorModel extends Model {

	public ErrorModel() {
		member = new ErrorMember();
	}

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "member_id")
	private ErrorMember member;

	public ErrorMember getMember() {
		return member;
	}

	public void setMember(ErrorMember member) {
		/* The following line produces the ERROR (comment line 28 and uncomment line 29 to verify)
			 The idea is that if I just overwrote the member's reference
			 a NEW tuple would be inserted in the db after calling ErrorModel.save() 
			 so I have to keep the existing reference here.
		*/
		getMember().setName(member.getName());
		//this.member = member;
	}
}
