package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.security.Principal;
import java.util.Iterator;

import org.apache.jackrabbit.core.security.principal.PrincipalIteratorAdapter;
import org.sakaiproject.nakamura.api.lite.authorizable.Authorizable;

public class SparsePrincipalIterator extends PrincipalIteratorAdapter {

	public SparsePrincipalIterator(final Iterator<? extends Authorizable> iterator) {
		super(new Iterator<Principal>() {
			
			Principal principal;
			public boolean hasNext() {
				while(iterator.hasNext()) {
					Authorizable a = iterator.next();
					if ( a != null ) {
						principal = new SparsePrincipal(a, this.getClass().getName());
						return true;
					}
				}
				principal = null;
				return false;
			}

			public Principal next() {
				return principal;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		});
	}


}
