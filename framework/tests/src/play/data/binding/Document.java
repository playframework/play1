package play.data.binding;

public class Document {
	public long id;
	private String title;
	protected String body;
	public float d;
	
	public Document () {
		
	}
	
	public Document(long id, String title, String body) {
		super();
		this.id = id;
		this.title = title;
		this.body = body;
	}

	public String getBody() {
		return body;
	}

	public long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}
	
	
}
