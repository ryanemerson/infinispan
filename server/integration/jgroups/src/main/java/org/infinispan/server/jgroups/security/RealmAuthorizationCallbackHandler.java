package org.infinispan.server.jgroups.security;

import static org.wildfly.security.sasl.util.SaslMechanismInformation.Names.DIGEST_MD5;
import static org.wildfly.security.sasl.util.SaslMechanismInformation.Names.EXTERNAL;
import static org.wildfly.security.sasl.util.SaslMechanismInformation.Names.GSSAPI;
import static org.wildfly.security.sasl.util.SaslMechanismInformation.Names.PLAIN;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;

import org.infinispan.server.jgroups.logging.JGroupsLogger;
import org.jboss.as.core.security.RealmUser;
import org.jboss.as.core.security.SubjectUserInfo;
import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.AuthorizingCallbackHandler;
import org.jboss.as.domain.management.RealmConfigurationConstants;
import org.jboss.as.domain.management.SecurityRealm;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.callback.AvailableRealmsCallback;
import org.wildfly.security.auth.callback.CredentialCallback;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.impl.PasswordFactorySpiImpl;
import org.wildfly.security.password.interfaces.DigestPassword;
import org.wildfly.security.password.spec.DigestPasswordSpec;
import org.wildfly.security.sasl.WildFlySasl;
import org.wildfly.security.sasl.util.UsernamePasswordHashUtil;

/**
 * RealmAuthorizationCallbackHandler. A {@link CallbackHandler} for JGroups which piggybacks on the
 * realm-provided {@link AuthorizingCallbackHandler}s and provides additional role validation
 *
 * @author Tristan Tarrant
 * @since 7.0
 */
public class RealmAuthorizationCallbackHandler implements CallbackHandler {
    private final String mechanismName;
    private final SecurityRealm realm;
    private final String clusterRole;

    private static final String SASL_OPT_PRE_DIGESTED_PROPERTY = "org.wildfly.security.sasl.digest.pre_digested";

    public RealmAuthorizationCallbackHandler(SecurityRealm realm, String mechanismName, String clusterRole, Map<String, String> mechanismProperties) {
        this.realm = realm;
        this.mechanismName = mechanismName;
        this.clusterRole = clusterRole;
        tunePropsForMech(mechanismProperties);
    }

    private void tunePropsForMech(Map<String, String> mechanismProperties) {
        if (DIGEST_MD5.equals(mechanismName)) {
            if (!mechanismProperties.containsKey(WildFlySasl.REALM_LIST)) {
                mechanismProperties.put(WildFlySasl.REALM_LIST, realm.getName());
            }
            Map<String, String> mechConfig = realm.getMechanismConfig(AuthMechanism.DIGEST);
            boolean plainTextDigest = true;
            if (mechConfig.containsKey(RealmConfigurationConstants.DIGEST_PLAIN_TEXT)) {
                plainTextDigest = Boolean.parseBoolean(mechConfig.get(RealmConfigurationConstants.DIGEST_PLAIN_TEXT));
            }
            if (!plainTextDigest) {
                mechanismProperties.put(SASL_OPT_PRE_DIGESTED_PROPERTY, "true");
            }
        }
//        mechanismProperties.put("com.sun.security.sasl.digest.utf8", "true");
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        AuthenticationConfiguration commonConfig = new AuthenticationConfiguration();

        JGroupsLogger.ROOT_LOGGER.errorf("RACH callbacks %s", Arrays.toString(callbacks));
        String name = null;
        for (Callback callback : callbacks) {
            if (callback instanceof AvailableRealmsCallback) {
                ((AvailableRealmsCallback) callback).setRealmNames(realm.getName());
                return;
            } else if (callback instanceof NameCallback) {
                name = ((NameCallback) callback).getName(   );
            } else if (callback instanceof CredentialCallback) {
                // TODO should we even be doing this? L261 digest_urp is always different to what we set here. Where is this being generated??????
//                JGroupsLogger.ROOT_LOGGER.errorf("Providers %s" + Arrays.toString(Security.getProviders()));
//                JGroupsLogger.ROOT_LOGGER.errorf("RealmAuthorization CredentialCallback: %s" + callback);
                CredentialCallback cb = (CredentialCallback) callback;
                if (name == null) {
                    JGroupsLogger.ROOT_LOGGER.errorf("Name is null, setting manually");
                    name = "node1";
                }
                Password password;
                try {
                    String credential = "192f232a72633ebb5ce2156fa787417d";
                    String realmName = realm.getName();

//                    final PasswordFactory instance = PasswordFactory.getInstance(DigestPassword.ALGORITHM_DIGEST_MD5);
                    Security.insertProviderAt(new WildFlyElytronProvider(), 0);
                    final PasswordFactory instance = new PasswordFactory(new PasswordFactorySpiImpl(), new WildFlyElytronProvider(), DigestPassword.ALGORITHM_DIGEST_MD5);
//                    password = instance.generatePassword(new DigestPasswordSpec(name, realmName, credential.getBytes(StandardCharsets.UTF_8)));

//                    final PasswordFactory instance = PasswordFactory.getInstance(DigestPassword.ALGORITHM_DIGEST_MD5);
//                    password = instance.generatePassword(new DigestPasswordSpec(name, realmName, credential.getBytes(StandardCharsets.UTF_8)));
//                    MessageDigest md = MessageDigest.getInstance("MD5");
//                    JGroupsLogger.ROOT_LOGGER.errorf("Server md | Alg=%s, Provider=%s, Hash=%s", md.getAlgorithm(), md.getProvider(), md.hashCode());
                    byte[] urpHash = new UsernamePasswordHashUtil().generateHashedURP(name, realmName, credential.toCharArray(), false);
                    KeySpec keySpec = new DigestPasswordSpec(name, realmName, urpHash);
                    password = instance.generatePassword(keySpec);
                    cb.setCredential(new PasswordCredential(password));
//
                    JGroupsLogger.ROOT_LOGGER.errorf("urpHash        : %s", Arrays.toString(urpHash));
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
        AuthorizingCallbackHandler cbh = getMechCallbackHandler();
        JGroupsLogger.ROOT_LOGGER.errorf("AuthorizingCallbackHandler=%s", cbh.getClass().getName());
        cbh.handle(callbacks);
    }

    private AuthorizingCallbackHandler getMechCallbackHandler() {
        if (PLAIN.equals(mechanismName)) {
            return new DelegatingRoleAwareAuthorizingCallbackHandler(realm.getAuthorizingCallbackHandler(AuthMechanism.PLAIN));
        } else if (DIGEST_MD5.equals(mechanismName)) {
            return new DelegatingRoleAwareAuthorizingCallbackHandler(realm.getAuthorizingCallbackHandler(AuthMechanism.DIGEST));
        } else if (GSSAPI.equals(mechanismName)) {
            return new DelegatingRoleAwareAuthorizingCallbackHandler(realm.getAuthorizingCallbackHandler(AuthMechanism.PLAIN));
        } else if (EXTERNAL.equals(mechanismName)) {
            return new DelegatingRoleAwareAuthorizingCallbackHandler(realm.getAuthorizingCallbackHandler(AuthMechanism.CLIENT_CERT));
        } else {
            throw new IllegalArgumentException("Unsupported mech " + mechanismName);
        }
    }

    SubjectUserInfo validateSubjectRole(SubjectUserInfo subjectUserInfo) {
        for(Principal principal : subjectUserInfo.getPrincipals()) {
            if (clusterRole.equals(principal.getName())) {
                return subjectUserInfo;
            }
        }
        throw JGroupsLogger.ROOT_LOGGER.unauthorizedNodeJoin(subjectUserInfo.getUserName());
    }

    class DelegatingRoleAwareAuthorizingCallbackHandler implements AuthorizingCallbackHandler {
        private final AuthorizingCallbackHandler delegate;

        DelegatingRoleAwareAuthorizingCallbackHandler(AuthorizingCallbackHandler acbh) {
            this.delegate = acbh;
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            AuthorizeCallback acb = findCallbackHandler(AuthorizeCallback.class, callbacks);
            if (acb != null) {
                String authenticationId = acb.getAuthenticationID();
                String authorizationId = acb.getAuthorizationID();
                acb.setAuthorized(authenticationId.equals(authorizationId));
                int realmSep = authorizationId.indexOf('@');
                RealmUser realmUser = realmSep < 0 ? new RealmUser(authorizationId) : new RealmUser(authorizationId.substring(realmSep+1), authorizationId.substring(0, realmSep));
                List<Principal> principals = new ArrayList<>();
                principals.add(realmUser);
                createSubjectUserInfo(principals);
            } else {
                delegate.handle(callbacks);
            }
        }

        @Override
        public SubjectUserInfo createSubjectUserInfo(Collection<Principal> principals) throws IOException {
            // The call to the delegate will supplement the user with additional role information
            SubjectUserInfo subjectUserInfo = delegate.createSubjectUserInfo(principals);
            return validateSubjectRole(subjectUserInfo);
        }
    }

    private static <T extends Callback> T findCallbackHandler(Class<T> klass, Callback[] callbacks) {
        for (Callback callback : callbacks) {
            if (klass.isInstance(callback))
                return (T) callback;
        }
        return null;
    }
}
