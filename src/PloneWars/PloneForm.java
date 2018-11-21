package PloneWars;

public class PloneForm extends PloneFolder {

	public static final String	TYPE	= "FormFolder";

	public PloneForm(String name, PloneFolder parent) {
		super(name, parent);
	}

	public boolean upload() {
		PloneWarsApp.out.println("form upload");
		return super.upload();
	}
}
