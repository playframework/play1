package models.japidsample;
 
import java.util.Date;

 
public class Post {
 
    public String title;
    
    public  Author author;
    
    public  Date postedAt;
    
    public String content;
    
    public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	public Date getPostedAt() {
		return postedAt;
	}

	public void setPostedAt(Date postedAt) {
		this.postedAt = postedAt;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("title: " + title + "; author: " + author.name + "; postAt: " + postedAt + "; content: " + content);
		return sb.toString();
	}
 
}