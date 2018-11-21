package PloneWars;

public class PloneFieldset extends PloneForm
{
	public static final String TYPE = "FieldsetFolder";
	
	
	public PloneFieldset(String name, PloneFolder parent)
	{
		super(name, parent);
	}
	
	// TODO: going to need to modify this for Plone 4 since it doesn't use fieldsets as folders

}
