/**
 * 
 */
package PloneWars;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

/**
 * @author dadave02
 *
 */
public class PloneCookieStore implements CookieStore {

	/**
	 * 
	 */
	public PloneCookieStore() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see java.net.CookieStore#add(java.net.URI, java.net.HttpCookie)
	 */
	@Override
	public void add(URI arg0, HttpCookie arg1) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see java.net.CookieStore#get(java.net.URI)
	 * Retrieve cookies associated with given URI, or whose domain matches the given URI.
	 */
	@Override
	public List<HttpCookie> get(URI arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.net.CookieStore#getCookies()
	 */
	@Override
	public List<HttpCookie> getCookies() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.net.CookieStore#getURIs()
	 */
	@Override
	public List<URI> getURIs() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.net.CookieStore#remove(java.net.URI, java.net.HttpCookie)
	 */
	@Override
	public boolean remove(URI arg0, HttpCookie arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see java.net.CookieStore#removeAll()
	 */
	@Override
	public boolean removeAll() {
		// TODO Auto-generated method stub
		return false;
	}

}
