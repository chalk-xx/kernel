package org.sakaiproject.nakamura.api.lite.jackrabbit;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.GuestCredentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.authentication.AbstractLoginModule;
import org.apache.jackrabbit.core.security.authentication.Authentication;
import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AdministrativeCredentials;
import org.apache.sling.jcr.jackrabbit.server.impl.security.AnonCredentials;
import org.apache.sling.jcr.jackrabbit.server.impl.security.CallbackHandlerWrapper;
import org.apache.sling.jcr.jackrabbit.server.impl.security.TrustedCredentials;
import org.sakaiproject.nakamura.api.lite.ConnectionPoolException;
import org.sakaiproject.nakamura.api.lite.Repository;
import org.sakaiproject.nakamura.api.lite.StorageClientUtils;
import org.sakaiproject.nakamura.api.lite.accesscontrol.Authenticator;
import org.sakaiproject.nakamura.api.lite.authorizable.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparseLoginModule extends AbstractLoginModule {

	private static final Logger LOGGER = LoggerFactory.getLogger(SparseLoginModule.class);
	private Repository repository;
	private Authenticator authenticator;
	private User user;

	@Override
	protected void doInit(CallbackHandler callbackHandler, Session session,
			@SuppressWarnings("rawtypes") Map options) throws LoginException {
		repository = SparseComponentHolder.getSparseRepositoryInstance();
		LOGGER.info("Initializing Login Module ieb");
		CredentialsCallback cb = new CredentialsCallback();
		try {
			callbackHandler.handle(new Callback[] { cb});
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnsupportedCallbackException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		LOGGER.info("Initializing Login Module ieb credentials are {}",cb.getCredentials());
		try {
			authenticator = repository.getAuthenticator();
		} catch (ConnectionPoolException e) {
			throw new LoginException(e.getMessage());
		}
	}

    /**
     * {@inheritDoc}
     * @see org.apache.jackrabbit.core.security.authentication.AbstractLoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        CallbackHandlerWrapper wrappedCallbackHandler = new CallbackHandlerWrapper(subject, callbackHandler);

        super.initialize(subject, wrappedCallbackHandler, sharedState, options);
    }
    
	@Override
	protected boolean impersonate(Principal principal, Credentials credentials)
			throws RepositoryException, LoginException {
		User user = authenticator.systemAuthenticate(principal.getName());
		if (user != null) {
			Subject impersSubject = getImpersonatorSubject(credentials);
			
			if ( !impersSubject.getPrincipals(AdminPrincipal.class).isEmpty() || !impersSubject.getPrincipals(SystemPrincipal.class).isEmpty() ) {
				return true;
			}

			String impersonators = StorageClientUtils.toString(user.getProperty(User.IMPERSONATORS_FIELD));
			if ( impersonators != null ) {
				Set<String> imp = new HashSet<String>();
				Collections.addAll(imp, StringUtils.split(impersonators,';'));
				for ( Principal p : subject.getPrincipals() ) {
					if ( imp.contains(p.getName()) ) {
						return true;
					}
				}
			}
			throw new FailedLoginException(
						"attempt by user "+principal.getName()+" with subjects "+impersSubject.getPrincipals()+" to impersonate "+credentials);
		}
		return false;
	}

	@Override
	protected Authentication getAuthentication(Principal principal,
			Credentials creds) throws RepositoryException {
        if ( creds instanceof TrustedCredentials ) {
            return new Authentication() {

                public boolean canHandle(Credentials credentials) {
                    return (credentials instanceof AdministrativeCredentials)
                            || (credentials instanceof AnonCredentials);
                }

                public boolean authenticate(Credentials credentials)
                        throws RepositoryException {
                    return (credentials instanceof AdministrativeCredentials)
                            || (credentials instanceof AnonCredentials);
                }
            };
        }
		if (user != null) {
			Authentication authentication = new SparseCredentialsAuthentication(
					user, authenticator);
			if (authentication.canHandle(creds)) {
				return authentication;
			}
		} else {
			LOGGER.info("User is null, no login being performed ");
		}
		return null;
	}

	@Override
	protected Principal getPrincipal(Credentials credentials) {
        if ( credentials instanceof TrustedCredentials ) {
            return ((TrustedCredentials) credentials).getPrincipal();
        }
		String userId = getUserID(credentials);
		LOGGER.info("Got User ID as [{}]",userId);
		User auser = authenticator.systemAuthenticate(userId);
		if (auser != null) {
			this.user = auser;
			if ( User.ADMIN_USER.equals(userId)) {
				LOGGER.info("Admin User Principa");
				return new AdminPrincipal(userId);
			} else if ( User.SYSTEM_USER.equals(userId)) {
				LOGGER.info("System User Principa");
				return new SystemPrincipal();
			}
			LOGGER.info("Sparse User Principal {}",userId);
			return new SparsePrincipal(userId, this.getClass().getName()+" credentials were "+((credentials == null)?null:credentials.getClass().getName()), SparseMapUserManager.USERS_PATH);
		} else {
			LOGGER.info("No User Found in credentials  UserID[{}] Credentials[{}]",userId,credentials);
		}
		return null;
	}

	/**
     * Since the AbstractLoginModule getCredentials does not know anything about TrustedCredentials we have to re-try here.
     */
    @Override
    protected Credentials getCredentials() {
        Credentials creds = super.getCredentials();
        if ( creds == null ) {
            CredentialsCallback callback = new CredentialsCallback();
            try {
                callbackHandler.handle(new Callback[]{callback});
                Credentials callbackCreds = callback.getCredentials();
                if ( callbackCreds instanceof TrustedCredentials ) {
                    creds = callbackCreds;
                }
            } catch (UnsupportedCallbackException e) {
                LOGGER.warn("Credentials-Callback not supported try Name-Callback");
            } catch (IOException e) {
                LOGGER.error("Credentials-Callback failed: " + e.getMessage() + ": try Name-Callback");
            }
        }
        return creds;
    }
    
    
	private static final String KEY_LOGIN_NAME = "javax.security.auth.login.name";
    protected String getUserID(Credentials credentials) {
        String userId = null;
        if (credentials != null) {
            if (credentials instanceof GuestCredentials) {
            	LOGGER.info("Getting Anon credentials from GuestCredentials ");
                userId = anonymousId;
            } else if (credentials instanceof SimpleCredentials) {
                userId = ((SimpleCredentials) credentials).getUserID();
            	LOGGER.info("Getting {} credentials from SimpleCredentials ",userId);
            } else {
                try {
                    NameCallback callback = new NameCallback("User-ID: ");
                    callbackHandler.handle(new Callback[]{callback});
                    userId = callback.getName();
                	LOGGER.info("Used Name Callback to get  {} ",userId);
                } catch (UnsupportedCallbackException e) {
                    LOGGER.warn("Credentials- or NameCallback must be supported");
                } catch (IOException e) {
                    LOGGER.error("Name-Callback failed: " + e.getMessage());
                }
            }
        }
        if (userId == null && sharedState.containsKey(KEY_LOGIN_NAME)) {
            userId = (String) sharedState.get(KEY_LOGIN_NAME);
        	LOGGER.info("Used sharedstate to get  {} ",userId);
        }

        // still no userId -> anonymousID if its has been defined.
        // TODO: check again if correct when used with 'extendedAuth'
        if (userId == null) {
            userId = anonymousId;
        	LOGGER.info("Defaulted to Anon");
        }
        return userId;
    }
}
